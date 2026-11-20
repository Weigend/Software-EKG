package org.jolokia.backend;

import org.jolokia.request.JmxRequest;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.LogHandler;
import org.jolokia.util.RequestType;

import javax.management.*;
import javax.management.openmbean.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * An optimized implementation of the Jolokia ListHandler, that creates only the
 * data that Software EKG can handle and understand the wildcard expression from
 * the standard Performance-Counter configuration
 *
 * To use the FilteredListJsonRequestHandler you have to send a "mode=ekg" flag and have
 * to specify the requested beans by a simple Wildcard expression or absolute MBean name like
 * MXBean(java.lang:type=Memory).HeapMemoryUsage.used
 *  *
 * Call with GET-Request:
 * http://localhost:8080/jolokia/list/mode=ekg/*Heap*
 *
 * Call with POST-Request (a Json Argument have to send with POST):
 * [{"path":"mode=ekg\/*Heap*","type":"LIST"},{"path":"mode=ekg\/*Cpu*","type":"LIST"}]
 *
 * @since 03.03.13
 */
public class FilteredListJsonRequestHandler extends AbstractFilteredJsonRequestHandler {

    protected FilteredListJsonRequestHandler(MBeanServerHandler pMBeanServerHandler,
                                             LogHandler pLogger, Restrictor pRestrictor) {
        super(pMBeanServerHandler, pLogger, pRestrictor);
    }

    @Override
    public RequestType getType() {
        return RequestType.LIST;
    }

    @Override
    protected Object doHandleRequest(MBeanServerConnection connection, JmxRequest request)
            throws InstanceNotFoundException, AttributeNotFoundException,
            ReflectionException, MBeanException, IOException {

        return expandCounter(connection, request.getPath());
    }


    private Collection<String> expandCounter(MBeanServerConnection con, String originalCounterName) throws IOException {

        Set<String> expandedCounter = new TreeSet<String>();

        if (originalCounterName.contains("*")) {
            // wildcards like w32 *abc* -> .*abc.*
            originalCounterName = originalCounterName.replace("*", ".*");
        }

        for (Object nameInstance : con.queryNames(null, null)) {

            if (!(nameInstance instanceof ObjectName name)) {
                throw new IllegalStateException("queried MBean name isn't an instance of ObjectName");
            }

            try {
                String prefix = "MXBean(" + name.getCanonicalName() + ").";

                // fetch details of the MBean, scan it's attributes and filter unwanted beans
                MBeanAttributeInfo[] mbAttrInfo = con.getMBeanInfo(name).getAttributes();
                for (MBeanAttributeInfo mBeanAttributeInfo : mbAttrInfo) {

                    // skip beans we can't read
                    if (!mBeanAttributeInfo.isReadable()) {
                        continue;
                    }

                    // skip beans that are not matching with the given counter-name
                    String newCounter = prefix + mBeanAttributeInfo.getName();
                    if (!isMatchingCounter(originalCounterName, newCounter)) {
                        continue;
                    }

                    // primitives are wanted
                    if (isNumeric(mBeanAttributeInfo)) {
                        expandedCounter.add(newCounter);

                    // OpenMBeans are used for CompositeDataTypes in JDK 6  or newer and JDK5 (if not in Weblogic)
                    } else if (mBeanAttributeInfo instanceof OpenMBeanAttributeInfo) {

                        OpenMBeanAttributeInfoSupport attrInfo = (OpenMBeanAttributeInfoSupport) mBeanAttributeInfo;
                        OpenType type = attrInfo.getOpenType();

                        if (type != null && isCompatible(type)) {
                            // simple type
                            expandedCounter.add(newCounter);
                        } else {
                            if (mBeanAttributeInfo.getType() == null) {
                                continue;
                            }
                            extractDataFromFeatureInfo(originalCounterName, expandedCounter, prefix, mBeanAttributeInfo);
                        }

                    // special treatment for Weblogic + JDK5 (only works for "java.lang" JMX-Domain
                    } else if (isCompositeCounter(mBeanAttributeInfo)) {

                        Object attributes = con.getAttribute(name, mBeanAttributeInfo.getName());

                        if (attributes instanceof CompositeDataSupport) {
                            CompositeType data = ((CompositeDataSupport) attributes).getCompositeType();

                            for (Object key : data.keySet()) {
                                Object value = ((CompositeDataSupport) attributes).get(key.toString());
                                if (isCompatible(value.getClass().getCanonicalName())) {
                                    expandedCounter.add(newCounter + "." + key);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getClass().getName() + " | " + e.getMessage(), e);
            }
        }

        return expandedCounter;
    }

    private void extractDataFromFeatureInfo(String originalCounterName, Set<String> expandedCounter,
                                            String prefix, MBeanAttributeInfo mBeanAttributeInfo)
                                            throws NoSuchFieldException, IllegalAccessException {

        // composite type
        Field descriptorField = MBeanFeatureInfo.class.getDeclaredField("descriptor");
        descriptorField.setAccessible(true);
        Descriptor descriptor = (Descriptor)descriptorField.get(mBeanAttributeInfo);
        extractDataFromDescriptor(originalCounterName, expandedCounter, prefix + mBeanAttributeInfo.getName(), descriptor);
    }

    private void extractDataFromDescriptor(String originalCounterName, Set<String> expandedCounter,
                                           String counterName, Descriptor descriptor) {

        String[] fieldNames = descriptor.getFieldNames();
        for (String fieldName : fieldNames) {
            Object fieldValue = descriptor.getFieldValue(fieldName);

            if (fieldValue instanceof CompositeType) {
                extractCompositeData(expandedCounter, originalCounterName, counterName, fieldValue);

            }
        }
    }

    private boolean isCompositeCounter(MBeanAttributeInfo mBeanAttributeInfo) {
        return (mBeanAttributeInfo.getType().equals(CompositeData.class.getName()));
    }

    private void extractCompositeData(Set<String> expandedCounter, String originalCounterName,
                                      String beanName, Object field) {

        CompositeType compositeType = (CompositeType) field;

        for (Object o : compositeType.keySet()) {
            OpenType t2 = compositeType.getType((String) o);
            if (isCompatible(t2)) {
                String newCounterString = beanName + "." + o;
                if (isMatchingCounter(originalCounterName, newCounterString)) {
                    expandedCounter.add(newCounterString);
                }
            }
        }
    }

    private boolean isMatchingCounter(String originalCounterName, String newCounterString) {
        return newCounterString.equals(originalCounterName) || newCounterString.matches(originalCounterName);
    }


    /**
     * Checks if a open type is numeric.
     *
     * @param type the type.
     * @return a numeric type.
     */
    private boolean isCompatible(OpenType type) {
        return isCompatible(type.getTypeName());
    }

    /**
     * Checks if a open type is numeric.
     *
     * @param typeName the type as string
     * @return a numeric type.
     */
    private boolean isCompatible(String typeName) {
        return typeName != null && (typeName.equals("java.lang.Long") ||
               typeName.equals("java.lang.Integer") ||
               typeName.equals("java.lang.Float") ||
               typeName.equals("java.lang.Double") ||
               typeName.equals("java.lang.Boolean"));
    }

    /**
     * Helper to check if we can use the attribute as EKG rlog (numeric only).
     *
     * @param mBeanAttributeInfo the bean info.
     * @return true if it is readable and numeric.
     */
    private boolean isNumeric(MBeanAttributeInfo mBeanAttributeInfo) {
        String type = mBeanAttributeInfo.getType();
        return isCompatible(type) || type != null &&
                (type.equals("long") ||
                 type.equals("double") ||
                 type.equals("float") ||
                 type.equals("int") ||
                 type.equals("boolean"));
    }
}
