/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import androidx.annotation.Px
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import kotlin.reflect.safeCast

fun AppBarLayout.setOffset(@Px offsetPx: Int, coordinatorLayout: CoordinatorLayout) {
    val params = CoordinatorLayout.LayoutParams::class.safeCast(layoutParams) ?: return
    AppBarLayout.Behavior::class.safeCast(params.behavior)?.onNestedPreScroll(
        coordinatorLayout,
        this,
        this,
        0,
        offsetPx,
        intArrayOf(0, 0),
        0
    )
}
