//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2024
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.common.ui.validation;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Avaje factory for Jakarta Bean Validation Validator.
 * Produces a singleton Validator instance for injection.
 *
 */
@Factory
public class ValidatorProducer {

    private static final ValidatorFactory VALIDATOR_FACTORY = 
            Validation.buildDefaultValidatorFactory();

    /**
     * Produces a Validator instance for injection.
     *
     * @return the Validator instance
     */
    @Bean
    public Validator createValidator() {
        return VALIDATOR_FACTORY.getValidator();
    }
}
