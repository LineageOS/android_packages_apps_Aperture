/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences

/**
 * [Int] preference.
 */
class IntPreference<T : Int?>(
    override val key: String,
    override val defaultValue: T,
) : Preference<T> {
    @Suppress("UNCHECKED_CAST")
    override fun SharedPreferences.getValueImpl() = getInt(
        key, defaultValue ?: 0
    ) as T

    override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
        putInt(key, value)
    }
}
