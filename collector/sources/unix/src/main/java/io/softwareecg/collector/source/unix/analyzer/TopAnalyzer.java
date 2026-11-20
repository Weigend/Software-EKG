//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 21.12.2010
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-07-21 13:03:04 +0200 (Mi, 21 Jul 2010) $
//       revision:      $Revision: 1082 $
//       description:   Unix-Top Analyzer
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterSourceListener;
import io.softwareecg.collector.source.unix.counters.top.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tested with linux only. Analyzes top output.
 */
public class TopAnalyzer extends AbstractTerminalAnalyzer {

    /**
     * a flag which is true if the TopAnalyzer is used with wildcards in the process counters.-
     * That are the only ones which changes with the time. The flag will polled if the wildcard
     * list have to check for updated data or not
     */
    private boolean processWildcardsUsed = false;

    /**
     * Derived counter of the CPU group: everything but idle time, in percent. Like the other
     * derived counters it has to be asked for by name; wildcards do not include it.
     */
    public static final String USED = "used";

    /**
     * Derived counter of the memory group: the memory available to programs, in bytes.
     */
    public static final String AVAILABLE = "available";

    /**
     * Derived counter of the memory and the swap group: the share in use, in percent.
     */
    public static final String USED_PERCENT = "usedPercent";

    /**
     * an unique fingerprint of the process list. If the content of
     * the process list changes, the fingerprint will too
     */
    private long processListFingerprint = -1;

    /**
     * a cached list of the listed process (command) names.
     */
    private Collection<String> processNames = null;


    //**********************************************************************************************
    //  public API
    //**********************************************************************************************

    /**
     * Minimal Constructor of the TopAnalyzer class.
     *
     * @param topOutput the output from the Unix top-command as String[]
     */
    public TopAnalyzer(String[] topOutput) {
        this.terminalOutput = topOutput != null ? topOutput.clone() : null;
        removeGarbage();
    }

    /**
     * Extended Constructor of the TopAnalyzer class, with an optional list of CounterSourceListeners.
     *
     * @param topOutput the output from the Unix top-command as String[]
     * @param counterSourceListeners a collection with CounterSourceListener
     */
    public TopAnalyzer(String[] topOutput, Collection<CounterSourceListener> counterSourceListeners) {
        this.counterSourceListeners = counterSourceListeners;
        this.terminalOutput = topOutput != null ? topOutput.clone() : null;
        removeGarbage();
    }

    /**
     * In rare cases it could be that in front of the top output some messages are printed.
     */
    private void removeGarbage() {
        if (this.terminalOutput == null) {
            return;
        }
        for (int i = 0; i < this.terminalOutput.length; i++) {
            if (this.terminalOutput[i].startsWith("top")) {
                this.terminalOutput = Arrays.copyOfRange(this.terminalOutput, i, this.terminalOutput.length);
                return;
            }
        }
    }

    /**
     * Sets the output from the terminal.
     *
     * @param topOutput the new top output
     */
    public void setTerminalOutput(String[] topOutput) {
        this.terminalOutput = topOutput != null ? topOutput.clone() : null;

        // some hosts print messages before the output of top; they are removed
        terminalOutput = removeInvalidContent(terminalOutput);

        if (processWildcardsUsed && processListFingerprint == -1) {
            processListFingerprint = AnalyzerStringUtils.getFingerPrint(getProcessNames());
        }

        processNames = null;

        if (processWildcardsUsed) {
            long newFingerprint = AnalyzerStringUtils.getFingerPrint(getProcessNames());

            if (newFingerprint != processListFingerprint) {
                processListFingerprint = newFingerprint;
                invokeCounterChangeListener();
            }
        }
    }

    /**
     * Methods that will clean-up unwanted blocks from the ssh terminal output than calling
     * top for the first time. This may occur than the ssh daemon send some kind of error messages
     * that will always show at the begin. For this the function strips each line from the output
     * that until it begins with "top" ... as first expected top output
     *
     * @param terminalOutput the original terminal output
     * @return the stripped terminal output
     */
    private String[] removeInvalidContent(String[] terminalOutput) {


        for (int index = 0; index < terminalOutput.length; index++) {

            if (!terminalOutput[index].startsWith("top")) {
                continue;
            }

            return Arrays.copyOfRange(terminalOutput, index, terminalOutput.length);
        }

        throw new IllegalStateException("Unable to find expected top process name in any line of the output!");
    }

    /**
     * Returns a collection of counters that replace the given one. If the given
     * MetricDefinition hasn't a wildcard, the collection only contains the given one.
     * Otherwise it will contain one or more Counters matching to the wildcard.
     *
     * @param metricDefinition the MetricDefinition to investigate
     * @return a collection with replacement counters
     */
    public Collection<? extends MetricDefinition> expandCounter(MetricDefinition metricDefinition) {

        processWildcardsUsed = false;

        String originalCounterDefinition = metricDefinition.getDefinition();

        // convert wildcards to regex expression like *abc* -> .*abc.*
        originalCounterDefinition = originalCounterDefinition.replace("*", ".*");

        HashSet<MetricDefinition> newMetricDefinitions = new HashSet<>();

        String groupName = extractByRegex(originalCounterDefinition, COUNTER_SEARCHGROUP_PATTERN, 1);
        String counterName = extractByRegex(originalCounterDefinition, COUNTER_SEARCHCOUNTER_PATTERN, 1);

        if (groupName.contains("*")) {
            newMetricDefinitions.addAll(expandGroup(groupName, counterName));
        }

        if (counterName.contains("*") && newMetricDefinitions.size() == 0) {
            newMetricDefinitions.addAll(createCounters(originalCounterDefinition, counterName));

        } else {
            // only add the original MetricDefinition if no custom counters
            // added before that replaced the original one
            if (newMetricDefinitions.size() == 0) {
                newMetricDefinitions.add(metricDefinition);
            }
        }

        return newMetricDefinitions;
    }

    private Collection<MetricDefinition> createCounters(String originalCounterDefinition, String counterName) {
        TopCounterGroupEnum groupNameEnum = extractGroup(originalCounterDefinition);

        switch (groupNameEnum) {
            case Process:
                return getProcessWildcardCounter(originalCounterDefinition
                        .substring(0, originalCounterDefinition.lastIndexOf('\\') + 1), counterName);
            case Memory:
                return TopCounters.getMemoryWildcardCounter(counterName);
            case Swap:
                return TopCounters.getSwapWildcardCounter(counterName);
            case Tasks:
                return TopCounters.getTaskWildcardCounter(counterName);
            case Cpu:
                return TopCounters.getCpuWildcardCounter(counterName);
            case User:
                return Collections.singletonList(TopCounters.getUserCounter());
            case Load:
                return TopCounters.getLoadWildcardCounter(counterName);
            case Uptime:
                return Collections.singletonList(TopCounters.getUptimeCounter());
            default:
                throw new IllegalStateException("Illegal group. No known handler.");
        }
    }

    private Set<MetricDefinition> expandGroup(String groupName, String counterName) {
        HashSet<MetricDefinition> newMetricDefinitions = new HashSet<>();

        // if groupName contains a '(', it has to be a 'PROCESS'.
        // In this case don't threat wildcard in outside the braces.
        if (groupName.contains("(")) {
            String processName = groupName.substring(groupName.indexOf('(') + 1, groupName.indexOf(')'));
            if (processName.contains(".*")) {

                processWildcardsUsed = true;

                Collection<String> names = getProcessNames();
                if (processName.length() == 2) {
                    for (String name : names) {
                        newMetricDefinitions.addAll(getProcessWildcardCounter("\\Process(" + name + ")\\", counterName));
                    }
                } else {
                    Pattern pattern = Pattern.compile(processName);
                    for (String name : names) {
                        if (pattern.matcher(name).matches()) {
                            newMetricDefinitions.addAll(getProcessWildcardCounter("\\Process(" + name + ")\\", counterName));
                        }
                    }
                }
            }

        } else {

            if (groupName.length() == 2) {
                newMetricDefinitions.addAll(TopCounters.getCpuWildcardCounter(counterName));
                newMetricDefinitions.addAll(TopCounters.getMemoryWildcardCounter(counterName));
                newMetricDefinitions.addAll(TopCounters.getSwapWildcardCounter(counterName));
                newMetricDefinitions.addAll(TopCounters.getTaskWildcardCounter(counterName));
                newMetricDefinitions.add(TopCounters.getUserCounter());
                newMetricDefinitions.add(TopCounters.getUptimeCounter());
                newMetricDefinitions.addAll(TopCounters.getLoadWildcardCounter(counterName));

            } else {
                Pattern pattern = Pattern.compile(groupName);

                if (pattern.matcher(TopCounterGroupEnum.Cpu.name()).matches()) {
                    newMetricDefinitions.addAll(TopCounters.getCpuWildcardCounter(counterName));
                }

                if (pattern.matcher(TopCounterGroupEnum.Memory.name()).matches()) {
                    newMetricDefinitions.addAll(TopCounters.getMemoryWildcardCounter(counterName));
                }

                if (pattern.matcher(TopCounterGroupEnum.Swap.name()).matches()) {
                    newMetricDefinitions.addAll(TopCounters.getSwapWildcardCounter(counterName));
                }

                if (pattern.matcher(TopCounterGroupEnum.Tasks.name()).matches()) {
                    newMetricDefinitions.addAll(TopCounters.getTaskWildcardCounter(counterName));
                }

                if (pattern.matcher(TopCounterGroupEnum.User.name()).matches()) {
                    newMetricDefinitions.add(TopCounters.getUserCounter());
                }

                if (pattern.matcher(TopCounterGroupEnum.Uptime.name()).matches()) {
                    newMetricDefinitions.add(TopCounters.getUptimeCounter());
                }

                if (pattern.matcher(TopCounterGroupEnum.Load.name()).matches()) {
                    newMetricDefinitions.addAll(TopCounters.getLoadWildcardCounter(counterName));
                }
            }
        }
        return newMetricDefinitions;
    }

    @Override
    public double getFilteredCounter(String metricName, String regexPostFilter) {

        if (extractGroup(metricName) != TopCounterGroupEnum.Process) {
            return getCounter(metricName);
        }

        Pattern postFilterPattern;

        if (StringUtils.isBlank(regexPostFilter)) {
            postFilterPattern = null;
        } else {
            postFilterPattern = Pattern.compile(regexPostFilter, Pattern.CASE_INSENSITIVE);
        }

        return getProcessCounter(metricName, extractCounter(metricName), postFilterPattern);
    }

    /**
     * Return the metric value captured from the Unix top output.
     *
     * @param name the name of the metric the caller requests the metric value for
     * @return the resolved metric value for the current time
     */
    @Override
    public double getCounter(String name) {
        TopCounterGroupEnum groupName = extractGroup(name);
        String counter = extractCounter(name);

        double result;
        switch (groupName) {
            case Process:
                result = getProcessCounter(name, counter, null);
                break;
            case Memory:
                result = getMemoryCounter(counter);
                break;
            case Swap:
                result = getSwapCounter(counter);
                break;
            case Tasks:
                result = getTasksCounter(counter);
                break;
            case Cpu:
                result = getCPUCounter(counter);
                break;
            case User:
                result = getUserCounter(counter);
                break;
            case Load:
                result = getLoadCounter(counter);
                break;
            case Uptime:
                result = getUptimeCounter(counter);
                break;
            default:
                throw new IllegalStateException("Illegal group. No known handler.");
        }

        return result;
    }

    //**********************************************************************************************
    //  private helpers
    //**********************************************************************************************

    /**
     * Fire counterChanged() to all listeners.
     */
    private void invokeCounterChangeListener() {
        for (CounterSourceListener counterSourceListener : counterSourceListeners) {
            counterSourceListener.counterChanged();
        }
    }

    /**
     * Create and returns a collection of counters for the given MetricDefinition name.
     * The counter name will be expanded if wildcards used
     *
     * @param counterDef  the process counter definition without the counter name
     * @param counterName the counter name (wildcard and/or a name from ProcessCounterEnum)
     * @return a collection with counters
     */
    private Collection<MetricDefinition> getProcessWildcardCounter(String counterDef, String counterName) {

        ArrayList<MetricDefinition> newMetricDefinition = new ArrayList<>();

        if (counterName.contains(".*")) {
            if (counterName.length() == 2) {
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.CPU, counterDef + ProcessCounterEnum.CPU));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.MEM, counterDef + ProcessCounterEnum.MEM));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.NI, counterDef + ProcessCounterEnum.NI));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.PID, counterDef + ProcessCounterEnum.PID));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.PR, counterDef + ProcessCounterEnum.PR));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.RES, counterDef + ProcessCounterEnum.RES));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.S, counterDef + ProcessCounterEnum.S));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.SHR, counterDef + ProcessCounterEnum.SHR));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.TIME, counterDef + ProcessCounterEnum.TIME));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.USER, counterDef + ProcessCounterEnum.USER));
                newMetricDefinition.add(new MetricDefinition(counterDef + ProcessCounterEnum.VIRT, counterDef + ProcessCounterEnum.VIRT));
            } else {
                Pattern pattern = Pattern.compile(counterName);

                for (ProcessCounterEnum pc : ProcessCounterEnum.values()) {
                    if (pattern.matcher(pc.name()).matches()) {
                        newMetricDefinition.add(new MetricDefinition(counterDef + pc.name(), counterDef + pc.name()));
                    }
                }
            }
        } else {
            newMetricDefinition.add(new MetricDefinition(counterDef + counterName, counterDef + counterName));
        }

        return newMetricDefinition;
    }

    // top - 22:43:26 up 10 min,  2 users,  load average: 0.23, 0.25, 0.21

    /**
     * Gets the uptime counter in minutes.
     *
     * @param name the name
     * @return the uptime counter
     */
    private double getUptimeCounter(String name) {
        UptimeCounterEnum up = UptimeCounterEnum.valueOf(name);
        String uptimeResult = extractByRegex(this.terminalOutput[0], ".*" + up + "\\s(.*)\\s\\d+\\suser.*", 1);
        return AnalyzerStringUtils.parseUptimeToMinutes(uptimeResult);
    }

    // Swap:  4144728k total,        0k used,  4144728k free,   172016k cached
    // MiB Swap:  50321.3 total,  50321.3 free,      0.0 used.  81969.4 avail Mem

    /**
     * Gets the swap counter in bytes, or {@value #USED_PERCENT} for the share of the swap space in use.
     *
     * @param name the name
     * @return the swap counter, -1 if the output does not have it
     */
    private double getSwapCounter(String name) {
        if (USED_PERCENT.equals(name)) {
            double total = getSwapCounter(SwapCounterEnum.total.name());
            double used = getSwapCounter(SwapCounterEnum.used.name());
            if (total < 0 || used < 0) {
                return -1;
            }
            return total == 0 ? 0 : 100.0 * used / total;
        }
        SwapCounterEnum mc = SwapCounterEnum.valueOf(name);
        return sizeOf(this.terminalOutput[4], mc.name());
    }

    // Mem:   1406812k total,   475336k used,   931476k free,   143432k buffers
    // MiB Mem :  96546.5 total,  58774.7 free,  14577.2 used,  26575.1 buff/cache

    /**
     * Gets the memory counter in bytes. Besides the counters top prints there are two derived ones:
     * {@value #AVAILABLE}, the memory that can be given to programs without swapping, and
     * {@value #USED_PERCENT}, the share of the memory that is not available.
     *
     * @param name the name
     * @return the memory counter, -1 if the output does not have it
     */
    private double getMemoryCounter(String name) {
        if (AVAILABLE.equals(name)) {
            // newer versions of top print it at the end of the swap line
            return sizeOf(this.terminalOutput[4], "avail Mem");
        }
        if (USED_PERCENT.equals(name)) {
            double total = getMemoryCounter(MemoryCounterEnum.total.name());
            double available = getMemoryCounter(AVAILABLE);
            double used = available >= 0 ? total - available : getMemoryCounter(MemoryCounterEnum.used.name());
            if (total <= 0 || used < 0) {
                return -1;
            }
            return 100.0 * used / total;
        }
        MemoryCounterEnum mc = MemoryCounterEnum.valueOf(name);
        double value = sizeOf(this.terminalOutput[3], mc.name());
        if (value < 0 && mc == MemoryCounterEnum.buffers) {
            value = sizeOf(this.terminalOutput[3], "buff/cache");
        }
        return value;
    }

    /**
     * Reads a size from one of the memory lines of top, in bytes. Older versions print the unit
     * behind every number ({@code 475336k used}), newer ones once in front of the line
     * ({@code MiB Mem : ... 14577.2 used}); the decimal sign may be a comma.
     *
     * @param line the line of the output
     * @param key  what the number is followed by
     * @return the size in bytes, -1 if the line has no such number
     */
    private static double sizeOf(String line, String key) {
        Matcher matcher = Pattern.compile("(?:^|[\\s:])([0-9]+(?:[.,][0-9]+)?)\\s*([kKmMgGtT]?)\\s+" + Pattern.quote(key))
                .matcher(line);
        if (!matcher.find()) {
            return -1;
        }
        double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
        String unit = matcher.group(2);
        if (unit.isEmpty()) {
            Matcher lineUnit = Pattern.compile("^\\s*([KMGTPE])iB\\s").matcher(line);
            // without any unit top means KiB
            unit = lineUnit.find() ? lineUnit.group(1) : "K";
        }
        return value * Math.pow(1024, "KMGTPE".indexOf(unit.toUpperCase().charAt(0)) + 1);
    }

    // Tasks: 142 total,   1 running, 141 sleeping,   0 stopped,   0 zombie

    /**
     * Gets the tasks counter.
     *
     * @param name the name
     * @return the tasks counter
     */
    private double getTasksCounter(String name) {
        TaskCounterEnum mc = TaskCounterEnum.valueOf(name);
        String result = extractByRegex(this.terminalOutput[1], ".* ([0-9]*) " + mc + ".*", 1);
        return AnalyzerStringUtils.convertToDouble(result);
    }

    // Cpu(s):  4.4%us,  8.0%sy,  0.3%ni, 83.4%id,  3.8%wa,  0.1%hi,  0.0%si,  0.0%st
    // %Cpu(s):  1.6 us,  0.9 sy,  0.0 ni, 97.3 id,  0.3 wa,  0.0 hi,  0.0 si,  0.0 st

    /**
     * Gets the cPU counter in percent, or {@value #USED} for everything but idle time.
     *
     * @param name the name
     * @return the cPU counter, -1 if the output does not have it
     */
    private double getCPUCounter(String name) {
        if (USED.equals(name)) {
            double idle = getCPUCounter(CPUCounterEnum.id.name());
            return idle < 0 ? -1 : Math.max(0, 100.0 - idle);
        }
        CPUCounterEnum mc = CPUCounterEnum.valueOf(name);
        // the decimal sign may be a comma, which also separates the values
        Matcher matcher = Pattern.compile("(?:^|[\\s:])([0-9]+(?:[.,][0-9]+)?)(?:\\s|%)\\s*" + mc + "(?:[,\\s]|$)")
                .matcher(this.terminalOutput[2]);
        return matcher.find() ? Double.parseDouble(matcher.group(1).replace(',', '.')) : -1;
    }

    // top - 22:43:26 up 10 min,  2 users,  load average: 0.23, 0.25, 0.21

    /**
     * Gets the user counter.
     *
     * @param name the name
     * @return the user counter
     */
    private double getUserCounter(String name) {
        UserCounterEnum mc = UserCounterEnum.valueOf(name);
        String result = extractByRegex(this.terminalOutput[0], ".* ([0-9]*) " + mc + ".*", 1);
        return AnalyzerStringUtils.convertToDouble(result);
    }

    // top - 22:43:26 up 10 min,  2 users,  load average: 0.23, 0.25, 0.21

    /**
     * Gets the load counter.
     *
     * @param name the name
     * @return the load counter
     */
    private double getLoadCounter(String name) {
        String result = extractByRegex(this.terminalOutput[0], ".*load average: (.*)", 1);
        String[] split = result.split(",\\s+");
        result = split[LoadCounterEnum.valueOf(name).ordinal()];
        // the decimal sign may be a comma
        return AnalyzerStringUtils.convertToDouble(result.trim().replace(',', '.'));
    }

    // PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
    // 3533 root      20   0 40584  11m 6912 S   45  0.9   1:08.48 /usr/X11R6/bin/X :0 -br -audit 0 -auth /var/lib/gdm/:0.Xauth -nolisten tcp vt7

    /**
     * Gets the process counter.
     *
     * @param name    the name
     * @param regexProcessFilter an regex filter the lookup process must match if not null
     * @param counter the counter
     * @return the process counter
     */
    private double getProcessCounter(String name, String counter, Pattern regexProcessFilter) {

        int idx = findIndexOfProcess(extractByRegex(name, "\\\\Process\\((.*)\\)\\\\.*", 1), regexProcessFilter);
        if (idx == -1) {
            return idx;
        }

        ProcessCounterEnum counterType = ProcessCounterEnum.valueOf(counter);

        String line = this.terminalOutput[idx];
        String[] splits = line.trim().split(SPACES_REGEX);
        splits = AnalyzerStringUtils.removeEmptySplits(splits);

        String result = splits[counterType.ordinal()];
        return AnalyzerStringUtils.convertToDouble(result);
    }

    /**
     * Finds the index of the line in the top result. We support the process#x Syntax of WPC here.
     *
     * @param processDefExpression the process name with or without arguments or #n to select the n-th process
     * @param regexProcessFilter an regex filter the lookup process must match if not null
     * @return the index of the right line.
     */
    private int findIndexOfProcess(String processDefExpression, Pattern regexProcessFilter) {

        // support multiple instances by using #1 #2 #3 syntax
        // #1 is the process with the lowest pid
        String[] multiInstance = processDefExpression.split("#");
        String processName = multiInstance[0];
        int instanceNr = multiInstance.length == 2 ? Integer.parseInt(multiInstance[1]) : 0;

        List<Integer> processLines = new ArrayList<>();
        for (int i = 0; i < terminalOutput.length; i++) {
            String completeLine = terminalOutput[i];
            // strip #[0-9]+
            if (matchesProcessFilters(completeLine, processName, regexProcessFilter)) {
                processLines.add(i);
            }
        }

        // no process with name found
        if (processLines.isEmpty()) {
            return -1; // not found
        }

        // exact one process found
        if (processLines.size() == 1) {
            return processLines.get(instanceNr);
        }

        // order lines by pid and return the matching line index
        processLines.sort((idx1, idx2) -> {
            String line1 = terminalOutput[idx1];
            String line2 = terminalOutput[idx2];
            String[] splits1 = line1.trim().split(SPACES_REGEX);
            String[] splits2 = line2.trim().split(SPACES_REGEX);

            try {
                String spit1 = AnalyzerStringUtils.removeEmptySplits(splits1)[0];
                String spit2 = AnalyzerStringUtils.removeEmptySplits(splits2)[0];


                Integer pid1 = Integer.parseInt(spit1);
                Integer pid2 = Integer.parseInt(spit2);
                // extract PID and compare
                return pid1.compareTo(pid2);
            } catch (Exception e) {
                return 0;
            }
        });


        if (instanceNr < processLines.size()) {
            return processLines.get(instanceNr);
        }

        // not enough instances
        return -1;
    }

    private boolean matchesProcessFilters(String topLine, String processName, Pattern regexProcessFilter) {

        if (!topLine.contains(processName)) {
            return false;
        }

        if (regexProcessFilter == null) {
            return true;
        }

        // first extract process name an all it's args
        String[] lineTokens = topLine.trim().split(SPACES_REGEX);
        int startOfProcessNameAndArgs = 11;

        StringBuilder processCommandBuilder = new StringBuilder(lineTokens[startOfProcessNameAndArgs]);
        for (int tokenIndex = startOfProcessNameAndArgs + 1; tokenIndex < lineTokens.length; tokenIndex++) {
            processCommandBuilder.append(" ").append(lineTokens[tokenIndex]);
        }

        // return the result if process and it's args matches to the filter
        return regexProcessFilter.matcher(processCommandBuilder.toString()).matches();
    }


    /**
     * Extract the group enum.
     *
     * @param name the complete counter string.
     * @return a group enum.
     */
    private TopCounterGroupEnum extractGroup(String name) {
        String group = extractByRegex(name, "\\\\(.*)\\\\.*", 1);
        if (group.startsWith(TopCounterGroupEnum.Process.toString())) {
            return TopCounterGroupEnum.Process;
        }
        return TopCounterGroupEnum.valueOf(StringUtils.capitalize(group.toLowerCase()));
    }

    /**
     * Return a collection with the names of all processes
     * listed by the top output.
     *
     * @return a collection with the names (or the call path + arguments) of all processes
     */
    private Collection<String> getProcessNames() {
        if (processNames == null) {
            ArrayList<String> newProcessNames = new ArrayList<>();

            // the split limit including the empty column at beginning and +1 for different counting (limit begins at 1)
            int limit = ProcessCounterEnum.COMMAND.ordinal() + 2;

            // skip the first 7 lines, because they are filled with the global information's and the column names
            for (int lineIndex = 7; lineIndex < terminalOutput.length; lineIndex++) {
                String[] splits = terminalOutput[lineIndex].split(SPACES_REGEX, limit);
                if (splits.length > ProcessCounterEnum.COMMAND.ordinal()) {
                    splits = AnalyzerStringUtils.removeEmptySplits(splits);
                    newProcessNames.add(splits[ProcessCounterEnum.COMMAND.ordinal()]);
                }
            }

            processNames = newProcessNames;
        }

        return processNames;
    }

}
