/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences

/**
 * Enum preference.
 *
 * @param T The enum type
 * @param PV The backing preference value type
 */
class EnumPreference<T, PV>(
    private val backingPreference: Preference<PV?>,
    override val defaultValue: T,
    preferenceValueToEnumMap: Map<PV, T>,
) : Preference<T> {
    /**
     * Bi-directional map of [T] to its preference value [PV].
     *
     * @param T The enum type
     * @param PV The backing preference value type
     * @param preferenceValueToEnum Map of [PV] to [T]
     */
    class EnumToPreferenceValue<T, PV>(private val preferenceValueToEnum: Map<PV, T>) {
        /**
         * Inverse map of [T] to its preference value [PV].
         */
        private val enumToPreferenceValue = preferenceValueToEnum.entries.associate { (k, v) ->
            v to k
        }

        fun enumToPreferenceValue(enum: T) = enumToPreferenceValue[enum]

        fun preferenceValueToEnum(value: PV) = preferenceValueToEnum[value]
    }

    private val enumToPreferenceValue = EnumToPreferenceValue(preferenceValueToEnumMap)

    override val key = backingPreference.key

    @Suppress("UNCHECKED_CAST")
    override fun SharedPreferences.getValueImpl(): T = backingPreference.run {
        getValueImpl()?.let {
            enumToPreferenceValue.preferenceValueToEnum(it) as T
        } ?: defaultValue as T
    }

    override fun SharedPreferences.Editor.setValueImpl(value: T & Any) = backingPreference.run {
        setValueImpl(
            enumToPreferenceValue.enumToPreferenceValue(value) ?: error("Unexpected value $value")
        )
    }
}
