/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences

/**
 * [Long] preference.
 */
class LongPreference<T : Long?>(
    override val key: String,
    override val defaultValue: T,
) : Preference<T> {
    @Suppress("UNCHECKED_CAST")
    override fun SharedPreferences.getValueImpl() = getLong(
        key, defaultValue ?: 0L
    ) as T

    override fun SharedPreferences.Editor.setValueImpl(value: T & Any) {
        putLong(key, value)
    }
}
