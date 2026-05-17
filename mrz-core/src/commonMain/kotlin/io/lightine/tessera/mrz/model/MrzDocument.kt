package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

public sealed class MrzDocument {
    public abstract val rawLines: List<String>
    public abstract val format: MrzFormat
    public abstract val commonFields: CommonFields
}
