package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusPlan;
import io.softwareecg.awb.datasource.spi.AnalysisSourceDefinition;
import io.softwareecg.awb.datasource.spi.AnalysisSourceOption;
import io.softwareecg.awb.datasource.spi.AnalysisSourceProvider;
import io.softwareecg.awb.datasource.AnalysisSourceRegistry;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.NewAnalysisService;
import io.softwareecg.awb.datasource.adapter.prometheus.MetricProfile;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment.Group;
import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery;
import io.softwareecg.awb.datasource.adapter.ssh.SshPlan;
import io.softwareecg.awb.datasource.adapter.ssh.TopMetric;
import io.softwareecg.awb.datasource.adapter.windows.WindowsDiscovery;
import io.softwareecg.awb.datasource.adapter.windows.WindowsMetric;
import io.softwareecg.awb.datasource.adapter.windows.WindowsPlan;
import io.softwareecg.awb.query.api.OpenInvestigationCommand;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.SeriesDataWrittenEvent;
import io.softwareecg.collector.source.prometheus.PrometheusException;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The "New Analysis" dialog against a stand-in for the service: the choice of topics it starts
 * with, what it shows after connecting, what it hands over on start and that the first chart
 * opens once data arrives.
 */
public class NewAnalysisDialogTest {

    private static final Group NODE = new Group("node", List.of("server-01", "server-02", "server-03"), 6000, Map.of(
            "up", 3, "node_cpu_seconds_total", 2400, "node_load1", 3,
            "node_memory_MemAvailable_bytes", 3, "node_memory_MemTotal_bytes", 3));

    private static final Group API = new Group("api", List.of("api-1"), 300, Map.of(
            "up", 1, "process_cpu_seconds_total", 1, "http_requests_total", 200));

    private static final Group SHOP = new Group("shop", List.of("shop-1", "shop-2"), 800, Map.of(
            "up", 2, "jvm_memory_used_bytes", 16, "jvm_threads_live_threads", 2, "process_cpu_usage", 2));

    private static EventBus<EventObject> eventBus;

    private final FakeService service = new FakeService();

    private NewAnalysisDialog dialog;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void startToolkitAndLookup() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);

        eventBus = new SimpleEventBus();
        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return clazz == EventBus.class ? clazz.cast(eventBus) : null;
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                return List.of();
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });
    }

    @AfterClass
    public static void shutdownLookup() {
        Lookup.shutdown();
    }

    @After
    public void closeDialog() throws Exception {
        onFx(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.setResult(null);
                dialog.close();
            }
            return null;
        });
    }

    @Test
    public void theDialogStartsWithTheTopicsAndGreysOutWhatIsNotBuiltYet() throws Exception {
        show(null);

        for (AnalysisTopic topic : AnalysisTopic.values()) {
            // Windows is built, but its counters cannot be read by the machine of this test
            assertEquals(topic + " can be chosen exactly if it is built", !topic.isAvailable() || topic == AnalysisTopic.WINDOWS,
                    (boolean) onFx(() -> tile(topic).isDisabled()));
        }
        assertTrue("the planned topics are there to be seen", onFx(() -> tile(AnalysisTopic.KUBERNETES).isVisible()));
        assertTrue("nothing chosen, nothing to continue with", onFx(() -> button("#continue").isDisabled()));
        assertFalse("the server is not asked for before the topic", onFx(() -> visible("#prometheusUrl")));

        onFx(() -> {
            tile(AnalysisTopic.LINUX).setSelected(true);
            return null;
        });
        assertFalse(onFx(() -> button("#continue").isDisabled()));
    }

    @Test
    public void chosenTopicsSelectTheirSystemsAndListTheRestBelow() throws Exception {
        service.groups = List.of(NODE, SHOP, API);
        show(null);
        choose(AnalysisTopic.LINUX, AnalysisTopic.JVM);
        connect();

        assertEquals("Where is the Prometheus server for Linux / Unix and Java / JVM?", text("#stepTitle"));
        assertEquals("Linux / Unix 3 systems found", textOf("#found-LINUX"));
        assertEquals("Java / JVM 2 systems found", textOf("#found-JVM"));
        assertTrue(textOf("#found-other").startsWith("Also found"));
        assertEquals("what belongs to the chosen topics is selected, the rest is offered",
                List.of("node [x]", "shop [x]", "api [ ]"), groups());
        assertTrue(text("#status"), text("#status").contains("Linux / Unix: 3 systems. Java / JVM: 2 systems."));
        assertFalse(onFx(() -> button("#startAnalysis").isDisabled()));

        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.started.isEmpty(), "the analysis to be started");
        assertEquals(List.of("node", "shop"), service.started.get(0).groups().stream()
                .map(PrometheusPlan.GroupSelection::group).collect(Collectors.toList()));
    }

    @Test
    public void aTopicTheServerHasNothingForIsSaidAndNothingIsSelected() throws Exception {
        show(null);
        choose(AnalysisTopic.JVM);
        connect();

        assertEquals("Java / JVM nothing found on this server", textOf("#found-JVM"));
        assertTrue(text("#status"), text("#status").contains("monitors nothing for Java / JVM"));
        assertEquals(List.of("node [ ]", "api [ ]"), groups());
        assertTrue("nothing selected, nothing to start", onFx(() -> button("#startAnalysis").isDisabled()));

        onFx(() -> {
            ((CheckBox) dialog.getDialogPane().lookup("#group-api")).setSelected(true);
            return null;
        });
        assertFalse("what the server has can still be taken", onFx(() -> button("#startAnalysis").isDisabled()));
    }

    @Test
    public void theSecondStepOffersTheWaysTheDataCanCome() throws Exception {
        service.groups = List.of(NODE, SHOP, API);
        show(null);
        onFx(() -> {
            tile(AnalysisTopic.LINUX).setSelected(true);
            tile(AnalysisTopic.JVM).setSelected(true);
            button("#continue").fire();
            return null;
        });

        assertTrue("the usual way is chosen", onFx(() -> source("LINUX-PROMETHEUS").isSelected()));
        assertFalse("hosts can also be read directly", onFx(() -> source("LINUX-SSH_TOP").isDisabled()));
        assertTrue(onFx(() -> source("JVM-PROMETHEUS").isSelected()));
        assertTrue("what is planned is shown, but cannot be taken", onFx(() -> source("JVM-JOLOKIA").isDisabled()));
        assertFalse("no address is asked for before the way is chosen", onFx(() -> visible("#prometheusUrl")));

        // a click on the chosen way does not leave the topic without one
        onFx(() -> {
            source("LINUX-PROMETHEUS").fire();
            return null;
        });
        assertTrue(onFx(() -> source("LINUX-PROMETHEUS").isSelected()));

        // Linux directly, Java through the server: first the server, then the hosts
        onFx(() -> {
            source("LINUX-SSH_TOP").fire();
            button("#continue").fire();
            return null;
        });
        assertEquals("Where is the Prometheus server for Java / JVM?", text("#stepTitle"));
        assertTrue("the server is not the last step", onFx(() -> dialog.getDialogPane().lookup("#startAnalysis") == null));
        connect();
        onFx(() -> {
            button("#continue").fire();
            return null;
        });
        assertTrue(onFx(() -> visible("#sshHosts")));
        assertFalse(onFx(() -> visible("#prometheusUrl")));
        assertTrue("the hosts are the last step", onFx(() -> dialog.getDialogPane().lookup("#startAnalysis") != null));
    }

    @Test
    public void linuxHostsReadDirectlyAreNamedCheckedAndMultipliedOut() throws Exception {
        List<OpenInvestigationCommand> charts = new CopyOnWriteArrayList<>();
        eventBus.subscribe(OpenInvestigationCommand.class, charts::add);
        show(null);
        chooseLinuxBySsh();

        assertTrue(onFx(() -> visible("#sshHosts")));
        assertEquals("Name the hosts to read.", text("#sshSummary"));
        assertTrue(onFx(() -> button("#startAnalysis").isDisabled()));

        typeArea("#sshHosts", "db-[01-03]");
        assertEquals("3 hosts:  db-01, db-02, db-03", text("#sshHostsFound"));
        assertEquals("3 hosts \u00d7 (the host itself)  =  3 collector jobs, 24 series", text("#sshSummary"));
        assertEquals("the name is proposed from the first host", "db-01", text("#sshAnalysisName"));
        assertFalse("with the defaults the analysis can be started", onFx(() -> button("#startAnalysis").isDisabled()));

        // the check logs in the way the collector will and offers what runs on the hosts
        type("#sshUser", "monitor");
        onFx(() -> {
            button("#checkHosts").fire();
            return null;
        });
        await(() -> onFx(() -> dialog.getDialogPane().lookup("#process-java") != null), "the hosts to be checked");
        assertEquals("monitor/null", service.inspectedAs);
        assertEquals("2 of 3 hosts can be read:", text("#hostCheckSummary"));
        assertTrue(onFx(() -> ((Pane) dialog.getDialogPane().lookup("#hostResults")).getChildren().stream()
                .anyMatch(node -> ((Label) node).getText().contains("db-02") && ((Label) node).getText().contains("Connection refused"))));

        onFx(() -> {
            ((CheckBox) dialog.getDialogPane().lookup("#process-java")).setSelected(true);
            ((CheckBox) dialog.getDialogPane().lookup("#topMetric-TASKS_RUNNING")).setSelected(false);
            return null;
        });
        type("#moreProcesses", "nginx");
        type("#sshAnalysisName", "Databases");
        assertEquals("3 hosts \u00d7 (the host itself + 2 processes)  =  9 collector jobs, 33 series", text("#sshSummary"));

        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.startedAny.isEmpty(), "the analysis to be started");

        SshPlan plan = (SshPlan) service.startedAny.get(0);
        assertEquals("Databases", plan.projectName());
        assertEquals("linux", plan.hostGroup());
        assertEquals(List.of("db-01", "db-02", "db-03"), plan.hosts().stream().map(SshPlan.Host::name).collect(Collectors.toList()));
        assertEquals("monitor", plan.username());
        assertEquals("no password: the SSH key is used", null, plan.password());
        assertEquals(List.of("java", "nginx"), plan.processes());
        assertEquals(7, plan.systemMetrics().size());
        assertFalse(plan.systemMetrics().contains(TopMetric.TASKS_RUNNING));
        assertEquals(List.of(TopMetric.PROCESS_CPU, TopMetric.PROCESS_MEMORY), plan.processMetrics());
        assertEquals(Duration.ofSeconds(15), plan.interval());
        await(() -> !onFx(dialog::isShowing), "the dialog to close");

        // the first chart shows the same metric as for hosts that come through a Prometheus server
        eventBus.publish(new SeriesDataWrittenEvent(this, "local", "Databases", 2L));
        await(() -> !charts.isEmpty(), "the first chart to open");
        assertEquals("linux", charts.get(0).getFilterParams().getHostGroupName());
        assertEquals("cpu_usage_percent", charts.get(0).getFilterParams().getMetricName());
    }

    @Test
    public void windowsCountersAreOfferedWhereTheyCanBeRead() throws Exception {
        show(null);
        assertTrue("not on a machine that cannot read them", onFx(() -> tile(AnalysisTopic.WINDOWS).isDisabled()));
        assertTrue("the tile says why", onFx(() -> tile(AnalysisTopic.WINDOWS).lookupAll(".tile-badge").stream()
                .anyMatch(node -> !((Label) node).getText().equals("coming soon"))));
        closeDialog();

        service.windowsProblem = null;
        show(null);
        assertFalse(onFx(() -> tile(AnalysisTopic.WINDOWS).isDisabled()));
        onFx(() -> {
            tile(AnalysisTopic.WINDOWS).setSelected(true);
            button("#continue").fire();
            return null;
        });
        assertTrue("the Prometheus server knows no Windows hosts yet", onFx(() -> source("WINDOWS-PROMETHEUS").isDisabled()));
        assertFalse(onFx(() -> source("WINDOWS-WINDOWS_COUNTERS").isDisabled()));
        assertTrue("the way that can be gone is chosen", onFx(() -> source("WINDOWS-WINDOWS_COUNTERS").isSelected()));
    }

    @Test
    public void windowsHostsAreNamedCheckedAndMultipliedOut() throws Exception {
        List<OpenInvestigationCommand> charts = new CopyOnWriteArrayList<>();
        eventBus.subscribe(OpenInvestigationCommand.class, charts::add);
        service.windowsProblem = null;
        show(null);
        onFx(() -> {
            tile(AnalysisTopic.WINDOWS).setSelected(true);
            button("#continue").fire();     // to the sources
            button("#continue").fire();     // to the hosts
            return null;
        });

        assertEquals("Which Windows hosts do you want to read?", text("#stepTitle"));
        assertTrue(onFx(() -> visible("#windowsHosts")));
        assertFalse(onFx(() -> visible("#sshHosts")));
        String thisMachine = WindowsPage.thisMachine();
        assertEquals("the machine of the AWB is proposed", thisMachine, areaText("#windowsHosts"));
        assertEquals("1 host:  " + thisMachine, text("#windowsHostsFound"));
        assertEquals("1 host \u00d7 (the host itself)  =  1 collector job, at least 11 series", text("#windowsSummary"));
        assertEquals("the name is proposed from the first host", thisMachine, text("#windowsAnalysisName"));
        assertFalse("with the defaults the analysis can be started", onFx(() -> button("#startAnalysis").isDisabled()));

        // other hosts, read from this one
        typeArea("#windowsHosts", "srv-[01-03]");
        assertEquals("3 hosts:  srv-01, srv-02, srv-03", text("#windowsHostsFound"));
        assertEquals("srv-01", text("#windowsAnalysisName"));

        // the check reads the hosts the way the collector will and offers what runs on them
        onFx(() -> {
            button("#checkWindowsHosts").fire();
            return null;
        });
        await(() -> onFx(() -> dialog.getDialogPane().lookup("#windowsProcess-java") != null), "the hosts to be checked");
        assertEquals("2 of 3 hosts can be read:", text("#windowsHostCheckSummary"));
        assertTrue(onFx(() -> ((Pane) dialog.getDialogPane().lookup("#windowsHostResults")).getChildren().stream()
                .anyMatch(node -> ((Label) node).getText().contains("srv-02") && ((Label) node).getText().contains("Remote Registry"))));
        assertEquals("several processes of a program are counted", "java  \u00d72",
                onFx(() -> ((CheckBox) dialog.getDialogPane().lookup("#windowsProcess-java")).getText()));

        onFx(() -> {
            ((CheckBox) dialog.getDialogPane().lookup("#windowsProcess-java")).setSelected(true);
            ((CheckBox) dialog.getDialogPane().lookup("#windowsMetric-CPU_QUEUE")).setSelected(false);
            ((CheckBox) dialog.getDialogPane().lookup("#windowsMetric-NETWORK_RECEIVE")).setSelected(false);
            ((CheckBox) dialog.getDialogPane().lookup("#windowsMetric-NETWORK_TRANSMIT")).setSelected(false);
            return null;
        });
        type("#windowsMoreProcesses", "sqlservr.exe");
        type("#windowsAnalysisName", "Windows Servers");
        assertEquals("3 hosts \u00d7 (the host itself + 2 processes)  =  9 collector jobs, 36 series", text("#windowsSummary"));

        // what the metrics do not cover is named as a counter; what is no counter is said so
        typeArea("#windowsCounters", "\\Memory\\Cache Bytes, cpu");
        assertTrue(text("#windowsCountersFound"), text("#windowsCountersFound").startsWith("Not a counter: cpu"));
        assertTrue(onFx(() -> button("#startAnalysis").isDisabled()));
        typeArea("#windowsCounters", "\\Memory\\Cache Bytes\n\\LogicalDisk(*)\\% Free Space");
        assertEquals("2 counters", text("#windowsCountersFound"));
        assertEquals("3 hosts \u00d7 (the host itself + 2 processes)  =  9 collector jobs, at least 42 series", text("#windowsSummary"));

        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.startedAny.isEmpty(), "the analysis to be started");

        WindowsPlan plan = (WindowsPlan) service.startedAny.get(0);
        assertEquals("Windows Servers", plan.projectName());
        assertEquals("windows", plan.hostGroup());
        assertEquals(List.of("srv-01", "srv-02", "srv-03"), plan.hosts().stream().map(SshPlan.Host::name).collect(Collectors.toList()));
        assertEquals("the program is named as the counters name it", List.of("java", "sqlservr"), plan.processes());
        assertEquals(8, plan.systemMetrics().size());
        assertFalse(plan.systemMetrics().contains(WindowsMetric.CPU_QUEUE));
        assertEquals(List.of(WindowsMetric.PROCESS_CPU, WindowsMetric.PROCESS_MEMORY), plan.processMetrics());
        assertEquals(List.of("\\Memory\\Cache Bytes", "\\LogicalDisk(*)\\% Free Space"), plan.counters());
        assertEquals(Duration.ofSeconds(15), plan.interval());
        await(() -> !onFx(dialog::isShowing), "the dialog to close");

        // the first chart shows the same metric as for hosts of every other source
        eventBus.publish(new SeriesDataWrittenEvent(this, "local", "Windows Servers", 2L));
        await(() -> !charts.isEmpty(), "the first chart to open");
        assertEquals("windows", charts.get(0).getFilterParams().getHostGroupName());
        assertEquals("cpu_usage_percent", charts.get(0).getFilterParams().getMetricName());
    }

    @Test
    public void twoWaysBecomeTwoDataSourcesOfOneAnalysis() throws Exception {
        service.groups = List.of(NODE, SHOP, API);
        show(null);
        onFx(() -> {
            tile(AnalysisTopic.LINUX).setSelected(true);
            tile(AnalysisTopic.JVM).setSelected(true);
            button("#continue").fire();
            source("LINUX-SSH_TOP").fire();
            button("#continue").fire();
            return null;
        });
        connect();
        assertEquals("only what comes through the server is selected there", List.of("shop [x]", "node [ ]", "api [ ]"), groups());
        type("#analysisName", "Zoo");
        onFx(() -> {
            button("#continue").fire();
            return null;
        });
        assertEquals("it is one analysis with one name", "Zoo", text("#sshAnalysisName"));
        typeArea("#sshHosts", "web-01, web-02");

        // the second source fails at first: the analysis is there, the dialog says what is missing
        service.addFailure = new IllegalStateException("no route to host");
        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> text("#sshSummary").contains("could not be added"), "the problem to be shown");
        assertTrue(text("#sshSummary"), text("#sshSummary").contains("The analysis is running, but web-01, web-02"));
        assertEquals(1, service.startedAny.size());
        assertTrue(onFx(dialog::isShowing));

        // another try adds what is missing and does not start the analysis a second time
        service.addFailure = null;
        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.addedAny.isEmpty(), "the second source to be added");

        assertEquals(1, service.startedAny.size());
        assertEquals("shop", ((PrometheusPlan) service.startedAny.get(0)).groups().get(0).group());
        SshPlan hosts = (SshPlan) service.addedAny.get(0);
        assertEquals("Zoo", hosts.projectName());
        assertEquals(2, hosts.hosts().size());
    }

    @Test
    public void aHostPatternIsResolvedAsTypedAndGoesIntoWhatIsCollected() throws Exception {
        show(null);
        choose(AnalysisTopic.LINUX);
        connect();
        assertEquals("all 3 hosts", text("#hostsFound-node"));

        type("#hosts-node", "SERVER-0?, nothing*");
        assertEquals("all 3 hosts", text("#hostsFound-node"));
        type("#hosts-node", "*-01, *-03");
        assertEquals("2 of 3:  server-01, server-03", text("#hostsFound-node"));
        assertTrue("the estimate follows the hosts: " + radios(), radios().get(0).contains("~8 series"));

        type("#hosts-node", "db*");
        assertEquals("no host matches", text("#hostsFound-node"));
        assertTrue("no host, nothing to start", onFx(() -> button("#startAnalysis").isDisabled()));

        type("#hosts-node", "*-01, *-03");
        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.started.isEmpty(), "the analysis to be started");

        List<String> expressions = service.started.get(0).groups().get(0).expressions().stream()
                .map(expression -> expression.expression()).collect(Collectors.toList());
        assertEquals(4, expressions.size());
        assertTrue("every expression is restricted to the pattern, not to the hosts found now: " + expressions,
                expressions.stream().allMatch(e -> e.contains("instance=~\"(?i:(?:.*-01|.*-03)(?::[0-9]+)?)\"")));
    }

    @Test
    public void theEssentialsCanBeTakenOneByOneAndFurtherMetricsAddedByPattern() throws Exception {
        show(null);
        choose(AnalysisTopic.LINUX);
        connect();

        List<CheckBox> metrics = onFx(() -> dialog.getDialogPane().lookup("#group-node").getParent()
                .lookupAll(".essential-metric").stream()
                .map(node -> (CheckBox) node).collect(Collectors.toList()));
        assertEquals(List.of("up", "cpu usage %", "load 1m", "memory used %"),
                metrics.stream().map(CheckBox::getText).collect(Collectors.toList()));

        onFx(() -> {
            metrics.get(3).setSelected(false);      // no memory
            return null;
        });
        // "up" is an essential already and must not be collected twice
        type("#more-node", "up, node_memory_Mem*");
        assertEquals("2 metrics:  node_memory_MemAvailable_bytes, node_memory_MemTotal_bytes", text("#moreFound-node"));
        assertTrue(radios().get(0), radios().get(0).contains("~15 series"));

        type("#more-node", "jvm_*");
        assertEquals("no further metric matches", text("#moreFound-node"));
        type("#more-node", "up, node_memory_Mem*");

        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.started.isEmpty(), "the analysis to be started");

        List<String> expressions = service.started.get(0).groups().get(0).expressions().stream()
                .map(expression -> expression.expression()).collect(Collectors.toList());
        assertEquals(4, expressions.size());
        assertEquals("up{job=\"node\"}", expressions.get(0));
        assertEquals("node_load1{job=\"node\"}", expressions.get(2));
        assertEquals("{job=\"node\",__name__=~\"node_memory_MemAvailable_bytes|node_memory_MemTotal_bytes\"}",
                expressions.get(3));

        // with nothing ticked and nothing added there is nothing to collect
        onFx(() -> {
            metrics.forEach(metric -> metric.setSelected(false));
            return null;
        });
        type("#more-node", "");
        assertTrue(onFx(() -> button("#startAnalysis").isDisabled()));
    }

    @Test
    public void goingBackKeepsTheChoiceAndTheServer() throws Exception {
        show(null);
        choose(AnalysisTopic.LINUX);
        connect();
        onFx(() -> {
            button("#back").fire();     // to the sources
            button("#back").fire();     // to the topics
            return null;
        });

        assertTrue(onFx(() -> tile(AnalysisTopic.LINUX).isSelected()));
        assertFalse(onFx(() -> visible("#prometheusUrl")));

        // another choice shows the same server under the new topics, without connecting again
        onFx(() -> {
            ((ButtonBase) dialog.getDialogPane().lookup("#tile-explore")).fire();
            return null;
        });
        assertEquals(1, service.discoveries);
        assertNull("without topics there is one plain list", onFx(() -> dialog.getDialogPane().lookup("#found-LINUX")));
        assertEquals(List.of("node [x]", "api [ ]"), groups());
    }

    @Test
    public void connectingShowsTheEnvironmentWithTheLargestGroupPreselected() throws Exception {
        open(null);
        connect();

        assertTrue(text("#status"), text("#status").contains("2 groups with 4 systems"));
        assertEquals(List.of("node [x]", "api [ ]"), groups());

        List<String> choices = onFx(() -> dialog.getDialogPane().lookupAll(".radio-button").stream()
                .map(node -> (RadioButton) node).map(radio -> radio.getText() + (radio.isSelected() ? " [x]" : ""))
                .collect(Collectors.toList()));
        assertTrue("the essentials are the default and say how little they are: " + choices.get(0),
                choices.get(0).startsWith("Essentials") && choices.get(0).contains("~12 series") && choices.get(0).endsWith("[x]"));
        assertTrue("everything says how much it is: " + choices.get(1), choices.get(1).contains("6,000 series")
                || choices.get(1).contains("6.000 series"));

        assertEquals("the name is proposed from the address", "prometheus.example", text("#analysisName"));
        assertFalse("with the defaults the analysis can be started right away", onFx(() -> button("#startAnalysis").isDisabled()));
    }

    @Test
    public void startHandsOverTheSelectionAndTheFirstChartOpensWhenDataArrives() throws Exception {
        List<OpenInvestigationCommand> charts = new CopyOnWriteArrayList<>();
        eventBus.subscribe(OpenInvestigationCommand.class, charts::add);

        open(null);
        connect();
        onFx(() -> {
            ((TextField) dialog.getDialogPane().lookup("#analysisName")).setText("Shop");
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.started.isEmpty(), "the analysis to be started");

        PrometheusPlan plan = service.started.get(0);
        assertEquals("Shop", plan.projectName());
        assertEquals("http://prometheus.example:9090", plan.url());
        assertEquals(Duration.ofHours(24), plan.lookback());
        assertEquals(Duration.ofSeconds(60), plan.interval());
        assertEquals(1, plan.groups().size());
        assertEquals("node", plan.groups().get(0).group());
        assertEquals(MetricProfile.essentials(NODE, Duration.ofSeconds(60)), plan.groups().get(0).expressions());
        await(() -> !onFx(dialog::isShowing), "the dialog to close");

        assertTrue("no chart before there is data", charts.isEmpty());
        eventBus.publish(new SeriesDataWrittenEvent(this, "local", "Other", 1L));
        eventBus.publish(new SeriesDataWrittenEvent(this, "local", "Shop", 2L));
        await(() -> !charts.isEmpty(), "the first chart to open");

        OpenInvestigationCommand chart = charts.get(0);
        assertEquals("Shop", chart.getFilterParams().getProjectName());
        assertEquals("node", chart.getFilterParams().getHostGroupName());
        assertEquals("cpu_usage_percent", chart.getFilterParams().getMetricName());

        eventBus.publish(new SeriesDataWrittenEvent(this, "local", "Shop", 3L));
        Thread.sleep(300);
        assertEquals("the chart is opened once, later data only updates it", 1, charts.size());
    }

    @Test
    public void rejectedCredentialsAreExplainedAndTheAuthenticationOpens() throws Exception {
        service.discoverFailure = new PrometheusException("Prometheus at http://prometheus.example:9090 rejected the credentials (HTTP 401).");
        open(null);
        onFx(() -> {
            button("#connect").fire();
            return null;
        });
        await(() -> text("#status").contains("rejected the credentials"), "the problem to be shown");

        assertTrue(onFx(() -> dialog.getDialogPane().lookupAll(".titled-pane").stream()
                .map(node -> (TitledPane) node).anyMatch(TitledPane::isExpanded)));
        assertTrue(onFx(() -> button("#startAnalysis").isDisabled()));
    }

    @Test
    public void addingASourceGoesToTheExistingAnalysis() throws Exception {
        open("Shop");
        connect();

        assertTrue("the analysis is fixed", onFx(() -> dialog.getDialogPane().lookup("#analysisName").isDisabled()));
        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.added.isEmpty(), "the source to be added");

        assertEquals("Shop", service.added.get(0).projectName());
        assertTrue("no new analysis is started", service.started.isEmpty());
    }

    //-----------------------------------------------------------------------------------------------------------

    /** Opens the dialog and takes the way without topics: "show me what is there". */
    /**
     * The extension point of the dialog, end to end: a source that is not built into it appears
     * as a way for its topic, its page becomes a step, and its plan is what the analysis starts with.
     */
    @Test
    public void aContributedSourceIsOfferedShownAndStarted() throws Exception {
        AnalysisSourceDefinition appliance = new AnalysisSourceDefinition("appliance", "Appliance", false, true);
        List<AnalysisSourceProvider> providers = new ArrayList<>();
        AnalysisSourceRegistry.standard().definitions().forEach(definition ->
                providers.add(AnalysisSourceRegistry.standard().require(definition.id())));
        providers.add(new AnalysisSourceProvider() {
            @Override public AnalysisSourceDefinition definition() { return appliance; }
            @Override public Map<AnalysisTopic, AnalysisSourceOption> topics() {
                return Map.of(AnalysisTopic.LINUX, new AnalysisSourceOption(appliance, "Each appliance is asked directly."));
            }
        });
        onFx(() -> {
            dialog = new NewAnalysisDialog(service, null, new AnalysisSourceRegistry(providers), List.of(new AppliancePage()));
            dialog.show();
            tile(AnalysisTopic.LINUX).setSelected(true);
            button("#continue").fire();
            source("LINUX-APPLIANCE").fire();
            button("#continue").fire();
            return null;
        });

        assertEquals("Where are the appliances for Linux / Unix?", text("#stepTitle"));
        assertTrue("the page of the source is the last step", onFx(() -> visible("#applianceAddress")));
        assertTrue("nothing entered, nothing to start", onFx(() -> button("#startAnalysis").isDisabled()));

        type("#applianceAddress", "appliance-01");
        assertTrue("the analysis needs its name", onFx(() -> button("#startAnalysis").isDisabled()));
        type("#applianceName", "Appliances");
        assertFalse(onFx(() -> button("#startAnalysis").isDisabled()));

        onFx(() -> {
            button("#startAnalysis").fire();
            return null;
        });
        await(() -> !service.startedAny.isEmpty(), "the analysis to start");
        CollectionPlan plan = service.startedAny.get(0);
        assertEquals("Appliances", plan.projectName());
        assertEquals("appliance", plan.sourceType());
        assertEquals("appliance-01", plan.endpoint());
    }

    /** A page as a source outside this dialog contributes it: an address, and the name of the analysis. */
    private static final class AppliancePage implements AnalysisSourceWizardPage {
        private final TextField address = new TextField();
        private final TextField name = new TextField();
        private final VBox node = new VBox(address, name);

        AppliancePage() {
            address.setId("applianceAddress");
            name.setId("applianceName");
        }

        @Override public String sourceId() { return "appliance"; }
        @Override public Node node() { return node; }
        @Override public String title(Set<AnalysisTopic> topics) {
            return "Where are the appliances for " + topics.iterator().next().title() + "?";
        }
        @Override public String hint(Set<AnalysisTopic> topics, String existingAnalysis) { return "Name one appliance."; }
        @Override public boolean isComplete() { return address.getText() != null && !address.getText().isBlank(); }

        @Override
        public void attach(StringProperty analysisName, boolean nameIsFixed, Runnable changed) {
            name.textProperty().bindBidirectional(analysisName);
            name.setDisable(nameIsFixed);
            address.textProperty().addListener((o, ov, nv) -> changed.run());
        }

        @Override
        public CollectionPlan plan(String projectName) {
            String endpoint = address.getText();
            return new CollectionPlan() {
                @Override public String projectName() { return projectName; }
                @Override public String sourceType() { return "appliance"; }
                @Override public String endpoint() { return endpoint; }
                @Override public String secret() { return null; }
                @Override public String toCollectorConfig(String storedProjectName) { return "metricGroups: {}"; }
                @Override public boolean isComplete() { return true; }
                @Override public String firstHostGroup() { return "appliances"; }
                @Override public List<String> firstMetrics() { return List.of("load"); }
            };
        }
    }

    private void open(String existingAnalysis) throws Exception {
        show(existingAnalysis);
        onFx(() -> {
            ((ButtonBase) dialog.getDialogPane().lookup("#tile-explore")).fire();
            return null;
        });
    }

    /** Opens the dialog at its first step, the choice of topics. */
    private void show(String existingAnalysis) throws Exception {
        onFx(() -> {
            dialog = new NewAnalysisDialog(service, existingAnalysis);
            dialog.show();
            ((TextField) dialog.getDialogPane().lookup("#prometheusUrl")).setText("http://prometheus.example:9090");
            return null;
        });
    }

    /** Chooses topics and takes the usual source of each: the Prometheus server. */
    private void choose(AnalysisTopic... topics) throws Exception {
        onFx(() -> {
            for (AnalysisTopic topic : topics) {
                tile(topic).setSelected(true);
            }
            button("#continue").fire();     // to the sources
            button("#continue").fire();     // to the server
            return null;
        });
    }

    /** Chooses Linux and, for it, the hosts read directly. */
    private void chooseLinuxBySsh() throws Exception {
        onFx(() -> {
            tile(AnalysisTopic.LINUX).setSelected(true);
            button("#continue").fire();
            source("LINUX-SSH_TOP").fire();
            button("#continue").fire();
            return null;
        });
    }

    private ToggleButton source(String topicAndSource) {
        return (ToggleButton) dialog.getDialogPane().lookup("#source-" + topicAndSource);
    }

    private void typeArea(String selector, String text) throws Exception {
        onFx(() -> {
            ((javafx.scene.control.TextArea) dialog.getDialogPane().lookup(selector)).setText(text);
            return null;
        });
    }

    private String areaText(String selector) throws Exception {
        return onFx(() -> ((javafx.scene.control.TextArea) dialog.getDialogPane().lookup(selector)).getText());
    }

    private void type(String selector, String text) throws Exception {
        onFx(() -> {
            ((TextField) dialog.getDialogPane().lookup(selector)).setText(text);
            return null;
        });
    }

    /** The choices between essentials and everything of all groups, as they read. */
    private List<String> radios() throws Exception {
        return onFx(() -> dialog.getDialogPane().lookupAll(".radio-button").stream()
                .map(node -> ((RadioButton) node).getText()).collect(Collectors.toList()));
    }

    private ToggleButton tile(AnalysisTopic topic) {
        return (ToggleButton) dialog.getDialogPane().lookup("#tile-" + topic.name());
    }

    /** Whether the node is shown, which also needs every parent to be shown. */
    private boolean visible(String selector) {
        for (Node node = dialog.getDialogPane().lookup(selector); node != null; node = node.getParent()) {
            if (!node.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** The discovered groups in the order shown, with their selection. */
    private List<String> groups() throws Exception {
        return onFx(() -> dialog.getDialogPane().lookupAll(".check-box").stream()
                .filter(node -> node.getId() != null && node.getId().startsWith("group-"))
                .map(node -> (CheckBox) node)
                .map(box -> box.getText().split("\\s+")[0] + (box.isSelected() ? " [x]" : " [ ]"))
                .collect(Collectors.toList()));
    }

    /** The texts of all labels of a row, joined. */
    private String textOf(String selector) throws Exception {
        return onFx(() -> ((Pane) dialog.getDialogPane().lookup(selector)).getChildren().stream()
                .map(node -> ((Label) node).getText()).collect(Collectors.joining(" ")));
    }

    private void connect() throws Exception {
        onFx(() -> {
            button("#connect").fire();
            return null;
        });
        await(() -> onFx(() -> dialog.getDialogPane().lookup("#group-node") != null), "the environment to be shown");
        // the test connects right after the dialog opened; let the dialog finish what it queued
        onFx(() -> null);
    }

    private Button button(String selector) {
        return (Button) dialog.getDialogPane().lookup(selector);
    }

    private String text(String selector) throws Exception {
        return onFx(() -> {
            Node node = dialog.getDialogPane().lookup(selector);
            return node instanceof Label label ? label.getText() : ((TextField) node).getText();
        });
    }

    private static void await(Callable<Boolean> condition, String what) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.call()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("timeout waiting for " + what);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    /** Stands in for the service: answers the discovery and records what the dialog asks for. */
    private static final class FakeService extends NewAnalysisService {

        final List<PrometheusPlan> started = new CopyOnWriteArrayList<>();
        final List<PrometheusPlan> added = new CopyOnWriteArrayList<>();
        /** Every plan started or added, of whatever source. */
        final List<CollectionPlan> startedAny = new CopyOnWriteArrayList<>();
        final List<CollectionPlan> addedAny = new CopyOnWriteArrayList<>();
        volatile RuntimeException addFailure;
        volatile String inspectedAs;
        /** Why the machine of the test cannot read Windows performance counters; {@code null}: it can. */
        volatile String windowsProblem = "Windows performance counters are read by a workbench that runs on Windows.";
        volatile RuntimeException discoverFailure;
        volatile List<Group> groups = List.of(NODE, API);
        volatile int discoveries;

        private final Repository repository = mock(Repository.class);

        FakeService() {
            super(null, null, null);
            when(repository.getId()).thenReturn("local");
        }

        @Override
        public PrometheusEnvironment discover(String url, String username, String password) {
            if (discoverFailure != null) {
                throw discoverFailure;
            }
            discoveries++;
            return new PrometheusEnvironment(url, new ArrayList<>(groups));
        }

        @Override
        public Project startAnalysis(CollectionPlan plan) {
            startedAny.add(plan);
            if (plan instanceof PrometheusPlan prometheus) {
                started.add(prometheus);
            }
            return new Project(plan.projectName());
        }

        @Override
        public String addSource(CollectionPlan plan) {
            if (addFailure != null) {
                throw addFailure;
            }
            addedAny.add(plan);
            if (plan instanceof PrometheusPlan prometheus) {
                added.add(prometheus);
            }
            return "source-id";
        }

        @Override
        public List<SshDiscovery.HostCheck> inspectHosts(List<SshPlan.Host> hosts, String username, String password) {
            inspectedAs = username + "/" + password;
            return hosts.stream().map(host -> host.name().equals("db-02")
                    ? new SshDiscovery.HostCheck(host, false, "Connection refused", List.of())
                    : new SshDiscovery.HostCheck(host, true, null, List.of(
                            new SshDiscovery.ProcessCandidate("java", 2, 20.0, 3.0),
                            new SshDiscovery.ProcessCandidate("postgres", 1, 5.0, 1.0)))).toList();
        }

        @Override
        public String windowsCountersProblem() {
            return windowsProblem;
        }

        @Override
        public List<WindowsDiscovery.HostCheck> inspectWindowsHosts(List<SshPlan.Host> hosts) {
            return hosts.stream().map(host -> host.name().equals("srv-02")
                    ? new WindowsDiscovery.HostCheck(host, false, "is the service 'Remote Registry' running?", List.of())
                    : new WindowsDiscovery.HostCheck(host, true, null, List.of(
                            new WindowsDiscovery.ProcessCandidate("sqlservr", 1, 2e9),
                            new WindowsDiscovery.ProcessCandidate("java", 1, 9e8)))).toList();
        }

        @Override
        public Repository getRepository() {
            return repository;
        }
    }
}
