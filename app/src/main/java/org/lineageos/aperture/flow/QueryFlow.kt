/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.flow

import android.database.Cursor
import kotlinx.coroutines.flow.Flow

abstract class QueryFlow<T> {
    /**
     * A flow of the data specified by the query
     */
    abstract fun flowData(): Flow<List<T>>

    /**
     * A flow of the cursor specified by the query
     */
    abstract fun flowCursor(): Flow<Cursor?>
}
