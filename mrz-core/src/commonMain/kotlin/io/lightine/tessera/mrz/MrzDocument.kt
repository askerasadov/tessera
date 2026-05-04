package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzFormat

public sealed class MrzDocument {
    public abstract val rawLines: List<String>
    public abstract val format: MrzFormat
    public abstract val commonFields: CommonFields
}
