/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.query

typealias Column = String

sealed interface Node {
    fun build(): String = when (this) {
        is Eq -> "${lhs.build()} = ${rhs.build()}"
        is Or -> "(${lhs.build()}) OR (${rhs.build()})"
        is And -> "(${lhs.build()}) AND (${rhs.build()})"
        is Literal<*> -> "$`val`"
        is In<*> -> "$value IN (${values.joinToString(", ")})"
        is Not -> "NOT (${node.build()})"
    }
}

private class Eq(val lhs: Node, val rhs: Node) : Node
private class Or(val lhs: Node, val rhs: Node) : Node
private class And(val lhs: Node, val rhs: Node) : Node
private class Literal<T>(val `val`: T) : Node
private class In<T>(val value: T, val values: Collection<T>) : Node
private class Not(val node: Node) : Node

class Query(val root: Node) {
    fun build() = root.build()

    companion object {
        const val ARG = "?"
    }
}

infix fun Query.or(other: Query) = Query(Or(this.root, other.root))
infix fun Query.and(other: Query) = Query(And(this.root, other.root))
infix fun Query.eq(other: Query) = Query(Eq(this.root, other.root))
infix fun <T> Column.eq(other: T) = Query(Literal(this)) eq Query(Literal(other))
infix fun <T> Column.`in`(values: Collection<T>) = Query(In(this, values))
operator fun Query.not() = Query(Not(root))

fun Iterable<Query>.join(
    func: Query.(other: Query) -> Query,
) = reduceOrNull(func)
