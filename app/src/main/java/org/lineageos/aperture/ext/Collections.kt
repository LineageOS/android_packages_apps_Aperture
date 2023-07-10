/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

/**
 * Get the next item in the array.
 * If the element is the last of the list or it's not present in the list
 * it will return the first item.
 */
internal fun <T> Array<T>.next(current: T): T {
    val currentIndex = indexOf(current)
    return getOrElse(currentIndex + 1) { first() }
}

/**
 * Get the previous item in the array.
 * If the element is the first of the list or it's not present in the list
 * it will return the last item.
 */
internal fun <T> Array<T>.previous(current: T): T {
    val currentIndex = indexOf(current)
    return getOrElse(currentIndex - 1) { last() }
}

/**
 * Get the next item in the list.
 * If the element is the last of the list or it's not present in the list
 * it will return the first item.
 */
internal fun <T> List<T>.next(current: T): T {
    val currentIndex = indexOf(current)
    return getOrElse(currentIndex + 1) { first() }
}

/**
 * Get the previous item in the list.
 * If the element is the first of the list or it's not present in the list
 * it will return the last item.
 */
internal fun <T> List<T>.previous(current: T): T {
    val currentIndex = indexOf(current)
    return getOrElse(currentIndex - 1) { last() }
}
