package tv.own.owntv.core.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Poterx: a panel in trouble answers `get_*_categories` with `[]` or an error object, and both read
 * as "no categories". A re-sync that believed it stored every item without a category and deleted
 * the categories, and with them the hidden, locked and kids choices keyed on them.
 */
class XtreamCategoryListLostTest {

    @Test
    fun `a re-sync handed no categories keeps the stored ones`() {
        assertTrue(XtreamSyncer.categoryListLost(freshSource = false, fetched = 0, forcePrune = false))
    }

    @Test
    fun `a first import has nothing to keep`() {
        assertFalse(XtreamSyncer.categoryListLost(freshSource = true, fetched = 0, forcePrune = false))
    }

    @Test
    fun `a list with anything in it is taken as sent`() {
        assertFalse(XtreamSyncer.categoryListLost(freshSource = false, fetched = 1, forcePrune = false))
    }

    @Test
    fun `a sync the user forced clean takes the panel at its word`() {
        assertFalse(XtreamSyncer.categoryListLost(freshSource = false, fetched = 0, forcePrune = true))
    }
}
