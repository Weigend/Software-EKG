package io.softwareecg.collector.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.softwareecg.collector.control.CollectorFrontend;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.source.CounterSource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * What keeps the parts of the collector apart. Between its modules the compiler does; these rules
 * do it for the packages, and for what a module boundary does not say. The test is in this module
 * because only here all of them come together.
 * <p>
 * See docs/architecture/collector-components.md.
 */
public class CollectorArchitectureTest {

    private static final String ROOT = "io.softwareecg.collector.";

    /** What is found at runtime: the packages below the contracts, and the display. */
    private static final String[] PLUGINS = {ROOT + "source.*..", ROOT + "repository.*..", ROOT + "ui.."};

    private static JavaClasses collector;

    @BeforeClass
    public static void readTheClasses() {
        collector = new ClassFileImporter()
                .withImportOption(location -> !location.contains("/test-classes/") && !location.contains("-tests.jar"))
                .importPackages("io.softwareecg.collector");
        for (String part : List.of("control", "source.unix", "source.prometheus", "source.wincount", "repository.csv",
                "repository.rdb.solr", "ui")) {
            assertTrue("the classes of " + part + " are read", collector.stream()
                    .anyMatch(type -> type.getPackageName().startsWith(ROOT + part)));
        }
    }

    /**
     * In no package its sub-packages and its classes need each other in a circle. A circle of classes
     * of one package is allowed: they belong together.
     */
    @Test
    public void noPackageHasACycleOfItsParts() {
        Map<String, Map<String, Set<String>>> graphs = new TreeMap<>();
        for (JavaClass type : collector) {
            for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                addEdge(graphs, topLevel(type), topLevel(dependency.getTargetClass()));
            }
        }
        List<String> cycles = new ArrayList<>();
        graphs.forEach((parent, graph) -> graph.keySet().forEach(node -> {
            // a package, not a class: its name begins with a small letter
            if (Character.isLowerCase(node.charAt(0)) && reaches(graph, node, node, new HashSet<>())) {
                cycles.add(parent + "." + node);
            }
        }));
        assertEquals("packages that need each other in a circle", List.of(), cycles);
    }

    /** The engine knows the contracts only, and nobody else knows a plugin either. */
    @Test
    public void nobodyNeedsAPlugin() {
        check(noClasses().that().resideOutsideOfPackages(PLUGINS)
                .should().dependOnClassesThat().resideInAnyPackage(PLUGINS)
                .because("a plugin has to be added and removed without touching anything else"));
    }

    @Test
    public void sourcesDoNotKnowEachOther() {
        check(slices().matching(ROOT + "source.(*)..").should().notDependOnEachOther()
                .because("a source has to be added and removed without touching another"));
    }

    /** The archive is made of CSV files, and Solr and Elasticsearch have rdb.api in common. */
    @Test
    public void repositoriesDoNotKnowEachOther() {
        check(slices().matching(ROOT + "repository.(*)..").should().notDependOnEachOther()
                .ignoreDependency(resideInAPackage(ROOT + "repository.archive.."), resideInAPackage(ROOT + "repository.csv.."))
                .because("a repository has to be added and removed without touching another"));
        check(slices().matching(ROOT + "repository.rdb.(*)..").should().notDependOnEachOther()
                .ignoreDependency(alwaysTrue(), resideInAPackage(ROOT + "repository.rdb.api.."))
                .because("Solr and Elasticsearch only have rdb.api in common"));
    }

    /** A source reads values. Who asks for them and where they go is none of its business. */
    @Test
    public void aSourceKnowsNeitherTheEngineNorTheStorage() {
        check(noClasses().that().resideInAPackage(ROOT + "source..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + "control..", ROOT + "repository..", ROOT + "connector..", ROOT + "ui..")
                .because("a source only reads values"));
        check(noClasses().that().resideInAPackage(ROOT + "source..").and().resideOutsideOfPackage(ROOT + "source.calc..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + "memory..")
                .because("only calculated values need the values of others"));
    }

    @Test
    public void aRepositoryKnowsNeitherTheEngineNorTheSources() {
        check(noClasses().that().resideInAPackage(ROOT + "repository..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + "control..", ROOT + "source..", ROOT + "connector..", ROOT + "memory..", ROOT + "ssh..", ROOT + "ui..")
                .because("a repository only writes values"));
    }

    @Test
    public void theFoundationsNeedNothingElse() {
        for (String foundation : List.of("dt", "common", "ssh")) {
            check(noClasses().that().resideInAPackage(ROOT + foundation + "..")
                    .should().dependOnClassesThat(insideTheCollectorButNotIn(foundation))
                    .because(foundation + " is at the bottom"));
        }
    }

    @Test
    public void theInsideOfAPartIsNotUsedByOthers() {
        for (String inside : List.of("config.impl", "connector.nop", "control.jobs", "control.monitor", "ssh.impl")) {
            String part = inside.substring(0, inside.indexOf('.'));
            check(noClasses().that().resideOutsideOfPackage(ROOT + part + "..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + inside + "..")
                    .because(inside + " is inside of " + part));
        }
    }

    /** A plugin that is not registered is not found, and nothing says so. */
    @Test
    public void everyPluginIsRegistered() {
        for (Class<?> contract : List.of(CounterSource.class, RepositoryFactory.class, CollectorFrontend.class)) {
            Set<String> written = collector.stream()
                    .filter(type -> type.isAssignableTo(contract) && !type.isInterface()
                            && !type.getModifiers().contains(JavaModifier.ABSTRACT) && type.isTopLevelClass())
                    .map(JavaClass::getName).collect(Collectors.toCollection(TreeSet::new));
            Set<String> registered = ServiceLoader.load(contract).stream()
                    .map(provider -> provider.type().getName()).collect(Collectors.toCollection(TreeSet::new));
            assertEquals("registered as " + contract.getSimpleName(), written, registered);
        }
    }

    private static void check(ArchRule rule) {
        rule.check(collector);
    }

    private static DescribedPredicate<JavaClass> insideTheCollectorButNotIn(String part) {
        return resideInAPackage("io.softwareecg.collector..").and(not(resideInAnyPackage(ROOT + part + "..")))
                .as("are in the collector, but not in " + part);
    }

    /** A dependency belongs to the package in which the ways to both classes part. */
    private static void addEdge(Map<String, Map<String, Set<String>>> graphs, String from, String to) {
        if (from.equals(to) || !to.startsWith(ROOT)) {
            return;
        }
        String[] source = from.split("\\.");
        String[] target = to.split("\\.");
        int level = 0;
        while (source[level].equals(target[level])) {
            level++;
        }
        graphs.computeIfAbsent(String.join(".", Arrays.copyOf(source, level)), parent -> new TreeMap<>())
                .computeIfAbsent(source[level], node -> new TreeSet<>()).add(target[level]);
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String to, Set<String> seen) {
        for (String next : graph.getOrDefault(from, Set.of())) {
            if (next.equals(to) || seen.add(next) && reaches(graph, next, to, seen)) {
                return true;
            }
        }
        return false;
    }

    private static String topLevel(JavaClass type) {
        return type.getBaseComponentType().getName().replaceAll("\\$.*", "");
    }
}
