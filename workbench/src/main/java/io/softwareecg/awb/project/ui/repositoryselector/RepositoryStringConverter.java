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
package io.softwareecg.awb.project.ui.repositoryselector;

import io.softwareecg.awb.common.ui.converter.UniversalStringConverter;
import io.softwareecg.awb.repository.api.EkgRepository;
import org.apache.commons.lang3.NotImplementedException;

/**
 * JavaFX object converter that converts {@link EkgRepository} objects into strings. Reverse convert is not supported by
 * types.
 *
 */
public class RepositoryStringConverter extends UniversalStringConverter<EkgRepository> {

    @Override
    public String toString(EkgRepository object) {

        if (object == null) {
            return "";
        }

        return object.getRepositoryName() + " | Typ: " + object.getEkgRepositoryDbType().getName();
    }

    @Override
    public EkgRepository fromString(String string) {
        throw new NotImplementedException("May be implemented later");
    }
}
