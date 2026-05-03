package io.lightine.tessera.mrz

public enum class MrzDateInferenceMethod {
    SLIDING_WINDOW_BIRTH,
    SLIDING_WINDOW_EXPIRY,
    RAW_ONLY,
}
