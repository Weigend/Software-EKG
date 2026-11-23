package io.softwareecg.awb.runtime.config;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EKG default config store that provides any kind of config for
 * each module of the Software-EKG AWB.
 *
 * @since 18.11.2018
 */
public class EkgConfigStore extends PropertyConfigStore {

    private static final Logger LOGGER = EkgLogger.get();

    private static final String CONFIG_KEY = "EKG_CONFIG";

    private static final EkgConfigStore instance = new EkgConfigStore();

    private final Map<ModuleType, String> moduleConfigPathMap = new HashMap<>();

    private String configRootPath;


    /**
     * Private constructor for singleton pattern.
     * Constructs a new instance of EkgConfigStore and resolves the correct path
     * for the module configs.
     */
    private EkgConfigStore() {

        configRootPath = System.getProperty(CONFIG_KEY, System.getenv(CONFIG_KEY));

        if (StringUtils.isNotBlank(configRootPath)  && new File(configRootPath).exists()) {
            configRootPath = configRootPath.replaceAll("/$", "");
            moduleConfigPathMap.put(ModuleType.CORE_MODULE, configRootPath + "/core/");
            moduleConfigPathMap.put(ModuleType.EXTENSION_MODULE, configRootPath + "/extension/");
            moduleConfigPathMap.put(ModuleType.IMPORT_MODULE, configRootPath + "/importer/");

        } else  {
            this.configRootPath = null;
        }
    }

    //=================================================================================================================
    // API of the EkgConfigStore
    //=================================================================================================================

    /**
     * Return a singleton instance of the EkgConfigStore that is pre-initialized
     * with the property configurations of the AWB core.
     *
     * @return the singleton instance of EkgConfigStore
     */
    public static EkgConfigStore getInstance() {
        return instance;
    }

    /**
     * Returns the each non-comment line form the requested module config as entry of
     * a list of Strings. Empty lines or comments (lines that start with '#') will skipped.
     * The list has the same order as the lines of the config file.
     *
     * The ConfigStore will search for the right config file at the location specified via EKG_CONF
     * environment property. If it doesn't exists the implementation will look at the classpath of the caller
     * to resolve a default config. For this the default config must located in the 'conf' sub-package of the
     * caller class package.
     *
     * @param moduleType an enum the specifies the module type (CORE/EXTENSION/IMPORTER) the caller wants the config for
     * @param moduleId the unique id of the module or null if no id exists.
     * @param configName the key of the config that is equal to the file name without file-extension
     * @return the module config as list of Strings
     */
    public List<String> getListConfig(ModuleType moduleType, String moduleId, String configName) {

        String configContent;

        if (configRootPath == null) {
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            configContent = readModuleDefaultConfig(caller, moduleId, configName);

            if (configContent == null) {
                return Collections.emptyList();
            }
        } else {
            configContent = readConfigFile(moduleType, moduleId, configName);
        }

        configContent = configContent.replaceAll("\r\n", "\n");
        String[] contentLines = StringUtils.split(configContent, '\n');

        return Arrays.stream(contentLines)
                .filter(line -> StringUtils.isNotBlank(line) && !line.trim().startsWith("#"))
                .collect(Collectors.toList());
    }

    /**
     * Returns the configuration as as map with the config key as map key and the config
     * value as map value.
     *
     * The ConfigStore will search for the right config file at the location specified via EKG_CONF
     * environment property. If it doesn't exists the implementation will look at the classpath of the caller
     * to resolve a default config. For this the default config must located in the 'conf' sub-package of the
     * caller class package.
     *
     * @param moduleType an enum the specifies the module type (CORE/EXTENSION/IMPORTER) the caller wants the config for
     * @param moduleId the unique id of the module or null if no id exists.
     * @param configName the key of the config that is equal to the file name without file-extension
     * @return the module config as map of Strings
     */
    public Map<String, String> getMapConfig(ModuleType moduleType, String moduleId, String configName) {

        String configContent;

        if (configRootPath == null) {
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            configContent = readModuleDefaultConfig(caller, moduleId, configName);

            if (configContent == null) {
                return Map.of();
            }
        } else {
            configContent = readConfigFile(moduleType, moduleId, configName);
        }

        configContent = configContent.replaceAll("\r\n", "\n");
        String[] contentLines = StringUtils.split(configContent, '\n');

        Map<String, String> resultMap = new HashMap<>();

        Arrays.stream(contentLines)
                .filter(line -> StringUtils.isNotBlank(line) && !line.trim().startsWith("#"))
                .forEach(line -> {
                    int delimiterPos = line.indexOf(':');
                    String key = line.substring(0, delimiterPos);
                    String value = line.substring(delimiterPos + 1);
                    resultMap.put(key.trim(), value.trim());
                });

        return resultMap;
    }


    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================

    private String readModuleDefaultConfig(Class<?> caller, String moduleId, String configName) {
        InputStream defaultConfigStream = caller.getResourceAsStream(configName + ".conf");

        if (defaultConfigStream == null) {
            LOGGER.warn("no default config for module {} and config-key={} exists", moduleId, configName);
            return null;
        }

        try {
            return IOUtils.toString(defaultConfigStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read stream of the module default config for key='" + configName
                    + "' and module='"+ moduleId + "'");
        }
    }

    private String readConfigFile(ModuleType moduleType, String moduleId, String configName) {

        File moduleConfigPath;

        if (StringUtils.isBlank(moduleId)) {
            moduleConfigPath = new File(moduleConfigPathMap.get(moduleType).replaceAll("/$", ""));
        } else {
            moduleConfigPath = new File(moduleConfigPathMap.get(moduleType) + moduleId);
        }

        if (!moduleConfigPath.exists()) {
            LOGGER.error("No config path exists for the module '{}' at {}", moduleId, moduleConfigPath.getAbsolutePath());
            throw new IllegalStateException("No config root exists for module " + moduleId);
        }

        File configFile = new File(moduleConfigPath, configName + ".conf");

        if (!configFile.exists()) {
            LOGGER.error("Requested config '{}' for importer '{}' doesn't exists!", configFile.getAbsolutePath(), moduleId);
            throw new IllegalStateException("Requested config '" + configFile.getAbsolutePath() + "' doesn't exists!");
        }

        String output;
        try {
            output = FileUtils.readFileToString(configFile, "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException("No config root exists for module " + moduleId);
        }
        return output;
    }
}
