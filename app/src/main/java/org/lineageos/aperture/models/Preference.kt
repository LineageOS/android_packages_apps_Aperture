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
     * @param value The value to set
     */
    fun setValue(editor: SharedPreferences.Editor, value: T)

    /**
     * Preference backed by a [SharedPreferences] key.
     *
     * @param T The type of the preference
     */
    sealed class PrimitivePreference<T>(
        override val key: String,
        private val defaultValue: T,
        private val getter: SharedPreferences.() -> T & Any,
        private val setter: SharedPreferences.Editor.(String, T & Any) -> Unit,
    ) : Preference<T> {
        override fun getValue(
            sharedPreferences: SharedPreferences,
        ) = when (sharedPreferences.contains(key)) {
            true -> sharedPreferences.getter()
            false -> defaultValue
        }

        override fun setValue(editor: SharedPreferences.Editor, value: T) {
            value?.also { editor.setter(key, it) } ?: editor.remove(key)
        }
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
    class EnumPreference<T, BT>(
        private val backingPreference: Preference<BT>,
        private val defaultValue: T,
        private val enumToPreferenceValue: Map<T, BT>,
    ) : Preference<T> {
        private val preferenceValueToEnum: Map<BT, T> = enumToPreferenceValue.entries.associate {
            it.value to it.key
        }

        override val key = backingPreference.key

        override fun getValue(
            sharedPreferences: SharedPreferences,
        ) = preferenceValueToEnum.getOrDefault(
            backingPreference.getValue(sharedPreferences),
            defaultValue,
        )

        override fun setValue(
            editor: SharedPreferences.Editor,
            value: T,
        ) = backingPreference.setValue(editor, enumToPreferenceValue.getValue(value))
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

        inline fun <reified T, reified BT> enumPreference(
            key: String,
            defaultValue: T,
            enumValues: Set<T & Any>,
            enumMapper: (T) -> BT,
        ): Preference<T> = EnumPreference(
            backingPreference = primitivePreference<BT>(key, enumMapper(defaultValue)),
            defaultValue = defaultValue,
            enumToPreferenceValue = buildMap {
                enumValues.forEach {
                    put(it, enumMapper(it))
                }

                when (val nullAsT: T? = null) {
                    is T -> put(nullAsT, enumMapper(nullAsT))
                }
            },
        )

        inline fun <reified T, reified BT> enumPreference(
            key: String,
            defaultValue: T,
            enumToPreferenceValue: Map<T, BT>,
        ): Preference<T> = enumPreference<T, BT>(
            key = key,
            defaultValue = defaultValue,
            enumValues = enumToPreferenceValue.keys.filterNotNull().toSet(),
            enumMapper = { enumToPreferenceValue.getValue(it) },
        )

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : E?, reified BT, reified E : Enum<E>> enumPreference(
            key: String,
            defaultValue: T,
            enumMapper: (T) -> BT,
        ): Preference<T> = enumPreference<T, BT>(
            key = key,
            defaultValue = defaultValue,
            enumValues = enumValues<E>().toSet() as Set<T & Any>,
            enumMapper = enumMapper,
        )
    }
}
