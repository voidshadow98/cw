package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.DataStore;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 3 — Sensor Resource: /api/v1/sensors
 *
 * Part 3.1 — @Consumes and Content-Type mismatch:
 * If a client sends text/plain or application/xml, JAX-RS inspects the Content-Type
 * header before invoking any business logic. Finding no @Consumes match, it automatically
 * returns HTTP 415 Unsupported Media Type — the method body never executes.
 *
 * Part 3.2 — Query param vs path segment for filtering:
 * GET /sensors?type=CO2 is preferred over GET /sensors/type/CO2 because:
 * - Path segments identify resources; query params filter/modify collections.
 * - Multiple filters compose naturally: ?type=CO2&status=ACTIVE
 * - RFC 3986 reserves paths for resource hierarchy, not search predicates.
 * - HTTP caching treats query strings and paths differently; filters as query
 *   params align with standard caching semantics.
 *
 * Part 5.2 — HTTP 422 vs 404 for missing referenced roomId:
 * 404 means the URL requested does not exist. Here /api/v1/sensors is valid —
 * the problem is a value INSIDE the JSON body references a non-existent room.
 * HTTP 422 Unprocessable Entity (RFC 4918) means the request is well-formed but
 * contains a semantic/business rule violation — far more informative than 404.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/sensors  or  GET /api/v1/sensors?type=CO2
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filtered = new ArrayList<>();
            for (Sensor s : all) {
                if (s.getType().equalsIgnoreCase(type)) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }
        return Response.ok(new ArrayList<>(all)).build();
    }

    // POST /api/v1/sensors
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return error(400, "Bad Request", "Sensor must include a non-empty 'id' field.");
        }
        // Validate that the referenced roomId actually exists
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return error(409, "Conflict", "Sensor '" + sensor.getId() + "' already exists.");
        }
        store.getSensors().put(sensor.getId(), sensor);
        // Link sensor into its room
        Room room = store.getRooms().get(sensor.getRoomId());
        room.addSensorId(sensor.getId());
        return Response
                .created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor).build();
    }

    // GET /api/v1/sensors/{sensorId}
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return error(404, "Not Found", "Sensor '" + sensorId + "' not found.");
        }
        return Response.ok(sensor).build();
    }

    // DELETE /api/v1/sensors/{sensorId}
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return error(404, "Not Found", "Sensor '" + sensorId + "' not found.");
        }
        if (sensor.getRoomId() != null) {
            Room room = store.getRooms().get(sensor.getRoomId());
            if (room != null) room.removeSensorId(sensorId);
        }
        store.getSensors().remove(sensorId);
        return Response.noContent().build();
    }

    /**
     * Part 4 — Sub-Resource Locator for /api/v1/sensors/{sensorId}/readings
     * No HTTP method annotation here — JAX-RS delegates path resolution to the
     * returned SensorReadingResource instance at runtime.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Response error(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return Response.status(status).entity(body).build();
    }
}
