package com.yral.featureflag.core

import kotlinx.serialization.KSerializer

internal data class BasicFeatureFlag<T>(
    override val key: String,
    override val name: String,
    override val description: String,
    override val defaultValue: T,
    override val audience: FlagAudience = FlagAudience.INTERNAL_QA,
    override val codec: FlagCodec<T>,
) : FeatureFlag<T>

open class FlagGroup internal constructor(
    private val keyPrefix: String,
    private val defaultAudience: FlagAudience = FlagAudience.INTERNAL_QA,
) {
    private val _allFlags = mutableListOf<FeatureFlag<*>>()
    val allFlags: List<FeatureFlag<*>> get() = _allFlags

    internal fun boolean(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: Boolean,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<Boolean> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            BooleanCodec,
        ).also { _allFlags.add(it) }

    internal fun string(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: String,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<String> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            StringCodec,
        ).also { _allFlags.add(it) }

    internal fun int(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: Int,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<Int> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            IntCodec,
        ).also { _allFlags.add(it) }

    internal fun long(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: Long,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<Long> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            LongCodec,
        ).also { _allFlags.add(it) }

    internal fun double(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: Double,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<Double> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            DoubleCodec,
        ).also { _allFlags.add(it) }

    internal fun <E : Enum<E>> enum(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: E,
        values: Array<E>,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<E> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            EnumCodec(values),
        ).also { _allFlags.add(it) }

    internal fun <T> json(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: T,
        serializer: KSerializer<T>,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<T> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            JsonCodec(serializer),
        ).also { _allFlags.add(it) }

    internal fun stringList(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: List<String>,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<List<String>> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            StringListCodec(),
        ).also { _allFlags.add(it) }

    internal fun stringMap(
        keySuffix: String,
        name: String,
        description: String,
        defaultValue: Map<String, String>,
        audience: FlagAudience = defaultAudience,
    ): FeatureFlag<Map<String, String>> =
        BasicFeatureFlag(
            fullKey(keySuffix),
            name,
            description,
            defaultValue,
            audience,
            StringMapCodec(),
        ).also { _allFlags.add(it) }

    private fun fullKey(suffix: String) = if (suffix.isEmpty()) keyPrefix else "$keyPrefix.$suffix"
}
