package io.softwareecg.awb.importer.adapter.prometheus.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A token is sent with the request, and is written nowhere else.
 */
public class OpenIdConnectClientCredentialFlowTest {

    private static final String TOKEN = "a-token-nobody-else-may-see";

    private final ListAppender<ILoggingEvent> log = new ListAppender<>();

    private HttpServer server;

    @Before
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            byte[] body = ("{\"access_token\": \"" + TOKEN + "\", \"expires_in\": 60}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        log.start();
        rootLogger().addAppender(log);
    }

    @After
    public void stop() {
        rootLogger().detachAppender(log);
        server.stop(0);
    }

    @Test
    public void theTokenIsSentAndNotLogged() {
        OpenIdConnectClientCredentialFlow flow = new OpenIdConnectClientCredentialFlow(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/token", "client", "secret-of-the-client");
        HttpGet request = new HttpGet("http://127.0.0.1/");

        flow.authenticate();
        flow.decorate(request);

        assertEquals("Bearer " + TOKEN, request.getFirstHeader("Authorization").getValue());
        List<String> messages = log.list.stream()
                .filter(event -> event.getLoggerName().contains("OpenIdConnect"))
                .map(ILoggingEvent::getFormattedMessage).toList();
        assertTrue(messages.toString(), messages.stream().anyMatch(message -> message.contains("valid until")));
        assertFalse(messages.toString(), messages.stream().anyMatch(
                message -> message.contains(TOKEN) || message.contains("secret-of-the-client")));
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }
}
