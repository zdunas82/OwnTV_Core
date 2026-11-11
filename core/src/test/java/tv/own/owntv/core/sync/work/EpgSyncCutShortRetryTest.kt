package tv.own.owntv.core.sync.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Poterx: a guide download that loses its connection part-way keeps the part it has and returns
 * normally. It used to be stamped as a finished sync, so the missing days waited for the next
 * scheduled refresh.
 */
class EpgSyncCutShortRetryTest {

    @Test
    fun `a download cut short is tried again`() {
        assertTrue(shouldRetryCutShortEpgSync(cutShort = true, runAttemptCount = 0))
    }

    @Test
    fun `a complete download is not`() {
        assertFalse(shouldRetryCutShortEpgSync(cutShort = false, runAttemptCount = 0))
    }

    @Test
    fun `the last attempt settles for the part it has`() {
        assertFalse(shouldRetryCutShortEpgSync(cutShort = true, runAttemptCount = MAX_EPG_RETRY_ATTEMPTS))
    }
}
