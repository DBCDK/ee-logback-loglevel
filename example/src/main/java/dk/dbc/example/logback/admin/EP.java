/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of ee-logback-level-example
 *
 * ee-logback-level-example is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ee-logback-level-example is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.example.logback.admin;

import java.time.Instant;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Stateless
@LocalBean
@Path("ts")
public class EP {

    private static final Logger log = LoggerFactory.getLogger(EP.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response ts() {
        log.trace("traaace");
        log.debug("debuuug");
        log.info("infoooo");
        log.warn("waaarning");
        log.error("errooooor");
        return Response.ok(Instant.now().toString()).build();
    }

}
