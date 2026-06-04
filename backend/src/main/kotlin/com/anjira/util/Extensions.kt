package com.anjira.util

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Suppress("UNCHECKED_CAST")
infix fun <T : Comparable<T>> Column<EntityID<T>>.eqId(value: T): Op<Boolean> =
    eq(EntityID(value, table as IdTable<T>))
