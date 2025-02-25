/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences

/**
 * Preference.
 */
interface Preference<T> {
    /**
     * The unique key for this preference.
     */
    val key: String

    /**
     * The default value.
     */
    val defaultValue: T

    /**
     * Get the value. Will only be called if the property is set.
     */
    fun SharedPreferences.getValueImpl(): T

    /**
     * Set the value.
     *
     * @param value The new value
     */
    fun SharedPreferences.Editor.setValueImpl(value: T & Any)

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> preference(
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

        inline fun <T, reified PV> enumPreference(
            key: String,
            defaultValue: T,
            valueToEnumMap: Map<PV, T>,
        ): Preference<T> = EnumPreference(
            backingPreference = preference<PV?>(key, null),
            defaultValue = defaultValue,
            preferenceValueToEnumMap = valueToEnumMap,
        )

        inline fun <T : E?, reified E : Enum<E>, reified PV> enumPreference(
            key: String,
            defaultValue: T,
            noinline mapper: T.() -> PV?,
        ): Preference<T> = enumPreference<T, PV>(
            key = key,
            defaultValue = defaultValue,
            valueToEnumMap = buildMap {
                enumValues<E>().forEach { enum ->
                    @Suppress("UNCHECKED_CAST")
                    val enumAsT = enum as T

                    enumAsT.mapper()?.let {
                        put(it, enumAsT)
                    }
                }
            },
        )
    }
}
