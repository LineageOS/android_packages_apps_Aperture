/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

/**
 * Get the next element in the list relative to the [current] element.
 *
 * If the element is the last in the list or it's not present in the list
 * it will return the first element.
 * If the list is empty, null will be returned.
 *
 * @param current The element to use as cursor
 *
 * @return [E] Either the next element, the first element or null
 */
fun <E> List<E>.next(current: E) = getOrElse(indexOf(current) + 1) { firstOrNull() }

/**
 * Get the previous element in the list relative to the [current] element.
 *
 * If the element is the first in the list or it's not present in the list
 * it will return the last element.
 * If the list is empty, null will be returned.
 *
 * @param current The element to use as cursor
 *
 * @return [E] Either the previous element, the last element or null
 */
fun <E> List<E>.previous(current: E) = getOrElse(indexOf(current) - 1) { lastOrNull() }
