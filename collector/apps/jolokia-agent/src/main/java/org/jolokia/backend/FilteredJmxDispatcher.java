package org.jolokia.backend;

import org.jolokia.backend.executor.NotChangedException;
import org.jolokia.config.ConfigKey;
import org.jolokia.config.Configuration;
import org.jolokia.converter.Converters;
import org.jolokia.detector.ServerHandle;
import org.jolokia.handler.JsonRequestHandler;
import org.jolokia.handler.RequestHandlerManager;
import org.jolokia.request.JmxRequest;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.LogHandler;
import org.jolokia.util.RequestType;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.io.IOException;

/**
 * @since 02.03.13
 */
public class FilteredJmxDispatcher implements RequestDispatcher, IConfigAware {

    public static final String EKG_MODE_FLAG = "mode=ekg";

    // Handler for finding and merging the various MBeanHandler
    protected MBeanServerHandler mBeanServerHandler;

    protected RequestHandlerManager requestHandlerManager;

    // An (optional) qualifier for registering MBeans.
    protected String qualifier;

    // Logger
    protected LogHandler log;

    protected Configuration config;

    protected Converters converters;

    protected ServerHandle serverHandle;

    protected Restrictor restrictor;

    protected FilteredListJsonRequestHandler filteredListJsonRequestHandler;

    protected FilteredReadJsonRequestHandler filteredReadJsonRequestHandler;

    public FilteredJmxDispatcher(Converters pConverters, ServerHandle pServerHandle,
                                              Restrictor pRestrictor) {
        restrictor = pRestrictor;
        converters = pConverters;
        serverHandle = pServerHandle;
    }

    public Object dispatchRequest(JmxRequest pJmxReq) throws InstanceNotFoundException, AttributeNotFoundException,
            ReflectionException, MBeanException, IOException, NotChangedException {

        if (mBeanServerHandler == null) {
            throw new IllegalStateException("MBeansServerHandler is null. This class wasn't initialized correctly!");
        }

        JsonRequestHandler handler = requestHandlerManager.getRequestHandler(pJmxReq.getType());

        if (pJmxReq.getType() == RequestType.LIST && pJmxReq.getPath() != null && pJmxReq.getPath().contains(EKG_MODE_FLAG)) {
            pJmxReq.getPathParts().remove(EKG_MODE_FLAG);
            return mBeanServerHandler.dispatchRequest(filteredListJsonRequestHandler, pJmxReq);
        } else if (pJmxReq.getType() == RequestType.READ && pJmxReq.getPath() != null && pJmxReq.getPath().contains(EKG_MODE_FLAG)) {
            pJmxReq.getPathParts().remove(EKG_MODE_FLAG);
            return mBeanServerHandler.dispatchRequest(filteredReadJsonRequestHandler, pJmxReq);
        } else {
            return mBeanServerHandler.dispatchRequest(handler, pJmxReq);
        }
    }

    public boolean canHandle(JmxRequest pJmxRequest) {
         return true;
    }

    public boolean useReturnValueWithPath(JmxRequest pJmxRequest) {
        JsonRequestHandler handler = requestHandlerManager.getRequestHandler(pJmxRequest.getType());
        return handler.useReturnValueWithPath();
    }

    public void setConfig(Configuration pConfig) {
        config = pConfig;
        initIfPossible();
    }

    public void setServerHandler(LogHandler pLogHandler) {
        log = pLogHandler;
        initIfPossible();
    }

    private void initIfPossible() {
        if (log != null && config != null) {
            // Get all MBean servers we can find. This is done by a dedicated
            // handler object
            mBeanServerHandler = new MBeanServerHandler(config, log);
            qualifier = config.get(ConfigKey.MBEAN_QUALIFIER);

            // Request handling manager
            requestHandlerManager = new RequestHandlerManager(converters, mBeanServerHandler.getServerHandle(), restrictor);
            filteredListJsonRequestHandler = new FilteredListJsonRequestHandler(mBeanServerHandler, log, restrictor);
            filteredReadJsonRequestHandler = new FilteredReadJsonRequestHandler(mBeanServerHandler, log, restrictor);
        }
    }
}
