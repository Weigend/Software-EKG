package org.jolokia.backend;

import org.jolokia.config.Configuration;
import org.jolokia.util.LogHandler;

/**
 * @since 02.03.13
 */
public interface IConfigAware {

    void setConfig(Configuration pConfig);

    void setServerHandler(LogHandler pLogHandler);
}
