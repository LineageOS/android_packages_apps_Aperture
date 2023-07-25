/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.lineageos.aperture.utils.CameraSoundsUtils
import org.lineageos.aperture.utils.PermissionsUtils

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, RootSettingsFragment())
                .commit()
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    abstract class SettingsFragment : PreferenceFragmentCompat() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            setDivider(ColorDrawable(Color.TRANSPARENT))
            setDividerHeight(0)

            @SuppressLint("InternalInsetResource", "DiscouragedApi")
            var navigationBarHeight = requireContext().resources
                .getIdentifier("navigation_bar_height", "dimen", "android")

            if (navigationBarHeight > 0) {
                navigationBarHeight =
                    requireContext().resources.getDimensionPixelSize(navigationBarHeight)
            }

            view.setPaddingRelative(
                view.paddingStart,
                view.paddingTop,
                view.paddingEnd,
                view.paddingBottom + navigationBarHeight
            )
        }
    }

    class RootSettingsFragment : SettingsFragment() {
        private val enableZsl by lazy { findPreference<SwitchPreference>("enable_zsl")!! }
        private val photoCaptureMode by lazy {
            findPreference<ListPreference>("photo_capture_mode")!!
        }
        private val saveLocation by lazy { findPreference<SwitchPreference>("save_location") }
        private val shutterSound by lazy { findPreference<SwitchPreference>("shutter_sound") }

        private val permissionsUtils by lazy { PermissionsUtils(requireContext()) }

        private val requestLocationPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (!permissionsUtils.locationPermissionsGranted()) {
                saveLocation?.isChecked = false
                Toast.makeText(
                    requireContext(), getString(R.string.save_location_toast), Toast.LENGTH_SHORT
                ).show()
            }
        }

        private val photoCaptureModePreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val currentPhotoCaptureMode = if (preference == photoCaptureMode) {
                    newValue as String
                } else {
                    photoCaptureMode.value
                }

                val enableZslCanBeEnabled = currentPhotoCaptureMode == "minimize_latency"
                enableZsl.isChecked = enableZsl.isChecked && enableZslCanBeEnabled
                enableZsl.isEnabled = enableZslCanBeEnabled

                true
            }

        @SuppressLint("UnsafeOptInUsageError")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            saveLocation?.let {
                // Reset location back to off if permissions aren't granted
                it.isChecked = it.isChecked && permissionsUtils.locationPermissionsGranted()
                it.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean) {
                            requestLocationPermissions.launch(PermissionsUtils.locationPermissions)
                        }
                        true
                    }
            }
            shutterSound?.isVisible = !CameraSoundsUtils.mustPlaySounds

            // Photo capture mode
            photoCaptureMode.onPreferenceChangeListener = photoCaptureModePreferenceChangeListener
            enableZsl.isEnabled = photoCaptureMode.value == "minimize_latency"
        }
    }

    class ProcessingSettingsFragment : SettingsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.processing_preferences, rootKey)
        }
    }
}
