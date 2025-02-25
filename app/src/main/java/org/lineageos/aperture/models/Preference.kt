/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.runBlocking
import org.lineageos.aperture.ext.preferenceFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Preference.
 *
 * @param sharedPreferences The [SharedPreferences]
 * @param key The unique key for this preference
 */
sealed class Preference<T>(
    protected val sharedPreferences: SharedPreferences,
    val key: String,
    protected val defaultValue: T,
) : ReadWriteProperty<Any?, T> {
    /**
     * Get the value.
     */
    abstract fun getValue(): T

    /**
     * Set the value.
     *
     * @param value The new value
     */
    protected abstract fun SharedPreferences.Editor.setValueImpl(value: T)

    override fun getValue(thisRef: Any?, property: KProperty<*>) = runBlocking {
        getValue()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = runBlocking {
        setValue(value)
    }

    /**
     * Check whether or not a preference is set.
     */
    fun isSet() = sharedPreferences.contains(key)

    /**
     * Set the value.
     */
    fun setValue(value: T) = sharedPreferences.edit {
        setValueImpl(value)
    }

    fun valueFlow() = sharedPreferences.preferenceFlow(key, getter = { getValue() })

    /**
     * Boolean preference.
     */
    class BooleanPreference(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Boolean,
    ) : Preference<Boolean>(sharedPreferences, key, defaultValue) {
        override fun getValue() = sharedPreferences.getBoolean(
            key, defaultValue
        )

        override fun SharedPreferences.Editor.setValueImpl(value: Boolean) {
            putBoolean(key, value)
        }
    }

    /**
     * String preference.
     */
    class StringPreference<T : String?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValue() = sharedPreferences.getString(
            key, defaultValue
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T) {
            putString(key, value)
        }
    }

    /**
     * Enum preference. Backed by strings.
     */
    class EnumPreference<E : Enum<E>>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: E?,
        private val enumToValue: EnumToValue<E>,
    ) : Preference<E?>(sharedPreferences, key, defaultValue) {
        /**
         * Bi-directional map of [E] to its preference value.
         *
         * @param enumValues All [E] entries
         * @param mapper A mapper from [E] to its preference value
         */
        class EnumToValue<E : Enum<E>>(
            enumValues: Array<E>,
            private val mapper: E.() -> String
        ) {
            /**
             * Map of [E] to its preference value.
             */
            private val enumToValue = enumValues.associateWith { it.mapper() }

            /**
             * Inverse map of preference value to it's [E].
             */
            private val valueToEnum = enumToValue.entries.associate { (key, value) -> value to key }

            fun enumToValue(enum: E) = enumToValue[enum]

            fun valueToEnum(value: String) = valueToEnum[value]

            companion object {
                inline operator fun <reified E : Enum<E>> invoke(
                    noinline mapper: E.() -> String
                ) = EnumToValue(enumValues<E>(), mapper)
            }
        }

        private val stringPreference = StringPreference(
            sharedPreferences = sharedPreferences,
            key = key,
            defaultValue = defaultValue?.let { enumToValue.enumToValue(it) },
        )

        override fun getValue(): E? = stringPreference.getValue()?.let {
            @Suppress("UNCHECKED_CAST")
            enumToValue.valueToEnum(it)
        } ?: defaultValue

        override fun SharedPreferences.Editor.setValueImpl(value: T) {
            stringPreference.setValue(value?.let { enumToValue.enumToValue(it) })
        }
    }
}
