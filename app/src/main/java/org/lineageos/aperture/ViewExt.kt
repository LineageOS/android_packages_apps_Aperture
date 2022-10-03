/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.view.View

internal fun View.setPadding(value: Int) {
    setPadding(value, value, value, value)
}
