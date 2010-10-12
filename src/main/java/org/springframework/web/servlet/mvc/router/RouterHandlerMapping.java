package org.springframework.web.servlet.mvc.router;

import org.springframework.web.servlet.mvc.router.exceptions.RouteFileParsingException;
import org.springframework.web.servlet.mvc.router.exceptions.NoRouteFoundException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerMapping}
 * interface that maps handlers based on HTTP routes defined in a route
 * configuration file.
 * 
 * <p>
 * RouterHandlerMapping is not the default HandlerMapping registered in
 * {@link org.springframework.web.servlet.DispatcherServlet} in SpringMVC. You
 * need to declare and configure it in your DispatcherServlet context, by adding
 * a RouterHandlerMapping bean explicitly. RouterHandlerMapping needs the name
 * of the route configuration file (available in the application classpath); it
 * also allows for registering custom interceptors:
 * 
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.mvc.router.RouterHandlerMapping"&gt;
 *   &lt;property name="routeFile" value="routes.conf" /&gt;
 *   &lt;property name="servletPrefix" value="/myservlet" /&gt;
 *   &lt;property name="interceptors" &gt;
 *     ...
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * <p>
 * Annotated controllers should be marked with the {@link Controller} stereotype
 * at the type level. This is not strictly necessary because the methodeInvoker
 * will try to map the Controller.invoker anyway using the current
 * ApplicationContext. The {@link RequestMapping} is not taken into account
 * here.
 * 
 * <p>
 * RouterHandlerMapping loads routes configuration from a file for route
 * configuration syntax (the Router implementation is adapted from Play!
 * Framework {@link http://www.playframework.org/documentation/1.0.3/routes#syntax}).
 * 
 * Example:
 * 
 * <pre class="code">
 * GET    /home                   PageController.showPage(id:'home')
 * GET    /page/{id}              PageController.showPage
 * POST   /customer/{<[0-9]+>customerid}  CustomerController.createCustomer
 * </pre>
 * <p>
 * The {@link RouterHandlerAdapter} is responsible for choosing and invoking the
 * right controller method, as mapped by this HandlerMapping.
 * 
 * @author Brian Clozel
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
 */
public class RouterHandlerMapping extends AbstractHandlerMapping {

    private static final Log logger = LogFactory.getLog(RouterHandlerMapping.class);

    private String routeFile;
    private String servletPrefix;

    /**
     * Servlet Prefix to be added in front of all routes
     * Injected by bean configuration (in servlet.xml)
     */
    public String getServletPrefix() {
        return servletPrefix;
    }

    public void setServletPrefix(String servletPrefix) {
        this.servletPrefix = servletPrefix;
    }

    /**
     * Routes configuration File name<
     * Injected by bean configuration (in servlet.xml)
     */
    public String getRouteFile() {
        return routeFile;
    }

    public void setRouteFile(String routeFile) {
        this.routeFile = routeFile;
    }


    /**
     *
     * @param request the HTTP Servlet request
     * @return a RouterHandler, containing matching route + wrapped request
     */
    @Override
    protected Object getHandlerInternal(HttpServletRequest request)
            throws Exception {

        RouterHandler handler;
        try {
            // Route request and resolve format

            // Adapt HTTPServletRequest for Router
            HTTPRequestAdapter rq = HTTPRequestAdapter.parseRequest(request);

            Router.Route route = Router.route(rq);
            rq.resolveFormat();

            handler = new RouterHandler();
            handler.setRequest(rq);
            handler.setRoute(route);

        } catch (NoRouteFoundException nrfe) {
            handler = null;
            logger.trace("no route found for method[" + nrfe.method
                    + "] and path[" + nrfe.path + "]");
        }

        return handler;
    }

    /**
     * Inits Routes from route configuration file
     */
    @Override
    protected void initApplicationContext() throws BeansException {

        super.initApplicationContext();

        try {
            // load routes configuration file and parse all routes
            File file = new File(this.getClass().getClassLoader().getResource(
                    routeFile).toURI());

            Router.load(file, this.servletPrefix);

        } catch (IOException e) {
            throw new RouteFileParsingException(
                    "Cannot parse route file routes.conf", e);
        } catch (URISyntaxException e) {
            throw new RouteFileParsingException(
                    "Cannot parse route file routes.conf", e);
        }
    }
}