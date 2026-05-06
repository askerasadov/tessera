package io.lightine.tessera.mrz

import io.lightine.tessera.domain.CountryCodeCategory
import kotlin.jvm.JvmInline

@JvmInline
public value class CountryCode(
    public val rawCode: String,
) {
    public val entry: CountryCodeEntry?
        get() = CountryCodeTable.lookup(rawCode)

    public val isRecognized: Boolean
        get() = entry != null

    public val displayName: String?
        get() = entry?.displayName

    public val category: CountryCodeCategory?
        get() = entry?.category
}
