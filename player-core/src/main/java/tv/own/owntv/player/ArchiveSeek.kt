package tv.own.owntv.player

/**
 * Poterx: a D-pad step back in a catch-up archive stays inside what the player holds in memory.
 *
 * An archive from an Xtream panel is one progressive `.ts` whose address names its start to the
 * minute. It cannot be opened at an offset: a request for bytes further in is answered from a minute
 * boundary. ExoPlayer keeps nothing behind the playback position by default, so "back 10 s" left the
 * buffer, turned into such a request and landed at the start of the NEXT minute (0:19 → 0:59,
 * 0:43 → 1:00, 1:16 → 2:00, measured on a Shield with 0.4.5.5). mpv, which played archives until
 * they moved to ExoPlayer, served the same step from its back cache.
 *
 * Two parts. An archive item is built with a back buffer ([backBufferMs], see the load control in
 * [ExoSubtitleEngine]), so a step back is served from memory. And [clamp] keeps a step back inside
 * the span that is held: from where that span starts, or the back buffer behind the furthest
 * position played in it. A step that asks for more goes as far back as memory reaches and no
 * further: it never becomes a request the panel answers from somewhere else. A step forward is left
 * alone; past the buffer it still lands on a minute boundary ahead, which is at least forward.
 *
 * The back buffer is kept by time while the load control stops at a byte target, and the two share
 * one allocator: a back buffer that fills the target leaves nothing to load ahead with, and playback
 * stops. So the target grows by [BACK_BUFFER_BYTES] as far as the heap allows, and the back buffer
 * is only kept when that leaves at least [MIN_TARGET_BYTES]: a stream then has to pass about
 * 17 Mbit/s before [BACK_BUFFER_MS] of it fills the target, well above an HD channel. Where the
 * heap has no room for that, the archive is built without a back buffer, as before, and a step
 * back does nothing rather than jump.
 */
object ArchiveSeek {

    /** How far behind the playback position an archive item's buffer is kept, when the heap allows. */
    const val BACK_BUFFER_MS = 20_000

    /** Kept clear of the start of the span: the seek lands on a keyframe, which is not the edge itself. */
    const val MARGIN_MS = 3_000L

    /** Bytes the back buffer may add to the film buffer's own target (20 s at about 9.6 Mbit/s). */
    const val BACK_BUFFER_BYTES = 24L * 1024 * 1024

    /** The smallest byte target a back buffer is kept with; under it the archive is built as a film is. */
    const val MIN_TARGET_BYTES = 40L * 1024 * 1024

    /** The film target grown by the back buffer's room, within [heapCapBytes] (0 = unknown) and never below the film's own. */
    private fun grown(filmTargetBytes: Int, heapCapBytes: Long): Long {
        val wanted = filmTargetBytes.toLong() + BACK_BUFFER_BYTES
        return if (heapCapBytes > 0L) wanted.coerceAtMost(maxOf(heapCapBytes, filmTargetBytes.toLong())) else wanted
    }

    /** The back buffer an archive item is built with: [BACK_BUFFER_MS], or none where the heap leaves too little room. */
    fun backBufferMs(filmTargetBytes: Int, heapCapBytes: Long): Int =
        if (grown(filmTargetBytes, heapCapBytes) >= MIN_TARGET_BYTES) BACK_BUFFER_MS else 0

    /** The byte target of an archive item: the grown one when it keeps a back buffer, else the film buffer's own. */
    fun targetBytes(filmTargetBytes: Int, heapCapBytes: Long): Int {
        val grown = grown(filmTargetBytes, heapCapBytes)
        return if (grown >= MIN_TARGET_BYTES) grown.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else filmTargetBytes
    }

    /**
     * [targetMs] for a step from [positionMs]: a step back kept inside the span held in memory, a step
     * forward as asked.
     *
     * @param spanStartMs where the span in memory begins (the item's start, or where a jump landed)
     * @param furthestMs the furthest position played in this span
     * @param backBufferMs the back buffer the player was built with (0 = none: a step back stays put)
     */
    fun clamp(targetMs: Long, positionMs: Long, spanStartMs: Long, furthestMs: Long, backBufferMs: Int): Long {
        if (targetMs >= positionMs) return targetMs
        val held = maxOf(furthestMs, positionMs) - backBufferMs + MARGIN_MS
        val floor = maxOf(spanStartMs, held).coerceIn(0L, positionMs.coerceAtLeast(0L))
        return targetMs.coerceAtLeast(floor)
    }
}
