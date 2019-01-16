/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of ee-logback-level
 *
 * ee-logback-level is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ee-logback-level is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.logback.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import dk.dbc.remoteadminaccess.RequiresAdmin;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.*;

/**
 * Log-level manipulator
 * <p>
 * This is a Bean that registers under the path "log-level", and allows for
 * changing log-levels of a logback configuration in runtime.
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Path("log-level")
@Stateless
@LocalBean
@RequiresAdmin
public class LogLevelAdminBean {

    private static final Logger log = LoggerFactory.getLogger(LogLevelAdminBean.class);

    /**
     * Provide a html page with the needed JavaScript embedded
     *
     * @return UNAUTHORIZED or a html index page
     */
    @GET
    public Response indexHtml() {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("LogLevelAdminBean-loglevel.html");
        if (is == null) // This is really messed up - our own resource is not present
            return Response.status(NOT_FOUND)
                    .build();
        return Response.ok(is)
                .type(MediaType.TEXT_HTML_TYPE)
                .build();
    }

    /**
     * Set a log-level
     *
     * @param logger name of logger to change (required)
     * @param level  level to set it to (missing or empty string for
     *               inherited)
     * @return {@link #loglevels()}
     */
    @Path("level")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loglevel(@QueryParam("logger") String logger,
                             @QueryParam("level") String level) {
        if (logger == null || logger.isEmpty()) {
            return Response.status(BAD_REQUEST)
                    .entity("logger is a required query parameter")
                    .build();
        }
        log.info("Setting logger: {} to: {}", logger, level);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(logger).setLevel(level == null || level.isEmpty() ? null :
                                           Level.valueOf(level.toUpperCase(Locale.ROOT)));
        return loglevels();
    }

    /**
     * Get a map of loggers and their level
     *
     * @return Unauthorized/{@link #loglevels()}
     */
    @Path("levels")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loglevels() {
        HashMap<String, String> map = new HashMap<>();
        ( (LoggerContext) LoggerFactory.getILoggerFactory() )
                .getLoggerList()
                .stream()
                .forEach(e -> map.put(e.getName(), levelToString(e)));
        return Response.ok(map)
                .build();
    }

    private static String levelToString(ch.qos.logback.classic.Logger logger) {
        Level l = logger.getLevel();
        if (l == null)
            return null;
        return l.toString();
    }

}
