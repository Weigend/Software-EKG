package io.softwareecg.awb.sdk.awbapi.explorer;


/**
 * This interface has to implement by all import modules that
 * support/provide custom context menu entries for the AWB explorer.
 * Each module is currently limited to a single context menu entry.
 *
 * @since 13.07.2018
 */
public interface ContextMenuAware {

    /**
     * Returns the importer specific context menu entry
     *
     * @return the context menu entry
     */
    ContextMenuEntry getImportContextMenuItem();
}
