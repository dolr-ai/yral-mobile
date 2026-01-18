package com.yral.shared.libs.videoplayback

data class PreloadWindow(
    val prepared: Set<Int>,
    val disk: Set<Int>,
) {
    val all: Set<Int> = prepared + disk
}

fun computePreloadWindow(
    centerIndex: Int,
    itemCount: Int,
    policy: PreloadPolicy,
): PreloadWindow {
    if (itemCount <= 0) return PreloadWindow(emptySet(), emptySet())

    val prepared = mutableSetOf<Int>()
    if (policy.usePreparedNextPlayer) {
        for (offset in 1..policy.preparedPrev) {
            val index = centerIndex - offset
            if (index in 0 until itemCount) prepared.add(index)
        }
        for (offset in 1..policy.preparedNext) {
            val index = centerIndex + offset
            if (index in 0 until itemCount) prepared.add(index)
        }
    }

    val disk = mutableSetOf<Int>()
    for (offset in 1..policy.diskPrefetchNext) {
        val index = centerIndex + offset
        if (index in 0 until itemCount && index !in prepared) {
            disk.add(index)
        }
    }

    return PreloadWindow(prepared = prepared, disk = disk)
}
