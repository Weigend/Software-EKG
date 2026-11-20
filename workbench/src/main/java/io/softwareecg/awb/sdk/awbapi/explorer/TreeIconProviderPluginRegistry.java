package io.softwareecg.awb.sdk.awbapi.explorer;

/**
 * This interface represents a global registry for TreeIconProvider's.
 * This kind of service will used by importers to register custom icon providers
 * for it's specific domain.
 * On the other side the EKG workbench will query this registry to retrieve
 * the right provider for a specific set of imported data.
 *
 * The interface only provides the API that required by the importers.
 *
 * @since 04.02.2020
 */
public interface TreeIconProviderPluginRegistry {

    /**
     * Register the given provider instance to the global registry
     * with the name (unique id) of the importer as lookup key.
     *
     * If this method is called multiple times with similar importModule name, the implementation ensures
     * that only the first call will have any affect
     *
     * @param importModule the name (unique id) of the import module the provider belongs to
     * @param provider an instance of ImporterTreeIconProvider that provides domain specific icons for the imported data
     */
    void registerImportTreeIconProvider(String importModule, ImporterTreeIconProvider provider);
}
