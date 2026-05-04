package io.lightine.tessera.domain

public enum class ReadMethod {
    LIVE_CAMERA,
    PRE_CAPTURED_IMAGE,
    MANUAL_ENTRY,
    NFC_CHIP,
    BACKEND_STRING_INPUT,
    MIXED,
}
