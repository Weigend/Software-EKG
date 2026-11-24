package io.softwareecg.awb.e2e;

import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every importer of the Workbench from one end to the other: its item in the menu
 * <b>Import</b>, its dialog, the import, and what the repository holds afterwards.
 * <p>
 * The tests start the real Workbench (see {@link Workbench}) and import small files that lie
 * with the tests, and the series of a Prometheus server that is part of the test
 * ({@link PrometheusStub}). They take a picture of every dialog into
 * {@code target/e2e/screenshots}.
 * <p>
 * Not part of {@code mvn test}: run them with {@code run-e2e-tests.sh}, or with
 * {@code mvn -pl workbench -am test -Pe2e}. They need a display, on Linux for example
 * {@code xvfb-run}.
 */
class ImporterE2ETest {

    private static final Duration IMPORT = Duration.ofMinutes(2);
    private static final Duration SEARCHABLE = Duration.ofSeconds(30);

    private static Workbench workbench;

    @BeforeAll
    static void startTheWorkbench() {
        workbench = Workbench.start();
    }

    /**
     * What software systems measure comes first, then the data sets that have nothing to do
     * with software.
     */
    @Test
    void theImportMenuListsTheImportersInTheirOrder() {
        assertThat(workbench.menuItems("Import")).containsExactly(
                "EKG Collector Logs",
                "Prometheus Data",
                "---",
                "OWID COVID-19 Data",
                "Binance Market Data",
                "Yahoo Finance Market Data");
    }

    /**
     * Adding a project of a remote repository imports nothing: it is in the menu of the
     * repositories, below adding a repository.
     */
    @Test
    void theRepositoryMenuAddsRepositoriesAndTheirProjects() {
        assertThat(workbench.menuItems("Repository")).containsExactly("Add Repository...", "Add Project...");
    }

    @Test
    void importsTheCsvFilesOfACollector() {
        ImportDialog dialog = workbench.openImporter("EKG Collector Logs");
        dialog.projectName("Collector logs").files(testData("e2e/collector"));
        dialog.screenshot("importer-collector-csv-dialog");
        dialog.importData(IMPORT);

        ImportedProject project = workbench.project("Collector logs");
        assertThat(project.exists()).isTrue();
        // 30 rows of 11 metrics
        project.awaitPoints(330, SEARCHABLE);
        assertThat(project.hostGroups()).containsExactly("workstations");
        assertThat(project.hosts()).containsExactly("localhost");
        assertThat(project.processes()).containsExactly("global");
        assertThat(project.metricGroups()).containsExactly("os");
        assertThat(project.metricNames()).hasSize(11).contains("\\Cpu\\us", "\\Load\\one");
        // the start of the Collector tells its runs apart
        assertThat(project.measurements()).containsExactly("2026-11-21-16-48-03");
    }

    @Test
    void importsTheCompactFileOfOurWorldInData() {
        ImportDialog dialog = workbench.openImporter("OWID COVID-19 Data");
        dialog.projectName("COVID-19").files(testData("testdata/owid_compact.csv"));
        dialog.screenshot("importer-owid-dialog");
        dialog.importData(IMPORT);

        ImportedProject project = workbench.project("COVID-19");
        assertThat(project.exists()).isTrue();
        // twelve days of one country; a value of 0 and an empty cell are not stored
        project.awaitPoints(298, SEARCHABLE);
        assertThat(project.hostGroups()).containsExactly("Asia");
        assertThat(project.hosts()).containsExactly("Afghanistan");
        assertThat(project.metricGroups()).contains("CASES", "DEATHS", "MISC");
        assertThat(project.metricNames())
                .contains("total_cases", "new_deaths", "stringency_index")
                .doesNotContain("country", "date", "code", "continent");
    }

    @Test
    void importsTheTradesOfBinance() {
        ImportDialog dialog = workbench.openImporter("Binance Market Data");
        dialog.projectName("Crypto").files(testData("e2e/binance"));
        dialog.screenshot("importer-binance-dialog");
        dialog.importData(IMPORT);

        ImportedProject project = workbench.project("Crypto");
        assertThat(project.exists()).isTrue();
        // 500 trades, each with a price, a quantity and a volume
        project.awaitPoints(1500, SEARCHABLE);
        assertThat(project.hostGroups()).containsExactly("Binance");
        assertThat(project.hosts()).containsExactly("Spot");
        assertThat(project.measurements()).containsExactly("2025-08");
        assertThat(project.metricNames()).containsExactly("price", "quantity", "volume");
        assertThat(project.namespaces()).containsExactly("USDT");
        assertThat(project.services()).containsExactly("BICOUSDT");
    }

    @Test
    void importsThePricesOfYahooFinance() {
        ImportDialog dialog = workbench.openImporter("Yahoo Finance Market Data");
        dialog.projectName("Stock markets").files(testData("e2e/yfinance"));
        dialog.screenshot("importer-yfinance-dialog");
        dialog.importData(IMPORT);

        ImportedProject project = workbench.project("Stock markets");
        assertThat(project.exists()).isTrue();
        // two values, 250 days, eight columns of the daily history
        project.awaitPoints(4000, SEARCHABLE);
        assertThat(project.hostGroups()).containsExactly("Equity");
        assertThat(project.hosts()).containsExactly("AAPL", "SAP.DE");
        assertThat(project.processes()).containsExactly("Nasdaq", "XETRA");
        assertThat(project.measurements()).containsExactly("History/1d");
        assertThat(project.metricGroups()).containsExactly("Corporate Actions", "Price", "Trading");
        assertThat(project.metricNames()).contains("Adjusted Close", "Close", "Volume", "Dividends");
    }

    @Test
    void yahooFinanceLeavesOutWhatIsNotChosen() {
        ImportDialog dialog = workbench.openImporter("Yahoo Finance Market Data");
        dialog.projectName("Stock markets, prices only").files(testData("e2e/yfinance"))
                .option("Import corporate actions", false)
                .option("Import adjusted close", false);
        dialog.importData(IMPORT);

        ImportedProject project = workbench.project("Stock markets, prices only");
        // Open, High, Low, Close and Volume are left
        project.awaitPoints(2500, SEARCHABLE);
        assertThat(project.metricNames()).containsExactly("Close", "High", "Low", "Open", "Volume");
    }

    @Test
    void importsTheSeriesOfAPrometheusServer() {
        try (PrometheusStub prometheus = new PrometheusStub()) {
            ImportDialog dialog = workbench.openImporter("Prometheus Data");
            dialog.projectName("Cluster monitoring").serverUrl(prometheus.url()).importRange("1d");
            dialog.screenshot("importer-prometheus-dialog");
            try {
                dialog.importData(IMPORT);
            } catch (AssertionError e) {
                throw new AssertionError(e.getMessage() + "; the server was asked " + prometheus.requests(), e);
            }

            ImportedProject project = workbench.project("Cluster monitoring");
            assertThat(project.exists()).isTrue();
            project.awaitPoints((long) PrometheusStub.METRICS.size() * PrometheusStub.INSTANCES.size() * PrometheusStub.POINTS,
                    SEARCHABLE);
            assertThat(project.hosts()).containsExactlyElementsOf(PrometheusStub.INSTANCES);
            assertThat(prometheus.requests()).noneMatch(request -> request.startsWith("UNEXPECTED"));
        }
    }

    /**
     * Who looks at a project and then decides for a new one does not get the server of the first:
     * its address and password stayed in the fields, and the import ended with an error.
     */
    @Test
    void aNewProjectDoesNotGetTheServerOfTheProjectChosenBefore() {
        long points = (long) PrometheusStub.METRICS.size() * PrometheusStub.INSTANCES.size() * PrometheusStub.POINTS;
        try (PrometheusStub first = new PrometheusStub(); PrometheusStub second = new PrometheusStub()) {
            workbench.openImporter("Prometheus Data").projectName("First cluster").serverUrl(first.url()).importRange("1d")
                    .importData(IMPORT);
            workbench.project("First cluster").awaitPoints(points, SEARCHABLE);

            ImportDialog dialog = workbench.openImporter("Prometheus Data").existingProject("First cluster");
            assertThat(dialog.serverUrl()).isEqualTo(first.url());
            dialog.newProject("Second cluster");
            assertThat(dialog.serverUrl()).as("the server of the new project").isNullOrEmpty();
            dialog.serverUrl(second.url()).newProject("Second cluster, renamed");
            assertThat(dialog.serverUrl()).as("the server after the new project got another name").isEqualTo(second.url());
            dialog.importRange("1d").importData(IMPORT);

            workbench.project("Second cluster, renamed").awaitPoints(points, SEARCHABLE);
            assertThat(workbench.project("First cluster").sourceUrl()).isEqualTo(first.url());
            assertThat(workbench.project("Second cluster, renamed").sourceUrl()).isEqualTo(second.url());
        }
    }

    /**
     * An import that runs while another one is started goes on reading from its own server. Both
     * shared one connection: the first went on with the server of the second.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void twoImportsAtATimeReadEachFromItsOwnServer(boolean remote) {
        String slow = remote ? "Slow server, remote" : "Slow server";
        String quick = remote ? "Quick server, remote" : "Quick server";
        try (PrometheusStub slowServer = new PrometheusStub(List.of("slow-01", "slow-02"));
             PrometheusStub quickServer = new PrometheusStub(List.of("quick-01", "quick-02"))) {
            slowServer.hold();
            prometheusImport(slow, slowServer, remote).startImport();
            workbench.awaitTrue("the first import to ask for its series", IMPORT,
                    () -> slowServer.requests().stream().anyMatch(request -> request.contains("__name__")));
            prometheusImport(quick, quickServer, remote).importData(IMPORT);
            slowServer.release();

            assertThat(hostsOf(quick)).containsExactly("quick-01", "quick-02");
            assertThat(hostsOf(slow)).containsExactly("slow-01", "slow-02");
            workbench.awaitTrue("the first import to read all its metrics", SEARCHABLE, () -> PrometheusStub.METRICS.stream()
                    .allMatch(metric -> workbench.project(slow).metricNames().stream().anyMatch(metric::contains)));
            assertThat(workbench.project(slow).hosts()).containsExactly("slow-01", "slow-02");
        }
    }

    private static ImportDialog prometheusImport(String project, PrometheusStub server, boolean remote) {
        ImportDialog dialog = workbench.openImporter("Prometheus Data").projectName(project).serverUrl(server.url()).importRange("1d");
        return remote ? dialog.remoteProject() : dialog;
    }

    /** The hosts of a project as soon as it has some. */
    private static Set<String> hostsOf(String project) {
        workbench.awaitTrue("the hosts of '" + project + "'", SEARCHABLE, () -> !workbench.project(project).hosts().isEmpty());
        return workbench.project(project).hosts();
    }

    @Test
    void prometheusNeedsAServerBeforeItImports() {
        ImportDialog dialog = workbench.openImporter("Prometheus Data");
        try {
            assertThat(dialog.canImport()).as("without a project").isFalse();
            dialog.projectName("No server yet");
            assertThat(dialog.canImport()).as("with a project, without a server").isFalse();
            dialog.serverUrl("http://127.0.0.1:9090");
            assertThat(dialog.canImport()).as("with a project and a server").isTrue();
        } finally {
            dialog.cancel();
        }
    }

    /** The project bar finds the importers and opens the one the data of the project came from. */
    @Test
    void theProjectBarOpensTheImporterOfTheProject() {
        workbench.openImporter("EKG Collector Logs").projectName("From the bar").files(testData("e2e/collector")).importData(IMPORT);
        workbench.project("From the bar").select();
        Button importData = (Button) workbench.find("btnImportDataToProject").orElseThrow();
        Stage dialog = workbench.openDialog("Import Data", importData::fire);
        assertThat(workbench.onFx(dialog::getTitle)).containsIgnoringCase("Collector");
        workbench.close(dialog);
    }

    private static Path testData(String resource) {
        try {
            return Path.of(ImporterE2ETest.class.getClassLoader().getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
