package tv.own.owntv.core.content

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.settings.SettingsRepository

class VodQueriesTest {

    @Test
    fun `a year range on All filters the provider year or the parsed one and orders newest first`() {
        val q = vodListQuery(VodTable.MOVIES, LiveKey.All, 7L, listOf(1L, 2L), "", SettingsRepository.SortMode.YEAR, VodFilter(2020, 2026))
        assertEquals(
            "SELECT x.* FROM movies x WHERE x.sourceId IN (?,?) AND COALESCE(x.year, x.parsedYear) >= ? AND COALESCE(x.year, x.parsedYear) <= ? " +
                "ORDER BY COALESCE(x.year, x.parsedYear) DESC, x.name ASC",
            q.sql,
        )
        assertArrayEquals(arrayOf<Any>(1L, 2L, 2020, 2026), q.args())
    }

    @Test
    fun `a folder keeps its playlist order and takes the search`() {
        val q = vodListQuery(VodTable.SERIES, LiveKey.Folder(42L), 7L, listOf(1L), "dark", SettingsRepository.SortMode.PLAYLIST, VodFilter(yearFrom = 2015))
        assertEquals(
            "SELECT x.* FROM series x WHERE x.categoryId = ? AND x.name LIKE '%' || ? || '%' AND COALESCE(x.year, x.parsedYear) >= ? ORDER BY x.sortOrder ASC, x.name ASC",
            q.sql,
        )
        assertArrayEquals(arrayOf<Any>(42L, "dark", 2015), q.args())
    }

    @Test
    fun `favourites and custom categories join their user-data tables`() {
        val fav = vodListQuery(VodTable.MOVIES, LiveKey.Favorites, 7L, listOf(1L), "", SettingsRepository.SortMode.PLAYLIST, VodFilter(yearTo = 1999))
        assertEquals(
            "SELECT x.* FROM movies x INNER JOIN favorites f ON f.itemId = x.id AND f.mediaType = 'MOVIE' " +
                "WHERE f.profileId = ? AND x.sourceId IN (?) AND COALESCE(x.year, x.parsedYear) <= ? ORDER BY f.addedAt DESC",
            fav.sql,
        )
        val custom = vodListQuery(VodTable.SERIES, LiveKey.Custom("cc:3"), 7L, listOf(1L), "", SettingsRepository.SortMode.ALPHA, VodFilter(2000, 2000))
        assertEquals(
            "SELECT x.* FROM series x INNER JOIN custom_category_members m ON m.itemId = x.id AND m.profileId = ? AND m.mediaType = 'SERIES' AND m.contextKey = ? " +
                "WHERE x.sourceId IN (?) AND COALESCE(x.year, x.parsedYear) >= ? AND COALESCE(x.year, x.parsedYear) <= ? ORDER BY x.name ASC",
            custom.sql,
        )
        assertArrayEquals(arrayOf<Any>(7L, "cc:3", 1L, 2000, 2000), custom.args())
    }

    @Test
    fun `the count of All leaves hidden categories out, as the fixed count does`() {
        val q = vodCountQuery(VodTable.MOVIES, LiveKey.All, 7L, listOf(1L), "", VodFilter(yearFrom = 2024), hiddenCats = setOf(5L, 6L))
        assertEquals(
            "SELECT COUNT(*) FROM movies x WHERE x.sourceId IN (?) AND (x.categoryId IS NULL OR NOT x.categoryId IN (?,?)) AND COALESCE(x.year, x.parsedYear) >= ?",
            q.sql,
        )
        assertArrayEquals(arrayOf<Any>(1L, 5L, 6L, 2024), q.args())
    }
}

/** The bound arguments, in order, as [androidx.sqlite.db.SimpleSQLiteQuery] hands them to the database. */
private fun androidx.sqlite.db.SupportSQLiteQuery.args(): Array<Any?> {
    val out = arrayOfNulls<Any>(argCount)
    bindTo(object : androidx.sqlite.db.SupportSQLiteProgram {
        override fun bindNull(index: Int) { out[index - 1] = null }
        override fun bindLong(index: Int, value: Long) { out[index - 1] = value }
        override fun bindDouble(index: Int, value: Double) { out[index - 1] = value }
        override fun bindString(index: Int, value: String) { out[index - 1] = value }
        override fun bindBlob(index: Int, value: ByteArray) { out[index - 1] = value }
        override fun clearBindings() {}
        override fun close() {}
    })
    return out
}
