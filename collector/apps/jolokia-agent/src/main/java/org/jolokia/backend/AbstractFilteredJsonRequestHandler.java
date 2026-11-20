package org.jolokia.backend;

import org.jolokia.backend.executor.MBeanServerExecutor;
import org.jolokia.backend.executor.NotChangedException;
import org.jolokia.handler.JsonRequestHandler;
import org.jolokia.request.JmxReadRequest;
import org.jolokia.request.JmxRequest;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.LogHandler;
import org.jolokia.util.RequestType;

import javax.management.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * @since 03.03.13
 */
public abstract class AbstractFilteredJsonRequestHandler<R extends JmxRequest> extends JsonRequestHandler<R> {

    protected MBeanServerHandler mBeanServerHandler;

    protected LogHandler logger;

    protected Restrictor jmxRestrictor;

    protected AbstractFilteredJsonRequestHandler(MBeanServerHandler pMBeanServerHandler,
                                             LogHandler pLogger, Restrictor pRestrictor) {
        super(pRestrictor);
        mBeanServerHandler = pMBeanServerHandler;
        logger = pLogger;
        jmxRestrictor = pRestrictor;
    }

    @Override
    public boolean handleAllServersAtOnce(R pRequest) {
        return true;
    }

    @Override
    protected void checkForRestriction(JmxRequest pRequest) {
        if (!jmxRestrictor.isTypeAllowed(getType())) {
            throw new SecurityException("Command type " +
                    getType() + " not allowed due to policy used");
        }
    }

    @SuppressWarnings("unchecked")
    public Object doHandleRequest(MBeanServerExecutor serverManager, R request)
            throws InstanceNotFoundException, AttributeNotFoundException, NotChangedException, ReflectionException, MBeanException, IOException {

        Set<MBeanServerConnection> connections = mBeanServerHandler.getMBeanServerManager().getMBeanServers();
        connections.remove(mBeanServerHandler.getMBeanServerManager().getJolokiaMBeanServer());

        if (connections.isEmpty()) {
            throw new IllegalStateException("unable to proceed the JmxRequest because no JMX-Connection is available!");
        }

        AttributeNotFoundException attrException = null;
        InstanceNotFoundException objNotFoundException = null;

        if (request.getType() == RequestType.LIST) {

            Collection<String> result = new TreeSet<String>();

            for (MBeanServerConnection connection : connections) {
                try {
                    result.addAll((Collection<String>) doHandleRequest(connection, request));
                } catch (InstanceNotFoundException exp) {
                    // Remember exceptions for later use
                    objNotFoundException = exp;
                } catch (AttributeNotFoundException exp) {
                    attrException = exp;
                } catch (IOException exp) {
                    throw new IllegalStateException("I/O Error while dispatching",exp);
                }
            }

            if (objNotFoundException == null && attrException == null) {
                return result;
            }

        } else if (request.getType() == RequestType.READ){
            String requestedDomain = ((JmxReadRequest) request).getObjectNameAsString();
            requestedDomain = requestedDomain.substring(requestedDomain.indexOf("(") + 1, requestedDomain.indexOf(":"));
            for (MBeanServerConnection connection : connections) {
                for (String providedDomain : connection.getDomains()) {
                    if (requestedDomain.equals(providedDomain)) {
                        return doHandleRequest(connection, request);
                    }
                }
            }

            objNotFoundException = new InstanceNotFoundException("The requested counter '" +
                    ((JmxReadRequest) request).getObjectNameAsString() + "' could not found");
        }

        if (attrException != null) {
            throw attrException;
        }

        throw objNotFoundException;
    }
}


