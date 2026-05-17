package io.lightine.tessera.domain.errors

public sealed class MrzValidationError {
    public abstract val description: String
}
