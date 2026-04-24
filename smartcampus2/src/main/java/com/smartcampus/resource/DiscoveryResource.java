package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1.2 — Discovery Endpoint: GET /api/v1/
 *
 * HATEOAS (Hypermedia as the Engine of Application State) is a hallmark of advanced
 * REST because it makes the API self-describing and self-navigable. Clients can
 * discover all available resources from a single entry point by following embedded
 * links — just as humans navigate websites via hyperlinks rather than memorising URLs.
 *
 * Benefits over static documentation:
 * - Links always reflect the live server state; documentation can go stale.
 * - Clients adapt to new resources at runtime without code changes.
 * - Server can change URL structure without breaking clients that follow links.
 * - No separate API portal needed for basic navigation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new HashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact", "admin@smartcampus.university.ac.uk");
        response.put("basePath", "/api/v1");

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        Map<String, String> resources = new HashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);

        return Response.ok(response).build();
    }
}
