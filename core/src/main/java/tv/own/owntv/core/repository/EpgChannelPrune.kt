package tv.own.owntv.core.repository

/**
 * Poterx: which of the guide ids a download did not list are dropped from the store, and which are
 * kept because the download looks like a part of the feed rather than the whole of it.
 *
 * After a download that parsed to its end, every stored id the feed did not list is removed with
 * its programmes: that is how a channel the provider dropped leaves the guide. It is also how a
 * download that was only *part* of the feed empties the guide. A document that stops early but
 * ends cleanly — a generator cut off mid-output, a file read while it was being rewritten, an
 * error page sent with "200 OK" — reads as a finished one, and everything it did not reach was
 * deleted on the spot. No other step of a refresh can take every programme of a channel, so this
 * is the reading of what was seen on 12 Nov 2026: a guide of 346 channels left with 162 by one
 * night-time refresh that reported success, and whole again after the next one. The download
 * itself was not captured.
 *
 * So ids are only dropped in bulk when there is nothing to lose. An id counts as *held* when the
 * download neither listed it nor carried a programme for it, yet the store still has a programme of
 * its that is to come. A handful of held ids is a provider dropping channels and goes through as
 * before. Many at once — at least [THIN_FEED_MIN_CHANNELS], and at least [THIN_FEED_SHARE_PCT] per
 * cent of all ids with a programme to come — is a thin download: the held ids stay, and the caller
 * treats the refresh as cut short, so it is tried again soon. Ids kept this way are dropped by a
 * later refresh once none of their programmes is still to come, so a channel that is really gone
 * leaves within the guide's own horizon.
 */
internal data class ChannelPrunePlan(val drop: List<String>, val keep: List<String>)

/** Fewer held ids than this are never a thin download, however small the guide. */
internal const val THIN_FEED_MIN_CHANNELS = 5

/** Share (per cent) of the ids with a programme to come that must be held for a download to count as thin. */
internal const val THIN_FEED_SHARE_PCT = 10

/**
 * @param missing stored guide ids the download did not list (all of them when it listed none)
 * @param upcoming stored guide ids with a programme that has not ended yet
 * @param fed guide ids the download carried a programme for, listed or not
 */
internal fun channelPrunePlan(
    missing: Collection<String>,
    upcoming: Set<String>,
    fed: Set<String>,
): ChannelPrunePlan {
    val held = missing.filter { it in upcoming && it !in fed }
    val thin = held.size >= THIN_FEED_MIN_CHANNELS &&
        held.size * 100L >= upcoming.size.toLong() * THIN_FEED_SHARE_PCT
    if (!thin) return ChannelPrunePlan(drop = missing.toList(), keep = emptyList())
    val kept = held.toHashSet()
    return ChannelPrunePlan(drop = missing.filterNot(kept::contains), keep = held)
}
