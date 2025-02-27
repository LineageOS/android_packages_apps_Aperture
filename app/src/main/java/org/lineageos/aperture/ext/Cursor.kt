/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.database.Cursor

fun <T> Cursor?.mapEachRow(
    projection: Array<String>,
    mapping: (Cursor, Array<Int>) -> T,
) = this?.use { cursor ->
    if (!cursor.moveToFirst()) {
        return@use emptyList<T>()
    }

    val indexCache = projection.map { column ->
        cursor.getColumnIndexOrThrow(column)
    }.toTypedArray()

    val data = buildList {
        do {
            add(mapping(cursor, indexCache))
        } while (cursor.moveToNext())
    }

    data.toList()
} ?: emptyList()
