/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

/**
 * Get the previous value.
 */
internal inline fun <reified E : Enum<E>> E.previous() = enumValues<E>().previous(this)

/**
 * Get the next value.
 */
internal inline fun <reified E : Enum<E>> E.next() = enumValues<E>().next(this)
