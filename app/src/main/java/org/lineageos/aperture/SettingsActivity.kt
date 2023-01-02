/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
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
                .replace(R.id.settings, SettingsFragment())
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
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val saveLocation by lazy { findPreference<SwitchPreference>("save_location") }
        private val shutterSound by lazy { findPreference<SwitchPreference>("shutter_sound") }
        private val videoStabilization by lazy {
            findPreference<SwitchPreference>("video_stabilization")!!
        }
        private val videoStabilizationPreview by lazy {
            findPreference<SwitchPreference>("video_stabilization_preview")!!
        }
        private val videoStabilizationOis by lazy {
            findPreference<SwitchPreference>("video_stabilization_ois")!!
        }

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

        private val videoStabilizationPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val videoStabilizationEnabled =
                    if (preference == videoStabilization) {
                        newValue as Boolean
                    } else {
                        videoStabilization.isChecked
                    }
                val videoStabilizationPreviewEnabled =
                    if (preference == videoStabilizationPreview) {
                        newValue as Boolean
                    } else {
                        videoStabilizationPreview.isChecked
                    }
                val videoStabilizationOisEnabled =
                    if (preference == videoStabilizationOis) {
                        newValue as Boolean
                    } else {
                        videoStabilizationOis.isChecked
                    }

                val videoStabilizationPreviewCanBeEnabled =
                    videoStabilizationEnabled && !videoStabilizationOisEnabled
                videoStabilizationPreview.isChecked =
                    videoStabilizationPreview.isChecked && videoStabilizationPreviewCanBeEnabled
                videoStabilizationPreview.isEnabled = videoStabilizationPreviewCanBeEnabled

                val videoStabilizationOisCanBeEnabled =
                    videoStabilizationEnabled && !videoStabilizationPreviewEnabled
                videoStabilizationOis.isChecked =
                    videoStabilizationOis.isChecked && videoStabilizationOisCanBeEnabled
                videoStabilizationOis.isEnabled = videoStabilizationOisCanBeEnabled

                true
            }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            setDivider(ColorDrawable(Color.TRANSPARENT))
            setDividerHeight(0)
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

            // Video stabilization
            videoStabilization.onPreferenceChangeListener =
                videoStabilizationPreferenceChangeListener
            videoStabilizationPreview.onPreferenceChangeListener =
                videoStabilizationPreferenceChangeListener
            videoStabilizationOis.onPreferenceChangeListener =
                videoStabilizationPreferenceChangeListener

            videoStabilizationPreview.isEnabled =
                videoStabilization.isChecked && !videoStabilizationOis.isChecked
            videoStabilizationOis.isEnabled =
                videoStabilization.isChecked && !videoStabilizationPreview.isChecked
        }
    }
}
