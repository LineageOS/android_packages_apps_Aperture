/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils.livedatadelegate

import androidx.lifecycle.MutableLiveData
import kotlin.reflect.KProperty

class NonNullableLiveDataDelegate<T>(
    initializer: () -> MutableLiveData<T>
) : LiveDataDelegate<T>(initializer) {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value.value!!
}
