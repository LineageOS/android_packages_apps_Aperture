/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Application
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.CameraIdUtil
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.MainScope
import org.lineageos.aperture.repositories.CameraRepository
import org.lineageos.aperture.repositories.MediaRepository
import org.lineageos.aperture.repositories.OverlaysRepository
import org.lineageos.aperture.repositories.PreferencesRepository

class ApertureApplication : Application() {
    private val coroutineScope = MainScope()

    private val appOpsManager by lazy { getSystemService(AppOpsManager::class.java) }

    @delegate:RequiresApi(Build.VERSION_CODES.R)
    private val appOpsCallback by lazy {
        object : AppOpsManager.OnOpNotedCallback() {
            private fun logPrivateDataAccess(
                opCode: String,
                attributionTag: String?,
                message: String?,
                throwable: Throwable?,
            ) {
                Log.i(
                    LOG_TAG,
                    "Private data accessed. " +
                            "Operation: $opCode\n" +
                            "Attribution tag: $attributionTag\n" +
                            "Message: $message",
                    throwable,
                )
            }

            override fun onNoted(syncNotedAppOp: SyncNotedAppOp) {
                logPrivateDataAccess(
                    syncNotedAppOp.op,
                    syncNotedAppOp.attributionTag,
                    null,
                    Throwable(),
                )
            }

            override fun onSelfNoted(syncNotedAppOp: SyncNotedAppOp) {
                logPrivateDataAccess(
                    syncNotedAppOp.op,
                    syncNotedAppOp.attributionTag,
                    null,
                    Throwable(),
                )
            }

            override fun onAsyncNoted(asyncNotedAppOp: AsyncNotedAppOp) {
                logPrivateDataAccess(
                    asyncNotedAppOp.op,
                    asyncNotedAppOp.attributionTag,
                    asyncNotedAppOp.message,
                    null,
                )
            }
        }
    }

    val cameraRepository by lazy { CameraRepository(this, coroutineScope, overlaysRepository) }
    val mediaRepository by lazy { MediaRepository(this) }
    val overlaysRepository by lazy { OverlaysRepository(this) }
    val preferencesRepository by lazy { PreferencesRepository(this, coroutineScope) }

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Set backward compatible camera ids
        CameraIdUtil.setBackwardCompatibleCameraIds(
            overlaysRepository.backwardCompatibleCameraIds.asList()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appOpsManager.setOnOpNotedCallback(mainExecutor, appOpsCallback)
        }
    }

    companion object {
        private val LOG_TAG = ApertureApplication::class.simpleName!!
    }
}
