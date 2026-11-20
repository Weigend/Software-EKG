//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * The collector.properties.
 */
public class CollectorProperties extends Properties {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The default directory for log files.
     */
    private static final String LOGDIR = "logdir";
    private static final String RUN_ARG_CONFIG_KEY = "collector.runArg.configPath";
    private static final String COLLECTOR_PROPERTY_PATH = "collectorPropertiesPath";
    private static final String DEFAULT_PROPERTY_NAME = "collector.properties";
    private static final String DEFAULT_CONFIG_DIR = "./config/";


    /**
     * Our instance.
     */
    private static final CollectorProperties properties = new CollectorProperties();

    /**
     * Singleton.
     */
    private CollectorProperties() {
        this(COLLECTOR_PROPERTY_PATH, DEFAULT_PROPERTY_NAME);
    }

    protected CollectorProperties(String customPropertyPath, String defaultPropertyPath) {
        try (InputStream configProperties = resolvePropertyPath(customPropertyPath, defaultPropertyPath)) {
            super.load(configProperties);

        } catch (IOException e) {
            LOGGER.debug("Can not load collector properties from config directory.", e);
        }
    }


    public static InputStream resolvePropertyPath(String envConfigKey, String defaultPropertyFilename) throws IOException {

        String resultFilePath;
        String customPropertyConfig = System.getProperty(envConfigKey);

        if (customPropertyConfig == null) {
            customPropertyConfig = System.getenv(envConfigKey);
        }

        // 1. check if path to property file is explicitly set via user env properties
        if ((resultFilePath = getValidFile(System.getProperty(envConfigKey), defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ('{}') configured via user environment vars.", System.getProperty(envConfigKey));
            return new FileInputStream(resultFilePath);
        }

        LOGGER.debug("No valid configuration file ({}') given view user properties.", defaultPropertyFilename);

        // 2. check if path to property file is explicitly set via system env properties
        if ((resultFilePath = getValidFile(System.getenv(envConfigKey), defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ('{}') configured via system environment vars.", System.getenv(envConfigKey));
            return new FileInputStream(resultFilePath);
        }

        LOGGER.debug("No valid configuration file ({}') given view system env properties.", defaultPropertyFilename);

        // 3. check if property file is a sidecar config that is located side-by-side to main XML config
        String xmlFile = System.getProperty(RUN_ARG_CONFIG_KEY);
        String xmlDir =  xmlFile == null ? null : new File(xmlFile).getParent();
        if ((resultFilePath = getValidFile(xmlDir, defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ({}') that is stored side-by-side to XML config.", defaultPropertyFilename);
            return new FileInputStream(resultFilePath);
        }

        LOGGER.debug("No valid configuration file ({}') is located side by side to XML config.", defaultPropertyFilename);

        // 4. check if property file is located in config dir
        if ((resultFilePath = getValidFile(DEFAULT_CONFIG_DIR, defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ({}') that is stored the default config directory.", defaultPropertyFilename);
            return new FileInputStream(resultFilePath);
        }

        LOGGER.warn("No explicit configured location of config file '{}' exists. Config also not available in default " +
                "config directory. Take a last try at base dir.", defaultPropertyFilename);

        // 5. check if a custom defined property file is located in classpath
        Class caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        InputStream inputStream;
        if (StringUtils.isNotBlank(customPropertyConfig) && (inputStream = caller.getResourceAsStream(customPropertyConfig)) != null) {
            LOGGER.info("Using a custom property file ({}') that is stored in the classpath", customPropertyConfig);
            return inputStream;
        }

        // 6. check if a default property file is located in root of classpath
        if ((inputStream = caller.getResourceAsStream("/" + defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ({}') that is stored in the root of classpath", defaultPropertyFilename);
            return inputStream;
        }

        // 7. last but not least check if property file is located in working directory
        if ((resultFilePath = getValidFile("./", defaultPropertyFilename)) != null) {
            LOGGER.info("Using property file ({}') that is stored the in collector base directory.", defaultPropertyFilename);
            return new FileInputStream(resultFilePath);
        }

        LOGGER.warn("Property file '{}' doesn't exists. Please check the collector setup.", defaultPropertyFilename);

        String msg = "Cant load properties from classpath/filesystem. Missing '" + defaultPropertyFilename + "' file.";
        Console.getErr().println(msg);
        throw new IllegalStateException(msg);
    }


    private static String getValidFile(String path, String defaultPropertyFilename) {
        if (StringUtils.isBlank(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        if (file.isFile() &&  file.canRead()) {
            return path;
        }

        File config = new File(file, defaultPropertyFilename);
        if (config.exists() && config.canRead()) {
            return config.getAbsolutePath();
        }

        return null;
    }


    /**
     * Singleton.
     *
     * @return all props.
     */
    public static CollectorProperties getProperties() {
        return properties;
    }

    /**
     * Pattern for rolling Policy.
     *
     * @return max row count;
     */
    public static String getRollingPolicyPattern() {
        return String.valueOf(properties.getProperty("io.softwareecg.collector.source.rollingPolicyPattern", "%d{yyyy-MM-dd}"));
    }

    /**
     * Pattern for archive Directory.
     *
     * @return max row count;
     */
    public static String getArchiveDirectoryPattern() {
        return String.valueOf(properties.getProperty("io.softwareecg.collector.source.archiveDirectoryPattern", "yyyy-MM-dd-HH"));
    }

    /**
     * Gets the use bulk query.
     *
     * @return the use bulk query
     */
    public static boolean isBulkQuery() {
        return Boolean.parseBoolean(properties.getProperty("io.softwareecg.collector.source.wincount.WindowsPerformanceCounterAccess.UseBulkQuery", "true"));
    }

    /**
     * Gets the use progress sign.
     *
     * @return the use progress sign
     */
    public static boolean isUseProgressSign() {
        return Boolean.parseBoolean(properties.getProperty("io.softwareecg.collector.control.CollectorJob.UseProgressSign", "true"));
    }

    /**
     * The logdir.
     *
     * @return the logdir.
     */
    public static String getLogdir() {
        return properties.getProperty(LOGDIR, "logs");
    }

    /**
     * Sets the logdir.
     *
     * @param logdir the logdir.
     */
    public static void setLogdir(String logdir) {
        properties.setProperty(LOGDIR, logdir);
    }


}
