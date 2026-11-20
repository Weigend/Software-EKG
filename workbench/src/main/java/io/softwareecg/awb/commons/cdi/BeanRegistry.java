package io.softwareecg.awb.commons.cdi;


/**
 * todo enter comment here
 *
 * @since 02.06.2018
 */
public class BeanRegistry {

    private static final BeanRegistry registry = new BeanRegistry();

    public static BeanRegistry getInstance() {
        return registry;
    }

}
