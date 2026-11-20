package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterSourceListener;
import io.softwareecg.collector.source.unix.counters.iostat.IOStatCounterEnum;
import io.softwareecg.collector.source.unix.counters.iostat.IOStatCounters;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * @since 06.03.13
 */
public class IOStatAnalyzer extends AbstractTerminalAnalyzer {

    private int lastTimeTerminalLineAmount = 0;

    private final Map<String, Integer> devicePathIndexMap = new HashMap<>();
    private final Map<String, Integer> filesystemPathIndexMap = new HashMap<>();
    private final Map<String, Integer> filesystemCounterIndexMap = new HashMap<>();
    private final Map<String, Integer> deviceCounterIndexMap = new HashMap<>();

    /**
     * the logger of this class
     */
    protected static final Logger LOGGER = LoggerFactory.create();

    /**
     * Some systems do not deliver file systems
     */
    private boolean systemHasFilesSystems = false;

    /**
     * Default constructor with no action (and no source listeners).
     */
    public IOStatAnalyzer() {
        //empty constructor
    }

    /**
     * Constructor with terminal output and listeners.
     *
     * @param counterSourceListeners the source listeners.
     */
    public IOStatAnalyzer(Collection<CounterSourceListener> counterSourceListeners) {
        this.counterSourceListeners = counterSourceListeners;
    }

    @Override
    public Collection<? extends MetricDefinition> expandCounter(MetricDefinition metricDefinition) {

        String originalCounterDefinition = metricDefinition.getDefinition();
        // wildcards like w32 *abc* -> .*abc.*
        originalCounterDefinition = originalCounterDefinition.replace("*", ".*");

        Collection<MetricDefinition> newMetricDefinitions = new HashSet<>();

        String groupName = extractByRegex(originalCounterDefinition, COUNTER_SEARCHGROUP_PATTERN, 1);
        String counterName = extractByRegex(originalCounterDefinition, COUNTER_SEARCHCOUNTER_PATTERN, 1);

        if (groupName.contains("(")) {

            int startPosBrace = groupName.indexOf('(');
            String lookupPathName = groupName.substring(startPosBrace + 1, groupName.indexOf(')'));
            String lookupCounterType = groupName.substring(0, startPosBrace);

            if (IOStatCounterEnum.Device.name().matches(lookupCounterType)) {
                Collection<String> devicePathNames = getPathNames(getDeviceLines());
                for (String availablePathName : devicePathNames) {
                    if (availablePathName.matches(lookupPathName)) {
                        newMetricDefinitions.addAll(IOStatCounters.getDeviceWildcardCounter(availablePathName, counterName, deviceCounterIndexMap.keySet()));
                    }
                }
            }

            if (IOStatCounterEnum.Filesystem.name().matches(lookupCounterType)) {
                Collection<String> filesystemPathNames = getPathNames(getFilesystemLines());
                for (String availablePathName : filesystemPathNames) {
                    if (availablePathName.matches(lookupPathName)) {
                        newMetricDefinitions.addAll(IOStatCounters.getFilesystemWildcardCounter(availablePathName, counterName, filesystemCounterIndexMap.keySet()));
                    }
                }
            }

        } else {
            // if groupName.length == 2 it must be '.*' and in this case we
            // will return all counters of all devices/paths
            if (groupName.length() == 2) {

                for (String availablePathName : getPathNames(getDeviceLines())) {
                    newMetricDefinitions.addAll(IOStatCounters.getDeviceWildcardCounter(availablePathName, counterName, deviceCounterIndexMap.keySet()));
                }

                for (String availablePathName : getPathNames(getFilesystemLines())) {
                    newMetricDefinitions.addAll(IOStatCounters.getFilesystemWildcardCounter(availablePathName, counterName, filesystemCounterIndexMap.keySet()));
                }

            } else {
                throw new IllegalArgumentException("the metricDefinition groupName has an unsupported format '"
                        + groupName + "'");
            }
        }

        return newMetricDefinitions;
    }

    @Override
    public double getCounter(String name) {

        if (terminalOutput == null) {
            return -1;
        }

        String groupType = extractByRegex(name, COUNTER_SEARCHGROUP_PATTERN, 1);
        String path = groupType.substring(groupType.indexOf('(') + 1, groupType.indexOf(')'));

        if (groupType.startsWith(IOStatCounterEnum.Device.name())) {
            return getDeviceCounter(path, extractCounter(name));

        } else if (groupType.startsWith(IOStatCounterEnum.Filesystem.name())) {
            return getFilesystemCounter(path, extractCounter(name));

        } else {
            throw new IllegalStateException("Illegal group. No known handler.");
        }
    }

    @Override
    public void setTerminalOutput(String[] pTerminalOutput) {

        try {

            // Search for the last line that starts with "Device:". iostat generates a new report every x seconds. We are
            // only interested in the last report. Everything before the last occurrence of "Device:" is removed.
            int startIdx;
            for (startIdx = pTerminalOutput.length - 1; startIdx >= 0; --startIdx) {
                if (StringUtils.startsWith(pTerminalOutput[startIdx], "Device:")) {
                    break;
                }
            }
            this.terminalOutput = Arrays.copyOfRange(pTerminalOutput, startIdx, pTerminalOutput.length);

            // init Index-Map if required
            if (deviceCounterIndexMap.size() == 0) {
                initCounterIndexMap(this.terminalOutput);
            }

            if (lastTimeTerminalLineAmount == 0 || lastTimeTerminalLineAmount != this.terminalOutput.length) {
                createNewPathIndex();
                lastTimeTerminalLineAmount = this.terminalOutput.length;
            }
        } catch (Exception e) {
            LOGGER.error("cannot apply new terminal output", e);
        }

    }


    private Collection<String> getPathNames(String[] pSource) {
        Collection<String> result = new ArrayList<>();

        for (String lineString : pSource) {
            result.add(lineString.split(SPACES_REGEX)[0]);
        }

        return result;
    }

    private void createNewPathIndex() {
        String[] deviceLines = getDeviceLines();
        int startDeviceLines = ArrayUtils.indexOf(terminalOutput, deviceLines[0]);
        int endDeviceLines = startDeviceLines + deviceLines.length;

        for (int index = startDeviceLines; index <= endDeviceLines; index++) {
            devicePathIndexMap.put(terminalOutput[index].split(SPACES_REGEX)[0], index);
        }


        if (systemHasFilesSystems) {
            String[] filesystemLines = getFilesystemLines();
            int startFilesystemLines = ArrayUtils.indexOf(terminalOutput, filesystemLines[0], startDeviceLines);
            int endFilesystemLines = startFilesystemLines + filesystemLines.length;

            for (int index = startFilesystemLines; index <= endFilesystemLines; index++) {
                filesystemPathIndexMap.put(terminalOutput[index].split(SPACES_REGEX)[0], index);
            }
        }
    }

    private void initCounterIndexMap(String[] pTerminalOutput) throws IOException {
        int deviceCounterNameLine = AnalyzerStringUtils.searchLineIndex(pTerminalOutput, "Device:", 0);

        if (deviceCounterNameLine < 0) {
            throw new IOException("unable to parse terminal content. Line with 'Device' token not found!");
        }

        String[] deviceCounterNames = pTerminalOutput[deviceCounterNameLine].split(SPACES_REGEX);

        for (int index = 1; index < deviceCounterNames.length; index++) {
            deviceCounterIndexMap.put(deviceCounterNames[index], index);
        }

        int fileSystemCounterNameLine = AnalyzerStringUtils.searchLineIndex(pTerminalOutput, "Filesystem:", deviceCounterNameLine);
        if (fileSystemCounterNameLine > 0) {
            systemHasFilesSystems = true;
            String[] filesystemCounterNames = pTerminalOutput[fileSystemCounterNameLine].split(SPACES_REGEX);

            for (int index = 1; index < filesystemCounterNames.length; index++) {
                filesystemCounterIndexMap.put(filesystemCounterNames[index], index);
            }
        }
    }

    private double getDeviceCounter(String path, String counterName) {
        Integer counterIndex = deviceCounterIndexMap.get(counterName);

        if (counterIndex == null) {
            LOGGER.error("the requested counter '" + counterName + " isn't indexed in " +
                    "the deviceCounterIndexMap. It seems to be not available'");
            return -1;
        }

        Integer pathIndex = devicePathIndexMap.get(path);

        if (pathIndex == null) {
            return -1;
        }

        return Double.parseDouble(terminalOutput[pathIndex].split(SPACES_REGEX)[counterIndex]);
    }

    private double getFilesystemCounter(String path, String counterName) {
        Integer counterIndex = filesystemCounterIndexMap.get(counterName);

        if (counterIndex == null) {
            LOGGER.error("the requested counter '" + counterName + " isn't indexed in " +
                    "the filesystemCounterIndexMap. It seems to be not available'");
            return -1;
        }

        Integer pathIndex = filesystemPathIndexMap.get(path);

        if (pathIndex == null) {
            return -1;
        }

        return Double.parseDouble(terminalOutput[pathIndex].split("\\s{3,}")[counterIndex]);
    }


    private String[] getDeviceLines() {
        return AnalyzerStringUtils.getMatchingSubArray(terminalOutput, "Device:", "DeviceList");
    }

    private String[] getFilesystemLines() {
        return AnalyzerStringUtils.getMatchingSubArray(terminalOutput, "Filesystem:", "Filesystem/NFS-List");
    }

}
