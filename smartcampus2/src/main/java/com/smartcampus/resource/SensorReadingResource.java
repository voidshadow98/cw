package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.DataStore;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 4 — Sub-Resource for sensor readings.
 *
 * Part 4.1 — Sub-Resource Locator Pattern benefits:
 * - Single Responsibility: SensorResource owns sensor CRUD; this class owns readings only.
 * - Each class is independently testable in isolation.
 * - Reduces complexity: developers navigate to the right class for the right concern.
 * - Open/Closed Principle: adding /alerts or /calibrations means a new locator + new class,
 *   existing code is untouched.
 * - JAX-RS resolves /sensors/{id}/readings at runtime through the locator chain,
 *   giving full annotation support on the sub-resource class.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return error(404, "Not Found", "Sensor '" + sensorId + "' not found.");
        }
        List<SensorReading> list = store.getReadingsForSensor(sensorId);
        return Response.ok(list).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings
    // Side effect: updates sensor.currentValue to maintain data consistency
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading incoming) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return error(404, "Not Found", "Sensor '" + sensorId + "' not found.");
        }
        // State constraint: MAINTENANCE sensors cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }
        SensorReading reading = new SensorReading(incoming.getValue());
        store.addReading(sensorId, reading);
        // Keep parent Sensor.currentValue in sync with the latest reading
        sensor.setCurrentValue(reading.getValue());
        return Response
                .created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + reading.getId()))
                .entity(reading).build();
    }

    private Response error(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return Response.status(status).entity(body).build();
    }
}
