package io.tl.nekopanel.model

data class TrafficSample(
    val up: Long,
    val down: Long,
    val upTotal: Long,
    val downTotal: Long,
    val upCumulative: Long,
    val downCumulative: Long,
    val hasTotals: Boolean,
)
