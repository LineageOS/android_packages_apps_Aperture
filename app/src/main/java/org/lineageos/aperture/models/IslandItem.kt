/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

/**
 * Island item.
 */
sealed interface IslandItem : UniqueItem<IslandItem> {
    /**
     * The device is in thermal throttling.
     *
     * @param isCritical Whether the device is in critical thermal throttling
     */
    data class ThermalThrottling(
        val isCritical: Boolean,
    ) : IslandItem

    /**
     * The device is low on battery.
     *
     * @param isCharging Whether the device is currently charging
     */
    data class LowBattery(
        val isCharging: Boolean,
    ) : IslandItem

    /**
     * JPEG Ultra HDR is currently being used. Only shown in photo camera mode.
     */
    data object PhotoJpegUltraHdr : IslandItem

    /**
     * RAW is currently being used. Only shown in photo camera mode.
     *
     * @param withJpeg Whether a JPEG image will also be stored on capture
     */
    data class PhotoRawEnabled(
        val withJpeg: Boolean,
    ) : IslandItem

    /**
     * The video mic is muted. Only shown in video camera mode.
     */
    data object VideoMicMuted : IslandItem

    override fun areItemsTheSame(other: IslandItem) = this::class.isInstance(other)
    override fun areContentsTheSame(other: IslandItem) = this == other
}
