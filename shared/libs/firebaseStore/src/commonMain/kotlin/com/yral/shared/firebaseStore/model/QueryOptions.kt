package com.yral.shared.firebaseStore.model

data class QueryOptions(
    val limit: Long? = null,
    val orderBy: OrderBy? = null,
    val filters: List<Filter> = emptyList(),
    val startAfter: String? = null,
) {
    sealed class Filter {
        data class Equals(
            val field: String,
            val value: Any?,
        ) : Filter()
        data class GreaterThan(
            val field: String,
            val value: Any,
        ) : Filter()
        data class LessThan(
            val field: String,
            val value: Any,
        ) : Filter()
        data class Contains(
            val field: String,
            val value: Any,
        ) : Filter()
        data class In(
            val field: String,
            val values: List<Any>,
        ) : Filter()
    }

    data class OrderBy(
        val field: String,
        val direction: Direction = Direction.ASCENDING,
    ) {
        enum class Direction {
            ASCENDING,
            DESCENDING,
        }
    }
}
