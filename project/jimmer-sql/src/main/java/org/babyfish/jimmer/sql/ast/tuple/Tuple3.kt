package org.babyfish.jimmer.sql.ast.tuple

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor
import java.util.function.BiFunction

data class Tuple3<T1, T2, T3>(
    val _1: T1,
    val _2: T2,
    val _3: T3
) : TupleImplementor {

    override fun size(): Int = 3

    override operator fun get(index: Int): Any? =
        when (index) {
            0 -> _1
            1 -> _2
            2 -> _3
            else -> throw IllegalArgumentException("Index must between 0 and ${size() - 1}")
        }

    override fun convert(block: BiFunction<Any?, Int, Any?>): TupleImplementor =
        Tuple3(
            block.apply(_1, 0),
            block.apply(_2, 1),
            block.apply(_3, 2)
        )

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> projection1(tuples: Collection<Tuple3<T, *, *>>) : Collection<T> =
            TupleImplementor.projection(tuples, 0) as Collection<T>

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> projection2(tuples: Collection<Tuple3<*, T, *>>) : Collection<T> =
            TupleImplementor.projection(tuples, 1) as Collection<T>

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> projection3(tuples: Collection<Tuple3<*, *, T>>) : Collection<T> =
            TupleImplementor.projection(tuples, 2) as Collection<T>
    }
}