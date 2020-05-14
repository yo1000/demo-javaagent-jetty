package com.yo1000.demo.javaagent;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class DemoAgent {
    private static final String ARGS_ENTRY_SEPARATOR = ",";
    private static final String ARGS_ENTRY_ITEM_SEPARATOR = "=";
    private static final String ARGS_ENTRY_ITEM_FORMAT = "[^=]+=[^=]+";
    private static final int ARGS_ENTRY_ITEM_KEY_INDEX = 0;
    private static final int ARGS_ENTRY_ITEM_VALUE_INDEX = 1;

    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        Map<String, String> argsMap = parseAgentArguments(agentArgs);

        String hostname = argsMap.getOrDefault("hostname", null);
        int port = Integer.parseInt(argsMap.getOrDefault("port", "8888"));

        // Create a basic jetty server object.
        Server server = createServer(hostname, port);

        // Start things up!
        server.start();

        // The use of server.join() the will make the current thread join and
        // wait until the server thread is done executing.
        server.join();
    }

    private static Map<String, String> parseAgentArguments(String agentArgs) {
        if (agentArgs == null || agentArgs.trim().isEmpty()) return Collections.emptyMap();

        String normalizedArgs = agentArgs.replaceAll("\\s*", "");
        return Arrays.stream(normalizedArgs.split(ARGS_ENTRY_SEPARATOR))
                .filter(s -> s.matches(ARGS_ENTRY_ITEM_FORMAT))
                .map(s -> s.split(ARGS_ENTRY_ITEM_SEPARATOR))
                .collect(Collectors.toMap(
                        keyValue -> keyValue[ARGS_ENTRY_ITEM_KEY_INDEX],
                        keyValue -> keyValue[ARGS_ENTRY_ITEM_VALUE_INDEX])
                );
    }

    public static Server createServer(String hostname, int port) {
        Server server = hostname != null && !hostname.equals("0.0.0.0")
                ? new Server(new InetSocketAddress(hostname, port))
                : new Server(port);

        server.setHandler(new ServletHandler() {{
            addServletWithMapping(IndexServlet.class, "/*");
        }});

        return server;
    }

    public static class IndexServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
            MemoryUsage memoryUsage = mbean.getHeapMemoryUsage();

            long max = memoryUsage.getMax();
            long committed = memoryUsage.getCommitted();
            long used = memoryUsage.getUsed();
            long available = (max > 0L ? max : committed) - used;

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            response.setCharacterEncoding("utf-8");

            PrintWriter writer = response.getWriter();
            writer.println("<ul>");
            writer.printf("<li>max: <code>%,d</code></li>\n", + max);
            writer.printf("<li>committed: <code>%,d</code></li>\n", committed);
            writer.printf("<li>used: <code>%,d</code></li>\n", used);
            writer.printf("<li>available: <code>%,d</code></li>\n", available);
            writer.println("</ul>");
        }
    }
}
