package bench

import kotlin.math.sqrt

data class Stats(
    val n: Int,
    val meanNs: Double,
    val stddevNs: Double,
    val minNs: Long,
    val p50Ns: Long,
    val p90Ns: Long,
    val p99Ns: Long,
    val maxNs: Long,
)

fun stats(samples: LongArray): Stats {
    val sorted = samples.sortedArray()
    val n = sorted.size
    val mean = sorted.average()
    var variance = 0.0
    for (v in sorted) {
        val d = v - mean
        variance += d * d
    }
    variance /= n
    val stddev = sqrt(variance)
    fun pct(p: Double) = sorted[(n * p).toInt().coerceIn(0, n - 1)]
    return Stats(
        n = n,
        meanNs = mean,
        stddevNs = stddev,
        minNs = sorted[0],
        p50Ns = pct(0.50),
        p90Ns = pct(0.90),
        p99Ns = pct(0.99),
        maxNs = sorted[n - 1],
    )
}

fun Stats.formatUs(): String {
    fun us(ns: Long) = "%.1f".format(ns / 1000.0)
    fun us(ns: Double) = "%.1f".format(ns / 1000.0)
    return "n=$n  mean=${us(meanNs)}us  sd=${us(stddevNs)}us  " +
            "min=${us(minNs)}us  p50=${us(p50Ns)}us  p90=${us(p90Ns)}us  " +
            "p99=${us(p99Ns)}us  max=${us(maxNs)}us"
}
