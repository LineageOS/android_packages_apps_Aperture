/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.KeyCharacterMap
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.Px
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.lineageos.aperture.ext.setOffset
import org.lineageos.aperture.models.HardwareKey
import org.lineageos.aperture.utils.CameraSoundsUtils
import org.lineageos.aperture.utils.PermissionsUtils
import kotlin.reflect.safeCast

class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {
    private val appBarLayout by lazy { findViewById<AppBarLayout>(R.id.appBarLayout) }
    private val coordinatorLayout by lazy { findViewById<CoordinatorLayout>(R.id.coordinatorLayout) }
    private val toolbar by lazy { findViewById<MaterialToolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, RootSettingsFragment())
                .commit()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
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

    abstract class SettingsFragment(
        @XmlRes private val preferencesResId: Int,
    ) : PreferenceFragmentCompat() {
        private val settingsActivity
            get() = SettingsActivity::class.safeCast(activity)

        @Px
        private var appBarOffset = -1

        private val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, i ->
            appBarOffset = -i
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            settingsActivity?.let { settingsActivity ->
                val appBarLayout = settingsActivity.appBarLayout

                if (appBarOffset != -1) {
                    appBarLayout.setOffset(appBarOffset, settingsActivity.coordinatorLayout)
                } else {
                    appBarLayout.setExpanded(true, false)
                }

                appBarLayout.setLiftOnScrollTargetView(listView)

                appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
            }
        }

        override fun onDestroyView() {
            settingsActivity?.appBarLayout?.apply {
                removeOnOffsetChangedListener(offsetChangedListener)

                setLiftOnScrollTargetView(null)
            }

            super.onDestroyView()
        }

        @CallSuper
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(preferencesResId, rootKey)
        }

        @CallSuper
        override fun onCreateRecyclerView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            savedInstanceState: Bundle?
        ) = super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            clipToPadding = false
            isVerticalScrollBarEnabled = false

            ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                updatePadding(
                    bottom = insets.bottom,
                    left = insets.left,
                    right = insets.right,
                )

                windowInsets
            }
        }
    }

    class RootSettingsFragment : SettingsFragment(R.xml.root_preferences) {
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

        @Suppress("UnsafeOptInUsageError")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

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

    class GesturesSettingsFragment : SettingsFragment(R.xml.gestures_preferences) {
        // Preferences
        private val singleButtonsPreferenceCategory by lazy {
            findPreference<PreferenceCategory>("single_buttons")
        }

        // Input device listener
        private val inputDeviceListener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                recheckKeys()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                recheckKeys()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                recheckKeys()
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)

            recheckKeys()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val inputManager = requireContext().getSystemService(InputManager::class.java)

            recheckKeys()

            inputManager.registerInputDeviceListener(inputDeviceListener, null)
        }

        override fun onDestroyView() {
            val inputManager = requireContext().getSystemService(InputManager::class.java)

            inputManager.unregisterInputDeviceListener(inputDeviceListener)

            super.onDestroyView()
        }

        private fun recheckKeys() {
            var singleKeysPresent = false

            for (hardwareKey in HardwareKey.entries) {
                val present = KeyCharacterMap.deviceHasKeys(
                    mutableListOf(hardwareKey.firstKeycode).apply {
                        hardwareKey.secondKeycode?.let {
                            add(it)
                        }
                    }.toIntArray()
                ).all { it }

                if (hardwareKey.isTwoWayKey) {
                    val keyCategory = findPreference<PreferenceCategory>(
                        hardwareKey.sharedPreferencesKeyPrefix
                    )

                    keyCategory?.isVisible = present
                } else {
                    val actionPreference = findPreference<ListPreference>(
                        "${hardwareKey.sharedPreferencesKeyPrefix}_action"
                    )

                    actionPreference?.isVisible = present

                    singleKeysPresent = singleKeysPresent || present
                }
            }

            singleButtonsPreferenceCategory?.isVisible = singleKeysPresent
        }
    }

    class ProcessingSettingsFragment : SettingsFragment(R.xml.processing_preferences)
}
