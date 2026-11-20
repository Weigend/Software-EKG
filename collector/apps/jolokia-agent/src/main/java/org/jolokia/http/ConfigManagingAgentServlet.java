package org.jolokia.http;

import org.jolokia.backend.BackendManager;
import org.jolokia.backend.IConfigAware;
import org.jolokia.backend.RequestDispatcher;
import org.jolokia.config.Configuration;
import org.jolokia.util.LogHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @since 02.03.13
 */
public class ConfigManagingAgentServlet extends AgentServlet {

    @Override
    public void init(ServletConfig pServletConfig) throws ServletException {
        super.init(pServletConfig);

        LogHandler logHandler = this.getLogHandler();
        Configuration config = initConfig(pServletConfig);

        List<RequestDispatcher> requestDispatchers = getRequestDispatchers();
        for (RequestDispatcher dispatcher : requestDispatchers) {
            if (dispatcher instanceof IConfigAware) {
                ((IConfigAware) dispatcher).setConfig(config);
                ((IConfigAware) dispatcher).setServerHandler(logHandler);
            }
        }
    }

    /**
     * This method extract a private member from the parent class by using reflection.
     * This is a bad pattern, but on the other side the only other option will be to reimplement
     * some Jolokia Classes to enable this Servlet to configure the RequestDispatcher.
     * In this case we get much more dependencies to implementation-details than by using a
     * two known member.
     *
     * @return return the list of RequestDispatcher
     */
    @SuppressWarnings("unchecked")
    private List<RequestDispatcher> getRequestDispatchers() {
        Object backendManager = getFieldValue("backendManager", AgentServlet.class, this);
        return (List<RequestDispatcher>) getFieldValue("requestDispatchers", BackendManager.class, backendManager);
    }

    private Object getFieldValue (String fieldName, Class classWithFields, Object object) {
        try {
            Field field = classWithFields.getDeclaredField(fieldName);
            field.setAccessible(true);
            return  field.get(object);

        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
