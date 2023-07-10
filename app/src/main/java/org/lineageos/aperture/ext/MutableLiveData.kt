/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlin.reflect.KProperty

class LiveDataDelegate<T>(private val liveData: MutableLiveData<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = liveData.value!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (Looper.getMainLooper().isCurrentThread) {
            this.liveData.value = value
        } else {
            this.liveData.postValue(value)
        }
    }
}

fun <T> MutableLiveData<T>.asPropertyDelegate() = LiveDataDelegate(this)
