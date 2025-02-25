/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.lineageos.aperture.ApertureApplication

/**
 * Base view model for all app view models.
 */
abstract class ApertureViewModel(application: Application) : AndroidViewModel(application) {
    private val apertureApplication = getApplication<ApertureApplication>()

    protected val cameraRepository = apertureApplication.cameraRepository
    protected val mediaRepository = apertureApplication.mediaRepository
    protected val overlaysRepository = apertureApplication.overlaysRepository
    protected val preferencesRepository = apertureApplication.preferencesRepository

    @Suppress("EmptyMethod")
    final override fun <T : Application> getApplication() = super.getApplication<T>()
}
