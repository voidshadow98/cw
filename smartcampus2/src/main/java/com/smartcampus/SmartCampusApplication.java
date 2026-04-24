package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 *
 * @ApplicationPath("/api/v1") sets the versioned base URL for all resources.
 *
 * Part 1.1 — Default JAX-RS Lifecycle:
 * JAX-RS creates a NEW resource class instance for every incoming HTTP request
 * (per-request lifecycle). This is thread-safe by nature since each thread gets
 * its own object. However, instance fields would be lost between requests, which
 * is why shared state lives in the DataStore singleton backed by ConcurrentHashMap,
 * preventing data loss and race conditions under concurrent load.
 *
 * Jersey 2.x with package scanning auto-discovers all @Path, @Provider classes
 * in the registered packages — no manual class registration needed.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-scans packages configured in Main.java ResourceConfig
}
