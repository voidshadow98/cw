package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory data store using ConcurrentHashMap.
 *
 * Singleton pattern ensures one shared instance across all JAX-RS resource objects.
 * JAX-RS default lifecycle creates a NEW resource instance per request, meaning
 * multiple threads can access this store simultaneously. ConcurrentHashMap prevents
 * race conditions and data loss under concurrent load, unlike a plain HashMap.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        // Seed rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("CS-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Seed sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 450.0, "CS-101");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LIB-301");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.addSensorId(s1.getId());
        r1.addSensorId(s3.getId());
        r2.addSensorId(s2.getId());

        // Initialise reading lists
        readings.put(s1.getId(), new ArrayList<>());
        readings.put(s2.getId(), new ArrayList<>());
        readings.put(s3.getId(), new ArrayList<>());
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        getReadingsForSensor(sensorId).add(reading);
    }
}
