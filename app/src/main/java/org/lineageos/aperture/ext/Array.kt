/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

/**
 * Get the next element in the array relative to the [current] element.
 *
 * If the element is the last in the array or it's not present in the array
 * it will return the first element.
 * If the array is empty, null will be returned.
 *
 * @param current The element to use as cursor
 *
 * @return [T] Either the next element, the first element or null
 */
fun <T> Array<T>.next(current: T) = getOrElse(indexOf(current) + 1) { firstOrNull() }

/**
 * Get the previous element in the array relative to the [current] element.
 *
 * If the element is the first in the array or it's not present in the array
 * it will return the last element.
 * If the array is empty, null will be returned.
 *
 * @param current The element to use as cursor
 *
 * @return [T] Either the previous element, the last element or null
 */
fun <T> Array<T>.previous(current: T) = getOrElse(indexOf(current) - 1) { lastOrNull() }
