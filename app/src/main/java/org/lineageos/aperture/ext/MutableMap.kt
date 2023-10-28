/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

inline fun <K, reified V : MutableList<ListE>, ListE> MutableMap<K, V>.getOrCreate(key: K) =
    getOrPut(key) {
        mutableListOf<ListE>() as V // idk why this is needed
    }

inline fun <K, reified V : MutableMap<MapK, MapV>, MapK, MapV> MutableMap<K, V>.getOrCreate(
    key: K
) = getOrPut(key) {
    mutableMapOf<MapK, MapV>() as V // idk why this is needed
}

inline fun <K, reified V : MutableSet<SetE>, SetE> MutableMap<K, V>.getOrCreate(key: K) =
    getOrPut(key) {
        mutableSetOf<SetE>() as V // idk why this is needed
    }
