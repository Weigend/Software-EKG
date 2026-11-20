package io.softwareecg.awb.sdk.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Simple configuration bean that stores various configs in property files that are
 * created automatically at runtime.
 *
 * @since 31.05.2018
 */
public class PropertyConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyConfigStore.class);

    private final Properties ekgAwbConfigProperties;

    private final File propertyFile;

    public PropertyConfigStore() {
        this(System.getProperty("java.io.tmpdir") + "/awb-config.properties");
    }

    public PropertyConfigStore(String propertyFilePath) {

        propertyFile = new File(propertyFilePath);

        try {

            if (propertyFile.exists()) {

                if (propertyFile.isDirectory()) {
                    String errorMsg = "the specified path to property file '" + propertyFilePath
                            + "' is an directory. Path to a *.property file is expected";
                    LOGGER.error(errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }

                if (!propertyFile.canWrite()) {
                    String errorMsg = "the specified property file " + propertyFile + " is not writable!";
                    LOGGER.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

            }
            else {

                File dirWithPropertyFile = propertyFile.getParentFile();

                if (!dirWithPropertyFile.canWrite()) {
                    String message = "Unable to create the property file because the specified directory hasn't RW access.";
                    LOGGER.error(message);
                    throw new IllegalStateException(message);
                }


                if (!propertyFile.createNewFile() && !propertyFile.exists()) {
                    throw new IOException("unable to create new property configuration at path " + propertyFilePath);
                }
            }


            ekgAwbConfigProperties = new Properties();
            ekgAwbConfigProperties.load(new FileInputStream(propertyFile));

        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns
     * {@code null} if the property is not found.
     *
     * @param   key   the config value key.
     * @return  the value in this property list with the specified key value.
     * @see     #setConfigValue
     */
    public String getConfigValue(String key) {
        return ekgAwbConfigProperties.getProperty(key);
    }

    /**
     * Writes the config value to the config file under the specified
     * key and immediately flush it to the file system.
     *
     * @param key the key of the config value
     * @param value the config value itself
     */
    public void setConfigValue(String key, String value) {
        ekgAwbConfigProperties.setProperty(key, value);

        try (FileWriter writer = new FileWriter(propertyFile)) {
            ekgAwbConfigProperties.store(writer, "last update: ");

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
