/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.content.SharedPreferences
import androidx.core.content.edit
import org.lineageos.aperture.ext.preferenceFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Preference.
 *
 * @param sharedPreferences The [SharedPreferences]
 * @param key The unique key for this preference
 * @param defaultValue The default value
 */
abstract class Preference<T>(
    protected val sharedPreferences: SharedPreferences,
    val key: String,
    protected val defaultValue: T,
) : ReadWriteProperty<Any?, T> {
    /**
     * Get the value. Will only be called if [isSet] is true.
     */
    abstract fun getValueImpl(): T

    /**
     * Set the value.
     *
     * @param value The new value
     */
    protected abstract fun SharedPreferences.Editor.setValueImpl(value: T & Any)

    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    /**
     * Check whether or not a preference is set.
     */
    fun isSet() = sharedPreferences.contains(key)

    /**
     * The value.
     */
    var value: T
        get() = when (isSet()) {
            true -> getValueImpl()
            false -> defaultValue
        }
        set(value) = sharedPreferences.edit {
            value?.also { setValueImpl(it) } ?: remove(key)
        }

    fun valueFlow() = sharedPreferences.preferenceFlow(key, getter = { value })

    /**
     * [Boolean] preference.
     */
    class BooleanPreference<T : Boolean?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl() = sharedPreferences.getBoolean(
            key, defaultValue ?: false
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            putBoolean(key, value)
        }
    }

    /**
     * [Float] preference.
     */
    class FloatPreference<T : Float?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl() = sharedPreferences.getFloat(
            key, defaultValue ?: 0f
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            putFloat(key, value)
        }
    }

    /**
     * [Int] preference.
     */
    class IntPreference<T : Int?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl() = sharedPreferences.getInt(
            key, defaultValue ?: 0
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            putInt(key, value)
        }
    }

    /**
     * [Long] preference.
     */
    class LongPreference<T : Long?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl() = sharedPreferences.getLong(
            key, defaultValue ?: 0L
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            putLong(key, value)
        }
    }

    /**
     * [String] preference.
     */
    class StringPreference<T : String?>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl() = sharedPreferences.getString(
            key, defaultValue
        ) as T

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            putString(key, value)
        }
    }

    /**
     * Enum preference.
     *
     * @param T The enum type
     * @param V The backing preference value type
     */
    class EnumPreference<T, V>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
        private val backingPreference: Preference<V?>,
        valueToEnumMap: Map<V, T>,
    ) : Preference<T>(sharedPreferences, key, defaultValue) {
        /**
         * Bi-directional map of [T] to its preference value [V].
         *
         * @param T The enum type
         * @param V The backing preference value type
         * @param valueToEnum Map of [T] to its preference value [V]
         */
        class EnumToValue<T, V>(private val valueToEnum: Map<V, T>) {
            /**
             * Inverse map of preference value to it's [T].
             */
            private val enumToValue = valueToEnum.entries.associate { (key, value) -> value to key }

            fun enumToValue(enum: T) = enumToValue[enum]

            fun valueToEnum(value: V) = valueToEnum[value]
        }

        private val enumToValue = EnumToValue(valueToEnumMap)

        init {
            require(key == backingPreference.key) { "Preference keys must match" }
        }

        @Suppress("UNCHECKED_CAST")
        override fun getValueImpl(): T = backingPreference.value?.let {
            enumToValue.valueToEnum(it) as T
        } ?: defaultValue

        override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
            backingPreference.value = enumToValue.enumToValue(value) ?: error(
                "Unexpected value $value"
            )
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> SharedPreferences.preference(
            key: String,
            defaultValue: T,
        ): Preference<T> = when (T::class) {
            Boolean::class -> when (null is T) {
                true -> BooleanPreference(this, key, defaultValue as Boolean?)
                false -> BooleanPreference(this, key, defaultValue as Boolean)
            }

            Float::class -> when (null is T) {
                true -> FloatPreference(this, key, defaultValue as Float?)
                false -> FloatPreference(this, key, defaultValue as Float)
            }

            Int::class -> when (null is T) {
                true -> IntPreference(this, key, defaultValue as Int?)
                false -> IntPreference(this, key, defaultValue as Int)
            }

            Long::class -> when (null is T) {
                true -> LongPreference(this, key, defaultValue as Long?)
                false -> LongPreference(this, key, defaultValue as Long)
            }

            String::class -> when (null is T) {
                true -> StringPreference(this, key, defaultValue as String?)
                false -> StringPreference(this, key, defaultValue as String)
            }

            else -> error("Unsupported type")
        } as Preference<T>

        inline fun <T, reified V> SharedPreferences.enumPreference(
            key: String,
            defaultValue: T,
            valueToEnumMap: Map<V, T>,
        ): Preference<T> = EnumPreference<T, V>(
            sharedPreferences = this,
            key = key,
            defaultValue = defaultValue,
            backingPreference = preference<V?>(key, null),
            valueToEnumMap = valueToEnumMap,
        )

        inline fun <T : E?, reified E : Enum<E>, reified V> SharedPreferences.enumPreference(
            key: String,
            defaultValue: T,
            noinline mapper: T.() -> V?,
        ): Preference<T> = enumPreference<T, V>(
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
