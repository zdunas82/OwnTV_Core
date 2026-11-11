package tv.own.owntv.core.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Poterx: the index folds the case of ASCII letters only, so a title beginning with Ś, Ż or Ł is
 * stored with its capital and a typed lower-case word never met it. The query now tries the forms
 * such a word can be stored in.
 */
class SearchFtsAnyCaseTest {

    @Test
    fun `a word of ASCII letters is written exactly as before`() {
        for (parenthesised in listOf(true, false)) {
            assertEquals("Spider* M*", SearchReader.ftsQueryAnyCase("Spider-M", parenthesised))
            assertEquals("harry* pot*", SearchReader.ftsQueryAnyCase("  harry   pot ", parenthesised))
            assertEquals("Ocean* s* 11*", SearchReader.ftsQueryAnyCase("Ocean's 11", parenthesised))
            assertNull(SearchReader.ftsQueryAnyCase("-- !", parenthesised))
        }
    }

    @Test
    fun `a lower-case word finds the title that starts with a capital`() {
        assertEquals("ścigany* OR Ścigany*", SearchReader.ftsQueryAnyCase("ścigany", parenthesised = true))
        assertEquals("ścigany* OR Ścigany*", SearchReader.ftsQueryAnyCase("ścigany", parenthesised = false))
    }

    @Test
    fun `a typed capital finds the lower-case title too`() {
        assertEquals("Ścigany* OR ścigany*", SearchReader.ftsQueryAnyCase("ŚCIGANY", parenthesised = true))
    }

    @Test
    fun `a name in capitals is stored with only its ASCII letters lowered`() {
        // "ŻÓŁW" is indexed as "ŻÓŁw", "MIŁOŚĆ" as "miŁoŚĆ".
        assertEquals("żółw* OR Żółw* OR ŻÓŁw*", SearchReader.ftsQueryAnyCase("żółw", parenthesised = true))
        assertEquals("miłość* OR miŁoŚĆ*", SearchReader.ftsQueryAnyCase("miłość", parenthesised = true))
    }

    @Test
    fun `several words keep meaning all of them in the enhanced syntax`() {
        assertEquals(
            "(świat* OR Świat*) AND kiepskich*",
            SearchReader.ftsQueryAnyCase("świat kiepskich", parenthesised = true),
        )
        assertEquals(
            "(świat* OR Świat*) AND (według* OR wedŁug*)",
            SearchReader.ftsQueryAnyCase("świat według", parenthesised = true),
        )
    }

    @Test
    fun `several words keep meaning all of them in the standard syntax`() {
        // No parentheses there, and OR binds tighter than the unwritten AND between words.
        assertEquals(
            "świat* OR Świat* kiepskich*",
            SearchReader.ftsQueryAnyCase("świat kiepskich", parenthesised = false),
        )
        assertEquals(
            "kuchnia* łowca* OR Łowca*",
            SearchReader.ftsQueryAnyCase("kuchnia+ łowca", parenthesised = false),
        )
    }
}
