package io.softwareecg.awb.importer.spi;

import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;

/**
 * Interface that represents all import modules that use data from local storage's.
 * This effectively means all kind of file based importers.
 * The other types are query (service) based importers that have implement
 * the {@link SplitSourceProjectSupport}.
 *
 * @since 14.07.2018
 */
public interface LocalProjectSupport {

    /**
     * Returns an instance of LocalProjectImportStrategy that orchestrate the
     * specific import steps required by to read, transform and write the
     * data the importer responsible for.
     *
     * @return an instance of LocalProjectImportStrategy the specific import steps.
     */
    LocalProjectImportStrategy getLocalProjectImportStrategy();
}
