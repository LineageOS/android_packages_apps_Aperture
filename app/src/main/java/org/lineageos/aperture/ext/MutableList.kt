/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

/**
 * Returns the value for the given [index] if the value is present and not `null`.
 * Otherwise, calls the [defaultValue] function,
 * puts its result into the list under the given index and returns the call result.
 *
 * Note that the operation is not guaranteed to be atomic if the map is being modified concurrently.
 */
inline fun <E> MutableList<E>.getOrPut(index: Int, defaultValue: () -> E) =
    get(index) ?: defaultValue().also {
        set(index, it)
    }

inline fun <reified E : MutableList<ListE>, ListE> MutableList<E>.getOrCreate(index: Int) =
    getOrPut(index) {
        mutableListOf<ListE>() as E // idk why this is needed
    }

inline fun <reified E : MutableMap<MapK, MapV>, MapK, MapV> MutableList<E>.getOrCreate(index: Int) =
    getOrPut(index) {
        mutableMapOf<MapK, MapV>() as E // idk why this is needed
    }

inline fun <reified E : MutableSet<SetE>, SetE> MutableList<E>.getOrCreate(index: Int) =
    getOrPut(index) {
        mutableSetOf<SetE>() as E // idk why this is needed
    }
