/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import org.lineageos.aperture.ext.preferenceFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * [Preference] holder.
 */
class PreferenceHolder<T>(
    private val sharedPreferences: SharedPreferences,
    coroutineScope: CoroutineScope,
    preference: Preference<T>,
) : Preference<T> by preference, ReadWriteProperty<Any?, T> {
    /**
     * The value.
     */
    var value: T
        get() = when (isSet()) {
            true -> sharedPreferences.getValueImpl()
            false -> defaultValue
        }
        set(value) = sharedPreferences.edit {
            value?.also { setValueImpl(it) } ?: remove(key)
        }

    /**
     * A [SharedFlow] of the value.
     */
    val valueFlow = sharedPreferences.preferenceFlow(key, getter = { value })
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    /**
     * Check whether or not this preference is set.
     */
    fun isSet() = sharedPreferences.contains(key)
}
