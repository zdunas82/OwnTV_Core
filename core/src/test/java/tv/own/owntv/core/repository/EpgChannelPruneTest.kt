package tv.own.owntv.core.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which guide ids a refresh drops when the download did not list them — and when it keeps them
 * because the download looks like a part of the feed.
 */
class EpgChannelPruneTest {

    private fun ids(range: IntRange) = range.map { "ch$it" }

    @Test
    fun `a few channels the provider dropped leave the guide`() {
        val stored = ids(1..346)
        val missing = ids(1..3)
        val plan = channelPrunePlan(missing, upcoming = stored.toSet(), fed = emptySet())
        assertEquals(missing, plan.drop)
        assertTrue(plan.keep.isEmpty())
    }

    @Test
    fun `a download that lists only part of the feed takes nothing with it`() {
        // The night of 12 Nov 2026: 346 channels stored, the download listed 162 of them.
        val stored = ids(1..346)
        val missing = ids(163..346)
        val plan = channelPrunePlan(missing, upcoming = stored.toSet(), fed = ids(1..162).toSet())
        assertTrue(plan.drop.isEmpty())
        assertEquals(missing, plan.keep)
    }

    @Test
    fun `an empty download does not wipe a guide that still has programmes to come`() {
        val stored = ids(1..40)
        val plan = channelPrunePlan(stored, upcoming = stored.toSet(), fed = emptySet())
        assertTrue(plan.drop.isEmpty())
        assertEquals(stored, plan.keep)
    }

    @Test
    fun `ids with nothing left to show go even from a thin download`() {
        val missing = ids(1..100)
        val upcoming = ids(1..60).toSet() // 61..100 have run out of programmes
        val plan = channelPrunePlan(missing, upcoming, fed = emptySet())
        assertEquals(ids(61..100), plan.drop)
        assertEquals(ids(1..60), plan.keep)
    }

    @Test
    fun `a guide that ran out of programmes is cleared as before`() {
        val stored = ids(1..200)
        val plan = channelPrunePlan(stored, upcoming = emptySet(), fed = emptySet())
        assertEquals(stored, plan.drop)
        assertTrue(plan.keep.isEmpty())
    }

    @Test
    fun `an id the download carried programmes for is not held`() {
        // Programmes without a channel element: the rows were just refreshed, only the listing goes.
        val missing = ids(1..50)
        val plan = channelPrunePlan(missing, upcoming = ids(1..300).toSet(), fed = ids(1..50).toSet())
        assertEquals(missing, plan.drop)
        assertTrue(plan.keep.isEmpty())
    }

    @Test
    fun `fewer than five held ids are never a thin download`() {
        val stored = ids(1..6)
        val missing = ids(1..4)
        val plan = channelPrunePlan(missing, upcoming = stored.toSet(), fed = ids(5..6).toSet())
        assertEquals(missing, plan.drop)
        assertTrue(plan.keep.isEmpty())
    }

    @Test
    fun `the line is a tenth of the channels with a guide to come`() {
        val upcoming = ids(1..300).toSet()

        val under = channelPrunePlan(ids(1..29), upcoming, fed = emptySet())
        assertEquals(ids(1..29), under.drop)
        assertTrue(under.keep.isEmpty())

        val at = channelPrunePlan(ids(1..30), upcoming, fed = emptySet())
        assertTrue(at.drop.isEmpty())
        assertEquals(ids(1..30), at.keep)
    }

    @Test
    fun `nothing missing is nothing to do`() {
        val plan = channelPrunePlan(emptyList(), upcoming = emptySet(), fed = ids(1..10).toSet())
        assertTrue(plan.drop.isEmpty())
        assertTrue(plan.keep.isEmpty())
    }
}
