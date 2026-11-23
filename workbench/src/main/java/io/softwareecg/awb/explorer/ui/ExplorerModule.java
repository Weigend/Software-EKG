//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.explorer.ui;

import io.softwareecg.awb.common.ui.explorer.ExplorerController;
import io.softwareecg.awb.common.ui.explorer.RootItem;
import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import io.softwareecg.awb.common.ui.explorer.api.ItemBuilderRegistry;
import io.softwareecg.awb.common.ui.view.EkgView;
import io.softwareecg.awb.runtime.module.EkgModule;
import io.softwareecg.awb.runtime.module.ModuleException;
import io.softwareecg.awb.explorer.ui.items.CommonItemBuilder;
import io.softwareecg.awb.explorer.ui.items.RepositoryItemsTask;
import io.softwareecg.awb.explorer.ui.items.RepositoryItem;
import io.softwareecg.wfx.lookup.api.Lookup;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * Register the explorerView view.
 *
 */
public class ExplorerModule implements EkgModule {

    protected EkgView<ExplorerController> explorerView;

    protected ExplorerExtendedController explorerExtController;

    //=================================================================================================================
    //   EkgModule API
    //=================================================================================================================

    /**
     * Preloads the explorer tree on startup of EKG workbench
     *
     * @throws ModuleException thrown than the FXML view couldn't load correctly
     */
    @Override
    public void preload() throws ModuleException {
        try {
            // create builder for repository and project tree items and register it
            registerDefaultTreeItemBuilders();

            // init the explorer view including the common controller
            ExplorerController controller = initExplorerView();

            // create a extended tree controller that do the Workbench specific stuff
            explorerExtController = Lookup.lookup(ExplorerExtendedController.class);
            explorerExtController.initWithCommonController(controller);

        } catch (IOException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public void start() {
        // no op
    }

    @Override
    public void stop() {
        // no op
    }

    //=================================================================================================================
    //   private logic that do the initializing stuff
    //=================================================================================================================

    private ExplorerController initExplorerView() throws IOException {
        explorerView = new EkgView.Builder<ExplorerController>()
                .withId("explorerView")
                .withTitle("Explorer")
                .withPos(EkgView.Position.LEFT)
                .withFile(ExplorerController.class.getResource("ExplorerView.fxml"))
                .withViewAreaSize(0.20)
                .build();

        ExplorerController controller = explorerView.getController();
        controller.setRootItem(Lookup.lookup(RootItem.class));
        return controller;
    }


    private void registerDefaultTreeItemBuilders() {
        // retrieve central builder registry
        ItemBuilderRegistry builderRegistry = Lookup.lookup(ItemBuilderRegistry.class);

        // static bind of item builder to different parent items
        // by using the central builder registry
        registerRepositoryItemBuilder(builderRegistry); // builder for repository
        registerProjectItemBuilder(builderRegistry);  // builder for projects
    }



    /**
     * Register an item builder for RepositoryItem instances for RootItem parent
     * at the given ItemBuilderRegistry
     *
     * @param builderRegistry the builder registry that should used to register the builders
     */
    private void registerRepositoryItemBuilder(ItemBuilderRegistry builderRegistry) {

        Function<RootItem, List<? extends AbstractItem>> builder =
                rootItem -> Lookup.lookup(RepositoryItemsTask.class).getRepositoryItems();

        builderRegistry.registerItemBuilder(RootItem.class, builder);
    }

    /**
     * Register an item builder for ProjectItem instances for RepositoryItem parents
     *  at the given ItemBuilderRegistry
     *
     * @param builderRegistry the builder registry that should used to register the builders
     */
    private void registerProjectItemBuilder(ItemBuilderRegistry builderRegistry) {
        builderRegistry.registerItemBuilder(RepositoryItem.class, CommonItemBuilder::buildProjectItems);
    }
}
