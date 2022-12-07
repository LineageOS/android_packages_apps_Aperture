/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.graphics.Rect
import android.util.Size

fun Rect.toSize() = Size(width(), height())
