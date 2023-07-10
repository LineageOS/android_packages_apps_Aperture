/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlin.reflect.KProperty

/**
 * Set the value immediately if we're in the main thread, else it will post it to be set later.
 */
fun <T> MutableLiveData<T>.setOrPostValue(value: T) {
    if (Looper.getMainLooper().isCurrentThread) {
        this.value = value
    } else {
        postValue(value)
    }
}

class LiveDataDelegate<T>(private val liveData: MutableLiveData<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = liveData.value!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        liveData.setOrPostValue(value)
    }
}

fun <T> MutableLiveData<T>.asPropertyDelegate() = LiveDataDelegate(this)
