package org.ccci.gto.android.common.db

import android.util.Pair
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import org.ccci.gto.android.common.db.AbstractDao.Companion.bindValues

class Query<T : Any> private constructor(
    query: Query<T>? = null,
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val table: Table<T> = query!!.table,
    internal val distinct: Boolean = query?.distinct ?: false,
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val joins: Array<Join<T, *>> = query?.joins ?: emptyArray(),
    projection: Array<String>? = query?.projection,
    @VisibleForTesting
    internal val where: Expression? = query?.where,
    internal val orderBy: String? = query?.orderBy,
    internal val groupBy: Array<Expression.Field> = query?.groupBy ?: emptyArray(),
    private val having: Expression? = query?.having,
    private val limit: Int? = query?.limit,
    private val offset: Int? = query?.offset
) {
    internal val projection = if (projection.isNullOrEmpty()) null else projection

    companion object {
        @JvmStatic
        fun <T : Any> select(type: Class<T>) = Query(table = Table.forClass(type))

        @JvmStatic
        fun <T : Any> select(table: Table<T>) = Query(table = table)

        @JvmStatic
        inline fun <reified T : Any> select() = select(T::class.java)
    }

    fun distinct(distinct: Boolean) = Query(this, distinct = distinct)
    fun join(vararg joins: Join<T, *>) = Query(this, joins = this.joins + joins)
    fun joins(vararg joins: Join<T, *>) = Query(this, joins = arrayOf(*joins))
    fun projection(vararg projection: String) = Query(this, projection = arrayOf(*projection))
    fun where(where: Expression?) = Query(this, where = where)
    fun where(where: String?, vararg args: Any) = where(where, *bindValues(*args))
    fun where(where: String?, vararg args: String) = where(if (where != null) Expression.raw(where, *args) else null)
    fun orderBy(orderBy: String?): Query<T> = Query(this, orderBy = orderBy)
    fun groupBy(vararg groupBy: Expression.Field) = Query(this, groupBy = arrayOf(*groupBy))
    fun having(having: Expression?) = Query(this, having = having)
    fun limit(limit: Int?) = Query(this, limit = limit)
    fun offset(offset: Int?) = Query(this, offset = offset)

    internal fun buildSqlFrom(dao: AbstractDao) = QueryComponent(table.sqlTable(dao)) +
        joins.fold(null as QueryComponent?) { sql, join -> sql + join.buildSql(dao) }

    fun buildSqlWhere(dao: AbstractDao): Pair<String?, Array<String>?> {
        return where?.buildSql(dao) ?: Pair.create<String?, Array<String>?>(null, null)
    }

    fun buildSqlHaving(dao: AbstractDao): Pair<String?, Array<String>?> {
        return having?.buildSql(dao) ?: Pair.create<String?, Array<String>?>(null, null)
    }

    internal val sqlLimit get() = when {
        // // XXX: not supported by Android
        // // "{limit} OFFSET {offset}" syntax
        // limit != null && offset != null -> "$limit OFFSET $offset"
        // "{offset},{limit}" syntax
        limit != null && offset != null -> "$offset, $limit"
        limit != null -> "$limit"
        else -> null
    }
}
