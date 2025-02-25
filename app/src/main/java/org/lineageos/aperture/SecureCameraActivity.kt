/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

// Use a different activity for secure camera only. So it can have a different
// task affinity from others. This makes sure non-secure camera activity is not
// started in secure lock screen.
class SecureCameraActivity : CameraActivity()
