package io.lightine.tessera.mrz.transliteration

/**
 * Registry of [TransliterationProfile]s known to the SDK. The ICAO default profile is
 * pre-registered.
 *
 * Register additional profiles at application startup before concurrent use; concurrent
 * registration is not supported in this release. [register] adds or replaces a profile
 * by [TransliterationProfile.identifier].
 */
public object TransliterationProfileRegistry {
    private val profiles: MutableMap<String, TransliterationProfile> = mutableMapOf()

    init {
        register(IcaoDefaultTransliterationProfile)
    }

    public fun register(profile: TransliterationProfile) {
        profiles[profile.identifier] = profile
    }

    public fun lookup(identifier: String): TransliterationProfile? = profiles[identifier]

    public fun all(): List<TransliterationProfile> = profiles.values.toList()
}
