/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repository

import android.content.Context
import org.lineageos.aperture.flow.CapturedMediaFlow

object MediaRepository {
    fun capturedMedia(
        context: Context,
    ) = CapturedMediaFlow(context).flowData()
}
