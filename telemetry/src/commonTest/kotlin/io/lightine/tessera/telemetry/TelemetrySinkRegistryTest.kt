package io.lightine.tessera.telemetry

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TelemetrySinkRegistryTest {
    @BeforeTest
    fun resetBeforeTest() {
        TelemetrySinkRegistry.reset()
    }

    @AfterTest
    fun resetAfterTest() {
        TelemetrySinkRegistry.reset()
    }

    @Test
    fun default_sink_is_no_op() {
        assertSame(NoOpTelemetrySink, TelemetrySinkRegistry.current)
    }

    @Test
    fun register_sets_current_sink() {
        val sink = CapturingTelemetrySink()
        TelemetrySinkRegistry.register(sink)
        assertSame(sink, TelemetrySinkRegistry.current)
    }

    @Test
    fun register_replaces_previous_sink() {
        val first = CapturingTelemetrySink()
        val second = CapturingTelemetrySink()
        TelemetrySinkRegistry.register(first)
        TelemetrySinkRegistry.register(second)
        assertSame(second, TelemetrySinkRegistry.current)
    }

    @Test
    fun reset_restores_no_op_default() {
        TelemetrySinkRegistry.register(CapturingTelemetrySink())
        TelemetrySinkRegistry.reset()
        assertSame(NoOpTelemetrySink, TelemetrySinkRegistry.current)
    }

    @Test
    fun registered_sink_receives_events_via_record() {
        val sink = CapturingTelemetrySink()
        TelemetrySinkRegistry.register(sink)
        val event: TelemetryEvent = NamedEvent("test.event")
        TelemetrySinkRegistry.current.record(event)
        assertEquals(listOf(event), sink.recorded)
    }

    private class CapturingTelemetrySink : TelemetrySink {
        val recorded: MutableList<TelemetryEvent> = mutableListOf()

        override fun record(event: TelemetryEvent) {
            recorded.add(event)
        }
    }

    private data class NamedEvent(
        override val name: String,
    ) : TelemetryEvent
}
