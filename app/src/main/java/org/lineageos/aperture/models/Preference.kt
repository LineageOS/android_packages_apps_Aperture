/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.content.SharedPreferences

/**
 * Preference definition.
 *
 * @param T The type of the preference
 */
sealed interface Preference<T> {
    /**
     * Get the key of the preference.
     */
    val key: String

    /**
     * Get the value of the preference from the preferences.
     *
     * @param sharedPreferences The [SharedPreferences] to get the value from
     * @return The value of the preference, or null if it doesn't exist
     */
    fun getValue(sharedPreferences: SharedPreferences): T

    /**
     * Update the preferences with the given value.
     *
     * @param editor The [SharedPreferences.Editor] to update
     * @param value The value to set, null to remove the preference
     */
    fun setValue(editor: SharedPreferences.Editor, value: T?)

    /**
     * Preference backed by a [SharedPreferences] key.
     *
     * @param T The type of the preference
     * @param BT The type of the backing preference
     */
    sealed class BackingPreference<T, BT : Any>(
        override val key: String,
        private val defaultValue: T,
        private val getter: SharedPreferences.() -> BT,
        private val setter: SharedPreferences.Editor.(String, BT) -> Unit,
    ) : Preference<T> {
        protected abstract fun backingValueToValue(backingValue: BT): T
        protected abstract fun valueToBackingValue(value: T & Any): BT

        override fun getValue(
            sharedPreferences: SharedPreferences,
        ) = when (sharedPreferences.contains(key)) {
            true -> sharedPreferences.getter().let(::backingValueToValue)
            false -> defaultValue
        }

        override fun setValue(editor: SharedPreferences.Editor, value: T?) {
            value?.also {
                setter(editor, key, valueToBackingValue(it))
            } ?: editor.remove(key)
        }
    }

    /**
     * Direct access to a primitive preference.
     */
    sealed class PrimitivePreference<T>(
        key: String,
        defaultValue: T,
        getter: SharedPreferences.() -> T & Any,
        setter: SharedPreferences.Editor.(String, T & Any) -> Unit,
    ) : BackingPreference<T, T & Any>(key, defaultValue, getter, setter) {
        override fun backingValueToValue(backingValue: T & Any) = backingValue
        override fun valueToBackingValue(value: T & Any) = value
    }

    /**
     * [Boolean] preference.
     */
    class BooleanPreference<T : Boolean?>(
        key: String,
        defaultValue: T,
    ) : PrimitivePreference<T>(
        key,
        defaultValue,
        { getBoolean(key, false).forceCast() },
        SharedPreferences.Editor::putBoolean,
    )

    /**
     * [Float] preference.
     */
    class FloatPreference<T : Float?>(
        key: String,
        defaultValue: T,
    ) : PrimitivePreference<T>(
        key,
        defaultValue,
        { getFloat(key, 0f).forceCast() },
        SharedPreferences.Editor::putFloat,
    )

    /**
     * [Int] preference.
     */
    class IntPreference<T : Int?>(
        key: String,
        defaultValue: T,
    ) : PrimitivePreference<T>(
        key,
        defaultValue,
        { getInt(key, 0).forceCast() },
        SharedPreferences.Editor::putInt,
    )

    /**
     * [Long] preference.
     */
    class LongPreference<T : Long?>(
        key: String,
        defaultValue: T,
    ) : PrimitivePreference<T>(
        key,
        defaultValue,
        { getLong(key, 0L).forceCast() },
        SharedPreferences.Editor::putLong,
    )

    /**
     * [String] preference.
     */
    class StringPreference<T : String?>(
        key: String,
        defaultValue: T,
    ) : PrimitivePreference<T>(
        key,
        defaultValue,
        { getString(key, "")!!.forceCast() },
        SharedPreferences.Editor::putString,
    )

    /**
     * Enum preference.
     *
     * @param T The enum type
     * @param BT The backing preference value type
     */
    class EnumPreference<T, BT : Any>(
        key: String,
        private val defaultValue: T,
        private val backingPreference: Preference<BT?>,
        private val preferenceValueToEnum: Map<BT, T>,
        private val enumToPreferenceValue: Map<T, BT> = preferenceValueToEnum.entries.associate {
            it.value to it.key
        },
    ) : BackingPreference<T, BT>(
        key,
        defaultValue,
        {
            backingPreference.getValue(this) ?: enumToPreferenceValue.getValue(defaultValue)
        },
        { _, value -> backingPreference.setValue(this, value) },
    ) {
        override fun backingValueToValue(backingValue: BT): T = preferenceValueToEnum[
            backingValue
        ] ?: defaultValue

        override fun valueToBackingValue(value: T & Any): BT = enumToPreferenceValue.getValue(value)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> Any.forceCast() = this as T

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> primitivePreference(
            key: String,
            defaultValue: T,
        ): Preference<T> = when (T::class) {
            Boolean::class -> when (null is T) {
                true -> BooleanPreference(key, defaultValue as Boolean?)
                false -> BooleanPreference(key, defaultValue as Boolean)
            }

            Float::class -> when (null is T) {
                true -> FloatPreference(key, defaultValue as Float?)
                false -> FloatPreference(key, defaultValue as Float)
            }

            Int::class -> when (null is T) {
                true -> IntPreference(key, defaultValue as Int?)
                false -> IntPreference(key, defaultValue as Int)
            }

            Long::class -> when (null is T) {
                true -> LongPreference(key, defaultValue as Long?)
                false -> LongPreference(key, defaultValue as Long)
            }

            String::class -> when (null is T) {
                true -> StringPreference(key, defaultValue as String?)
                false -> StringPreference(key, defaultValue as String)
            }

            else -> error("Unsupported type")
        } as Preference<T>

        inline fun <T : Any, reified BT : Any> enumPreference(
            key: String,
            defaultValue: T,
            valueToEnumMap: Map<BT, T>,
        ): Preference<T> = EnumPreference(
            key = key,
            backingPreference = primitivePreference<BT?>(key, null),
            defaultValue = defaultValue,
            preferenceValueToEnum = valueToEnumMap,
        )

        @JvmName("nullableEnumPreference")
        inline fun <T : Any?, reified BT : Any> enumPreference(
            key: String,
            defaultValue: T?,
            valueToEnumMap: Map<BT, T>,
        ): Preference<T?> = EnumPreference(
            key = key,
            backingPreference = primitivePreference<BT?>(key, null),
            defaultValue = defaultValue,
            preferenceValueToEnum = valueToEnumMap,
        )

        inline fun <reified T : Enum<T>, reified BT : Any> enumPreference(
            key: String,
            defaultValue: T,
            noinline mapper: (T) -> BT?,
        ): Preference<T> = enumPreference<T, BT>(
            key = key,
            defaultValue = defaultValue,
            valueToEnumMap = buildMap {
                enumValues<T>().forEach { enum ->
                    mapper(enum)?.let {
                        put(it, enum)
                    }
                }
            },
        )

        @JvmName("nullableEnumTEnumPreference")
        inline fun <reified T : Enum<T>, reified BT : Any> enumPreference(
            key: String,
            defaultValue: T?,
            noinline mapper: (T?) -> BT?,
        ): Preference<T?> = enumPreference<T?, BT>(
            key = key,
            defaultValue = defaultValue,
            valueToEnumMap = buildMap {
                mapper(null)?.let {
                    put(it, null)
                }

                enumValues<T>().forEach { enum ->
                    mapper(enum)?.let {
                        put(it, enum)
                    }
                }
            },
        )
    }
}
