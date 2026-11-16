package tv.own.owntv.player

/**
 * Poterx: a D-pad seek in a catch-up archive stays inside what the player holds in memory.
 *
 * An archive from an Xtream panel is one progressive `.ts` whose address names its start to the
 * minute. It cannot be opened at an offset: a request for bytes further in is answered from a minute
 * boundary. ExoPlayer keeps nothing behind the playback position by default, so "back 10 s" left the
 * buffer, turned into such a request and landed at the start of the NEXT minute (0:19 → 0:59,
 * 0:43 → 1:00, 1:16 → 2:00, measured on a Shield with 0.4.5.5). mpv, which played archives until
 * they moved to ExoPlayer, served the same step from its back cache.
 *
 * Two parts. An archive item is built with a back buffer of [BACK_BUFFER_MS] (see the load control in
 * [ExoSubtitleEngine]), so a step back is served from memory. And [clamp] keeps a step inside the
 * span that is held: from where that span starts, or [BACK_BUFFER_MS] behind the furthest position
 * played in it, up to what is buffered ahead. A step that asks for more goes as far as memory
 * reaches and no further: it never becomes a request the panel cannot answer.
 */
object ArchiveSeek {

    /** How far behind the playback position an archive item's buffer is kept. */
    const val BACK_BUFFER_MS = 30_000

    /** Kept clear of both ends of the span: the seek lands on a keyframe, which is not the edge itself. */
    const val MARGIN_MS = 3_000L

    /** Bytes the back buffer may take on top of the film buffer's own target (30 s at about 6.4 Mbit/s). */
    const val BACK_BUFFER_BYTES = 24L * 1024 * 1024

    /**
     * [targetMs] kept inside the span held in memory.
     *
     * @param positionMs where playback is now
     * @param spanStartMs where the span in memory begins (the item's start, or where an absolute seek landed)
     * @param furthestMs the furthest position played in this span
     * @param bufferedMs how far ahead is buffered
     */
    fun clamp(targetMs: Long, positionMs: Long, spanStartMs: Long, furthestMs: Long, bufferedMs: Long): Long {
        val floor = maxOf(spanStartMs, maxOf(furthestMs, positionMs) - BACK_BUFFER_MS + MARGIN_MS).coerceAtMost(positionMs)
        val ceiling = (bufferedMs - MARGIN_MS).coerceAtLeast(positionMs)
        return targetMs.coerceIn(floor.coerceAtLeast(0L), ceiling)
    }

    /**
     * The byte target of an archive item: the film buffer's own ([filmTargetBytes]) plus room for the
     * back buffer, within [heapCapBytes] (0 when unknown) and never below the film buffer's.
     */
    fun targetBytes(filmTargetBytes: Int, heapCapBytes: Long): Int {
        val wanted = filmTargetBytes.toLong() + BACK_BUFFER_BYTES
        val capped = if (heapCapBytes > 0L) wanted.coerceAtMost(maxOf(heapCapBytes, filmTargetBytes.toLong())) else wanted
        return capped.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
