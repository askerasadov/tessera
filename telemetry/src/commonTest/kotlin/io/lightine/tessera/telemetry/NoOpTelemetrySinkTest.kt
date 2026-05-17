package io.lightine.tessera.telemetry

import kotlin.test.Test

class NoOpTelemetrySinkTest {
    @Test
    fun record_discards_event_without_throwing() {
        NoOpTelemetrySink.record(NamedEvent("no-op.event"))
    }

    private data class NamedEvent(
        override val name: String,
    ) : TelemetryEvent
}
