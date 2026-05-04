package io.lightine.tessera.domain

public sealed class MrzValidationError {
    public abstract val description: String
}
