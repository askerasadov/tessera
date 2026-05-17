package io.lightine.tessera.telemetry

/**
 * The default [TelemetrySink]: discards every event it receives. Registered as the
 * initial sink in [TelemetrySinkRegistry], so SDK modules can always look up a sink
 * without a null check.
 *
 * Consumers who do not want telemetry can ignore the interface entirely — this sink is
 * the default behavior.
 */
public object NoOpTelemetrySink : TelemetrySink {
    override fun record(event: TelemetryEvent) {
        // Intentionally empty.
    }
}
