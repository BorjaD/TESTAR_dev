package nl.ou.testar.StateModel.Analysis.HttpServer;

import nl.ou.testar.StateModel.Analysis.AnalysisManager;
import nl.ou.testar.StateModel.Analysis.GraphServlet;
import nl.ou.testar.StateModel.Analysis.StateModelServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.fruit.alayer.State;

public class JettyServer {
    private Server server;

    public void start(String resourceBase, AnalysisManager analysisManager) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8090);
        server.setConnectors(new Connector[]{connector});

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
//        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resourceHandler.setResourceBase(resourceBase);

//        ServletHandler servletHandler = new ServletHandler();
//        servletHandler.addServletWithMapping(StateModelServlet.class, "/status");
//        ServletContextHandler contextHandler = new ServletContextHandler();
//        contextHandler.setContextPath("/");
//        contextHandler.setResourceBase(resourceBase);
//        contextHandler.addServlet(StateModelServlet.class, "/status");


        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setResourceBase(resourceBase);
        webAppContext.addServlet(new ServletHolder(new StateModelServlet()), "/models");
        webAppContext.addServlet(new ServletHolder(new GraphServlet()), "/graph");
        webAppContext.setAttribute("analysisManager", analysisManager);

        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault( server );
        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration" );
        webAppContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$" );

        HandlerList handlerList = new HandlerList();

        handlerList.addHandler(resourceHandler);
//        handlerList.addHandler(contextHandler);
        handlerList.addHandler(webAppContext);
        handlerList.addHandler(new DefaultHandler());
        server.setHandler(handlerList);
        server.start();
    }
}