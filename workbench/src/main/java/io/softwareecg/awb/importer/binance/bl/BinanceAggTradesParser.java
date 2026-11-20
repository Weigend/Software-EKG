package io.softwareecg.awb.importer.binance.bl;

import io.softwareecg.awb.sdk.awbapi.repository.FilterDimension;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.Value;
import org.slf4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Parser for Binance aggTrades CSV files (from data.binance.vision).
 *
 * CSV format (no header):
 * agg_trade_id, price, quantity, first_trade_id, last_trade_id, timestamp, is_buyer_maker, is_best_match
 *
 * Note: Binance changed the timestamp format — pre-2025 files use milliseconds (13 digits),
 * while 2025+ files use microseconds (16 digits). The parser auto-detects the format.
 *
 * Produces 3 RawSeriesData per file: price, quantity, volume (price * quantity).
 *
 */
public class BinanceAggTradesParser {

    private static final Logger LOGGER = EkgLogger.get();

    private static final Set<String> KNOWN_QUOTE_ASSETS = Set.of(
            "USDT", "BUSD", "USDC", "TUSD", "FDUSD", "DAI",
            "BTC", "ETH", "BNB", "XRP", "DOGE",
            "EUR", "GBP", "TRY", "BRL", "ARS", "PLN", "RON", "UAH",
            "AUD", "RUB", "NGN", "ZAR", "BIDR", "IDRT", "VAI"
    );

    /**
     * Parses a Binance aggTrades ZIP file and returns a stream of RawSeriesData.
     *
     * @param zipFile the ZIP file to parse
     * @return stream of 3 RawSeriesData (price, quantity, volume)
     */
    public Stream<RawSeriesData> parseZipFile(File zipFile) {
        try {
            String fileName = zipFile.getName();
            String baseName = fileName.replace(".zip", "");

            // Extract symbol and period from filename: BICOUSDT-aggTrades-2025-08
            String[] parts = baseName.split("-");
            if (parts.length < 4) {
                LOGGER.warn("Unexpected filename format: {}", fileName);
                return Stream.empty();
            }

            String symbol = parts[0];
            String period = parts[2] + "-" + parts[3];
            String quoteAsset = extractQuoteAsset(symbol);

            // Create 3 series: price, quantity, volume
            RawSeriesData priceSeries = new RawSeriesData("price");
            RawSeriesData quantitySeries = new RawSeriesData("quantity");
            RawSeriesData volumeSeries = new RawSeriesData("volume");

            // Parse CSV from ZIP
            try (ZipFile zip = new ZipFile(zipFile)) {
                ZipEntry csvEntry = zip.entries().nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(zip.getInputStream(csvEntry)))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseLine(line, priceSeries, quantitySeries, volumeSeries);
                    }
                }
            }

            // Set filter dimensions on all series
            List<RawSeriesData> allSeries = Arrays.asList(priceSeries, quantitySeries, volumeSeries);
            for (RawSeriesData series : allSeries) {
                series.addLabel(FilterDimension.HOST_GROUP, "Binance");
                series.addLabel(FilterDimension.HOST, "Spot");
                series.addLabel(FilterDimension.NAMESPACE, quoteAsset);
                series.addLabel(FilterDimension.SERVICE, symbol);
                series.addLabel(FilterDimension.MEASUREMENT, period);
                series.addLabel(FilterDimension.METRIC_GROUP, "Market Data");
            }

            LOGGER.info("Parsed {} with {} price values, {} quantity values, {} volume values",
                    fileName, priceSeries.getValues().size(),
                    quantitySeries.getValues().size(), volumeSeries.getValues().size());

            return allSeries.stream();

        } catch (Exception e) {
            LOGGER.error("Failed to parse Binance aggTrades file: {}", zipFile.getName(), e);
            return Stream.empty();
        }
    }

    private void parseLine(String line, RawSeriesData price, RawSeriesData quantity, RawSeriesData volume) {
        // Format: agg_trade_id,price,quantity,first_trade_id,last_trade_id,timestamp,is_buyer_maker,is_best_match
        String[] fields = line.split(",");
        if (fields.length < 7) return;

        try {
            double priceVal = Double.parseDouble(fields[1]);
            double quantityVal = Double.parseDouble(fields[2]);
            long rawTimestamp = Long.parseLong(fields[5]);

            // Binance changed timestamp format: pre-2025 files use milliseconds (13 digits),
            // 2025+ files use microseconds (16 digits). Normalize to milliseconds.
            long timestampMillis = (rawTimestamp > 9_999_999_999_999L)
                    ? rawTimestamp / 1000   // microseconds → milliseconds
                    : rawTimestamp;          // already milliseconds

            price.addValue(new Value(timestampMillis, priceVal));
            quantity.addValue(new Value(timestampMillis, quantityVal));
            volume.addValue(new Value(timestampMillis, priceVal * quantityVal));

        } catch (NumberFormatException e) {
            // skip malformed lines
        }
    }

    /**
     * Extracts the quote asset from a trading pair symbol.
     * Tries known quote assets from longest to shortest match.
     */
    static String extractQuoteAsset(String symbol) {
        // Try longer suffixes first (FDUSD before USD)
        return KNOWN_QUOTE_ASSETS.stream()
                .filter(symbol::endsWith)
                .max((a, b) -> Integer.compare(a.length(), b.length()))
                .orElse("UNKNOWN");
    }
}
