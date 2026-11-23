package io.softwareecg.awb.importer.adapter.prometheus.client.model.query;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class TimeInterval {

    private final int amount;

    private final Unit unit;

    public TimeInterval(int amount, Unit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public int getAmount() {
        return amount;
    }

    public Unit getUnit() {
        return unit;
    }

    public enum Unit {

        MINUTE,

        DAY,

        WEEK,

        YEAR
    }
}
