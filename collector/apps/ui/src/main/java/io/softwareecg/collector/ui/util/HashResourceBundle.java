//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Weigend AM
//       creation date: 2009 - 2013
//       changed by:    $Author$
//       change date:   $Date$
//       revision:      $Revision$
//       description:   Collector UI
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.ui.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * ResourceBundle based on a HasMap
 *
 */
public class HashResourceBundle extends ResourceBundle {

    /**
     * Value of this key should contain a subclass of FileSystemAssistant.
     * It is used to pass a FileSystemAssistant around.
     */
    public static final String ASSISTANT_KEY = "assistant";

    /**
     * Value of this key should contain the target, that the backButton on the next screen should point to
     */
    public static final String SOURCE_KEY = "backButtonTargetView";

    /**
     * In order to make the back button work, some screens need to be handed back their ResourceBundle, as important informatin is specified there.
     * This key should contain the resourceBundle of the class, that the SOURCE_KEY is targeting.
     */
    public static final String SOURCE_RESOURCE_BUNDLE = "sourceRes";

    private final HashMap<String, Object> map;
    private final ArrayList<String> keys;

    /**
     * Creates a new HashResourceBundle instance.
     */
    public HashResourceBundle() {
        map = new HashMap<>();
        keys = new ArrayList<>();
    }

    /**
     * Adds a key-value-pair to the HasMap
     *
     * @param key   Key of the value
     * @param value Value saved in HashMap
     */
    public void putValue(String key, Object value) {
        map.put(key, value);
        keys.add(key);
    }

    @Override
    protected Object handleGetObject(String key) {
        return map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new Enumeration<String>() {

            final ArrayList<String> keysInternal = (ArrayList<String>) keys.clone();

            @Override
            public boolean hasMoreElements() {
                return keysInternal.size() > 0;
            }

            @Override
            public String nextElement() {
                return keysInternal.remove(0);
            }
        };
    }

}
