package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.DataStore;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 2 — Room Resource: /api/v1/rooms
 *
 * Part 1.1 — JAX-RS Lifecycle:
 * By default JAX-RS creates a NEW resource instance per request (per-request lifecycle).
 * Instance fields would be lost between requests, so all shared state lives in the
 * DataStore singleton backed by ConcurrentHashMap — preventing race conditions when
 * multiple threads hit the API simultaneously.
 *
 * Part 2.1 — Full objects vs IDs:
 * Returning full Room objects costs more bandwidth per call but means clients are
 * immediately ready without follow-up requests. Returning only IDs forces N additional
 * GET calls (the "N+1 problem"), increasing latency and server load for large campuses.
 * Industry best practice: return full objects for moderate collections; paginate for large ones.
 *
 * Part 2.2 — Idempotency of DELETE:
 * DELETE is idempotent per RFC 7231: repeated identical requests leave the server in
 * the same state. First DELETE → room removed → 204 No Content. Subsequent identical
 * requests → room already gone → 404 Not Found. Server state (room absent) is identical
 * after each call; only the status code differs, which is permitted by the spec.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(new ArrayList<>(rooms)).build();
    }

    // POST /api/v1/rooms
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return error(400, "Bad Request", "Room must include a non-empty 'id' field.");
        }
        if (store.getRooms().containsKey(room.getId())) {
            return error(409, "Conflict", "A room with id '" + room.getId() + "' already exists.");
        }
        store.getRooms().put(room.getId(), room);
        return Response
                .created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room).build();
    }

    // GET /api/v1/rooms/{roomId}
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return error(404, "Not Found", "Room '" + roomId + "' not found.");
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId}
    // Business rule: blocked if room still has sensors (prevents orphan sensors)
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return error(404, "Not Found", "Room '" + roomId + "' not found.");
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }

    private Response error(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return Response.status(status).entity(body).build();
    }
}
