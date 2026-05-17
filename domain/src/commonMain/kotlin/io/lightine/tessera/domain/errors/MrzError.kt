package io.lightine.tessera.domain.errors

public sealed class MrzError {
    public abstract val description: String
}
