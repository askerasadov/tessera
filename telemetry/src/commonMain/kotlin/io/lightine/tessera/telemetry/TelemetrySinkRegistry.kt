package io.lightine.tessera.telemetry

/**
 * Holds the currently-registered [TelemetrySink]. SDK modules that emit telemetry look
 * up the sink via [current] when they need to record an event. The initial value is
 * [NoOpTelemetrySink].
 *
 * **Usage contract:** register a sink at application startup before concurrent use.
 * Concurrent registration is not supported in this release. Matches the precedent set
 * by `TransliterationProfileRegistry` in the `mrz-core` module.
 *
 * **0.1.0 behavior:** no SDK module reads [current] in 0.1.0, so registration is
 * forward-compatibility scaffolding. The first real readers are the camera module
 * (0.2.0) and the NFC module (0.6.0).
 */
public object TelemetrySinkRegistry {
    private var sink: TelemetrySink = NoOpTelemetrySink

    /** Registers [sink] as the current sink, replacing any previously-registered one. */
    public fun register(sink: TelemetrySink) {
        this.sink = sink
    }

    /** Restores the [NoOpTelemetrySink] default. Primarily useful for test isolation. */
    public fun reset() {
        this.sink = NoOpTelemetrySink
    }

    /** The currently-registered sink. Returns [NoOpTelemetrySink] if none was registered. */
    public val current: TelemetrySink
        get() = sink
}
