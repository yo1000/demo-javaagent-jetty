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

public class DemoAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        // Create a basic jetty server object that will listen on port 8080.
        Server server = createServer(8888);

        // Start things up!
        server.start();

        // The use of server.join() the will make the current thread join and
        // wait until the server thread is done executing.
        server.join();
    }

    public static Server createServer(int port) {
        return new Server(port) {
            {
                setHandler(new ServletHandler() {
                    {
                        addServletWithMapping(IndexServlet.class, "/*");
                    }
                });
            }
        };
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
            writer.println("<li>max: " + max + "</li>");
            writer.println("<li>committed: " + committed + "</li>");
            writer.println("<li>used: " + used + "</li>");
            writer.println("<li>available: " + available + "</li>");
            writer.println("</ul>");
        }
    }
}
