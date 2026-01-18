/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

/**
 * An item that can be uniquely identified.
 */
interface UniqueItem<T> {
    /**
     * Return whether this item is the same as the other.
     */
    fun areItemsTheSame(other: T): Boolean

    /**
     * Return whether this item has the same content as the other.
     * This is called only when [UniqueItem.areItemsTheSame] returns true.
     */
    fun areContentsTheSame(other: T): Boolean
}
