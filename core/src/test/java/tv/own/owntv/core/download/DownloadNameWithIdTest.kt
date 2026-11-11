package tv.own.owntv.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Poterx: two titles with one name must not be written to one file. */
class DownloadNameWithIdTest {

    @Test
    fun `the id goes before the extension`() {
        assertEquals("Dune [1234].mkv", downloadNameWithId("Dune.mkv", 1234))
    }

    @Test
    fun `only the last dot starts the extension`() {
        assertEquals("Mr. Robot S01E01 [7].mp4", downloadNameWithId("Mr. Robot S01E01.mp4", 7))
    }

    @Test
    fun `a name without an extension gets the id at its end`() {
        assertEquals("Dune [1234]", downloadNameWithId("Dune", 1234))
        // A leading dot is part of the name, not an extension.
        assertEquals(".hidden [5]", downloadNameWithId(".hidden", 5))
    }

    @Test
    fun `two titles of one name end up apart`() {
        assertNotEquals(downloadNameWithId("Dune.mkv", 1), downloadNameWithId("Dune.mkv", 2))
    }

    @Test
    fun `a title already written under the name with its id keeps that name`() {
        val name = "Dune [2].mkv"
        val encoded = "Dune%20%5B2%5D.mkv"
        // On a mounted volume, and in a folder picked through the system (addressed by a URI).
        assertTrue(keepsNameWithId(listOf("/storage/emulated/0/Download/Movies/Dune [2].mkv"), name, encoded))
        assertTrue(
            keepsNameWithId(
                listOf("content://com.android.externalstorage.documents/tree/primary%3ADownload/document/primary%3ADownload%2FMovies%2FDune%20%5B2%5D.mkv"),
                name, encoded,
            ),
        )
        // Written under the plain name, or not downloaded at all: the plain name is tried first.
        assertFalse(keepsNameWithId(listOf("/storage/emulated/0/Download/Movies/Dune.mkv"), name, encoded))
        assertFalse(keepsNameWithId(emptyList(), name, encoded))
    }
}
