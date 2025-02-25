/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences

/**
 * [Boolean] preference.
 */
class BooleanPreference<T : Boolean?>(
    override val key: String,
    override val defaultValue: T,
) : Preference<T> {
    @Suppress("UNCHECKED_CAST")
    override fun SharedPreferences.getValueImpl() = getBoolean(
        key, defaultValue ?: false
    ) as T

    override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
        putBoolean(key, value)
    }
}
