//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.investigation.api;

import io.softwareecg.awb.investigation.model.QuerySpec;

import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.uitoolkit.events.WindowOpenEvent;
import io.softwareecg.awb.repository.api.EkgRepository;

import java.util.EventObject;

/**
 * This event type is intended for ui components which change the query context.
 *
 * @since 12.01.2017
 */
public class OpenInvestigationCommand extends EventObject implements WindowOpenEvent {

    private final QuerySpec filterParams;

    private final EkgRepository repository;

    private final OpeningMode openingMode;

    //=================================================================================================================
    // multiple constructors to initial the context with exact the query data need for the further steps
    //=================================================================================================================

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param filterParams a container with all filter parameters that should used to define the query
     * @param repository the {@link EkgRepository} instance that should used to fetch the data
     * @throws IllegalArgumentException if source is null.
     */
    public OpenInvestigationCommand(Object source, QuerySpec filterParams, EkgRepository repository) {
        this(source, filterParams, repository, OpeningMode.CLEAR_VIEW);
    }

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param filterParams a container with all filter parameters that should used to define the query
     * @param repository the {@link EkgRepository} instance that should used to fetch the data
     * @param openingMode an enum that control which view should use to display the data and how to deal with existing data
     *
     * @throws IllegalArgumentException if source is null.
     */
    public OpenInvestigationCommand(Object source, QuerySpec filterParams, EkgRepository repository, OpeningMode openingMode) {
        super(source);
        this.filterParams = filterParams;
        this.repository = repository;
        this.openingMode = openingMode;
     }


    //=================================================================================================================
    // accessors API of the OpenInvestigationCommand class
    //=================================================================================================================

    /**
     * Getter for property filterParams.
     *
     * @return Value for property filterParams.
     */
    public QuerySpec getFilterParams() {
        return filterParams;
    }

    /**
     * Get the types.
     *
     * @return the types.
     */
    public EkgRepository getRepository() {
        return repository;
    }

    @Override
    public boolean enforceNewView() {
        return openingMode == OpeningMode.NEW_VIEW;
    }

    @Override
    public OpeningMode getOpeningMode() {
        return openingMode;
    }
}
