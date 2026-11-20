package org.jolokia.backend;

import org.jolokia.request.JmxReadRequest;
import org.jolokia.request.JmxRequest;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.LogHandler;
import org.jolokia.util.RequestType;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.InvalidKeyException;
import java.io.IOException;

/**
 *
 * @since 03.03.13
 */
public class FilteredReadJsonRequestHandler extends AbstractFilteredJsonRequestHandler {

    protected FilteredReadJsonRequestHandler(MBeanServerHandler pMBeanServerHandler,
                                             LogHandler pLogger, Restrictor pRestrictor) {
        super(pMBeanServerHandler, pLogger, pRestrictor);
    }

    @Override
    public RequestType getType() {
        return RequestType.READ;
    }

    @Override
    protected Object doHandleRequest(MBeanServerConnection connection, JmxRequest request) throws InstanceNotFoundException,
            AttributeNotFoundException, ReflectionException, MBeanException, IOException {

        // we have to add an empty value here, because the next stages fetch this (without important using) but
        // thrown an exception if not exists
        ((JmxReadRequest)request).getAttributeNames().add("");

        String counter = ((JmxReadRequest) request).getObjectNameAsString();

        if (counter.startsWith("MXBean")) {
            String mbeanName = counter.substring(counter.indexOf("(") + 1, counter.lastIndexOf(")"));
            String mbeanAttribute = counter.substring(counter.lastIndexOf(")") + 2);
            String value;

            try {

                //a composite data is collected (f.e. "Usage.max")
                if (mbeanAttribute.contains(".")) {
                    String data = mbeanAttribute.substring(0, mbeanAttribute.indexOf("."));
                    String key = mbeanAttribute.substring(mbeanAttribute.indexOf(".") + 1);
                    try {
                        CompositeData cd = (CompositeData) connection.getAttribute(new ObjectName(mbeanName), data);
                        if (cd == null) {
                            return -1;
                        }
                        Object object = cd.get(key);
                        if (object == null) {
                            logger.debug("Can not get object for key: " + key);
                            return -1;
                        }
                        value = object.toString();
                    } catch (ClassCastException e) {
                        throw new IllegalStateException("No composite jmx counter: " + counter);
                    } catch (AttributeNotFoundException e) {
                        throw new IllegalStateException("No such attribute found: '" + data + "' in " + counter);
                    } catch (InvalidKeyException e) {
                        throw new IllegalStateException("No such key found: '" + key + "' in " + counter);
                    }
                } else {
                        Object v = connection.getAttribute(new ObjectName(mbeanName), mbeanAttribute);
                        value = v != null ? v.toString() : "-1";
                }

                // convert boolean to numeric
                if (value.equals("true")) {
                    value = "1";
                }
                if (value.equals("false")) {
                    value = "0";
                }

                return Double.parseDouble(value);

            } catch (UnsupportedOperationException e) {
                //logger.debug(e.getMessage()); // not all properties has to be supported
            } catch (IOException e) {
                logger.debug(e.getMessage());
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            } catch (RuntimeMBeanException e) {
                // nothing to do here
            }
            return -1;
        } else {
            throw new IllegalStateException("Only MXBeans are supported.");
        }
    }
}
