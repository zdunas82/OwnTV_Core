package tv.own.owntv.core.content

import androidx.paging.PagingSource
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import tv.own.owntv.core.database.dao.CustomCategoryDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.entity.ContentOrderEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.settings.SettingsRepository

/**
 * Which query backs a Movies or Series list selection — the VOD twin of `livePagingSource`.
 *
 * Movies and Series select from the same rail model Live TV uses ([LiveKey]), so one selection type
 * covers all three sections. Catch-up is the exception and degrades to All: channels keep archives,
 * films do not.
 *
 * [contextKey] is the folder's stable customization key and [hasManualOrder] says whether that folder
 * actually carries manual-order rows — without one, the plain indexed query has the identical
 * (sortOrder, name) order and skips the join-sort.
 */
fun moviePagingSource(
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    query: String,
    sort: SettingsRepository.SortMode,
    movieDao: MovieDao,
    customCategoryDao: CustomCategoryDao,
    contextKey: (Long) -> String?,
    hasManualOrder: (String) -> Boolean,
    filter: VodFilter = VodFilter.NONE,
): PagingSource<Int, MovieEntity> {
    val ids = sourceIds.ifEmpty { listOf(-1L) }
    // A year filter or the year order: one built query instead of a copy of every fixed one below.
    if (filter.active || sort == SettingsRepository.SortMode.YEAR) {
        return movieDao.pagingRaw(vodListQuery(VodTable.MOVIES, key, profileId, ids, query, sort, filter))
    }
    val rating = sort == SettingsRepository.SortMode.RATING
    val dateAdded = sort == SettingsRepository.SortMode.DATE_ADDED
    val playlist = sort == SettingsRepository.SortMode.PLAYLIST
    return if (query.isBlank()) {
        when (key) {
            LiveKey.All, LiveKey.Catchup -> when {
                rating -> movieDao.pagingAllRating(ids)
                dateAdded -> movieDao.pagingAllDateAdded(ids)
                playlist -> movieDao.pagingAllOriginal(ids)
                else -> movieDao.pagingAll(ids)
            }
            LiveKey.Favorites -> movieDao.pagingFavoritesManual(profileId, ContentOrderEntity.FAV_CONTEXT, ids)
            LiveKey.History -> movieDao.pagingHistory(profileId, ids)
            is LiveKey.Custom ->
                if (playlist) customCategoryDao.pagingMovies(profileId, key.id, ids)
                else customCategoryDao.pagingMoviesAlpha(profileId, key.id, ids)
            is LiveKey.Folder -> {
                val ctxKey = contextKey(key.id).orEmpty()
                when {
                    rating -> movieDao.pagingByCategoryRating(key.id)
                    dateAdded -> movieDao.pagingByCategoryDateAdded(key.id)
                    !hasManualOrder(ctxKey) -> if (playlist) movieDao.pagingByCategory(key.id) else movieDao.pagingByCategoryAlpha(key.id)
                    playlist -> movieDao.pagingByCategoryManual(key.id, profileId, ctxKey)
                    else -> movieDao.pagingByCategoryManualAlpha(key.id, profileId, ctxKey)
                }
            }
        }
    } else {
        when (key) {
            LiveKey.All, LiveKey.Catchup ->
                if (dateAdded) movieDao.searchAllDateAdded(query, ids) else movieDao.searchAll(query, ids)
            LiveKey.Favorites -> movieDao.searchFavorites(query, profileId, ids)
            LiveKey.History -> movieDao.searchHistory(query, profileId, ids)
            is LiveKey.Custom -> customCategoryDao.searchMovies(query, profileId, key.id, ids)
            is LiveKey.Folder ->
                if (dateAdded) movieDao.searchInCategoryDateAdded(query, key.id)
                else movieDao.searchInCategory(query, key.id)
        }
    }
}

/** [moviePagingSource] for shows. */
fun seriesPagingSource(
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    query: String,
    sort: SettingsRepository.SortMode,
    seriesDao: SeriesDao,
    customCategoryDao: CustomCategoryDao,
    contextKey: (Long) -> String?,
    hasManualOrder: (String) -> Boolean,
    filter: VodFilter = VodFilter.NONE,
): PagingSource<Int, SeriesEntity> {
    val ids = sourceIds.ifEmpty { listOf(-1L) }
    if (filter.active || sort == SettingsRepository.SortMode.YEAR) {
        return seriesDao.pagingRaw(vodListQuery(VodTable.SERIES, key, profileId, ids, query, sort, filter))
    }
    val rating = sort == SettingsRepository.SortMode.RATING
    val dateAdded = sort == SettingsRepository.SortMode.DATE_ADDED
    val playlist = sort == SettingsRepository.SortMode.PLAYLIST
    return if (query.isBlank()) {
        when (key) {
            LiveKey.All, LiveKey.Catchup -> when {
                rating -> seriesDao.pagingAllRating(ids)
                dateAdded -> seriesDao.pagingAllDateAdded(ids)
                playlist -> seriesDao.pagingAllOriginal(ids)
                else -> seriesDao.pagingAll(ids)
            }
            LiveKey.Favorites -> seriesDao.pagingFavoritesManual(profileId, ContentOrderEntity.FAV_CONTEXT, ids)
            LiveKey.History -> seriesDao.pagingHistory(profileId, ids)
            is LiveKey.Custom ->
                if (playlist) customCategoryDao.pagingSeries(profileId, key.id, ids)
                else customCategoryDao.pagingSeriesAlpha(profileId, key.id, ids)
            is LiveKey.Folder -> {
                val ctxKey = contextKey(key.id).orEmpty()
                when {
                    rating -> seriesDao.pagingByCategoryRating(key.id)
                    dateAdded -> seriesDao.pagingByCategoryDateAdded(key.id)
                    !hasManualOrder(ctxKey) -> if (playlist) seriesDao.pagingByCategory(key.id) else seriesDao.pagingByCategoryAlpha(key.id)
                    playlist -> seriesDao.pagingByCategoryManual(key.id, profileId, ctxKey)
                    else -> seriesDao.pagingByCategoryManualAlpha(key.id, profileId, ctxKey)
                }
            }
        }
    } else {
        when (key) {
            LiveKey.All, LiveKey.Catchup ->
                if (dateAdded) seriesDao.searchAllDateAdded(query, ids) else seriesDao.searchAll(query, ids)
            LiveKey.Favorites -> seriesDao.searchFavorites(query, profileId, ids)
            LiveKey.History -> seriesDao.searchHistory(query, profileId, ids)
            is LiveKey.Custom -> customCategoryDao.searchSeries(query, profileId, key.id, ids)
            is LiveKey.Folder ->
                if (dateAdded) seriesDao.searchInCategoryDateAdded(query, key.id)
                else seriesDao.searchInCategory(query, key.id)
        }
    }
}

/** The film count for one selection. */
fun movieCountFlow(
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    hiddenCats: Set<Long>,
    movieDao: MovieDao,
    customCategoryDao: CustomCategoryDao,
    filter: VodFilter = VodFilter.NONE,
    query: String = "",
): Flow<Int> {
    val ids = sourceIds.ifEmpty { listOf(-1L) }
    if (filter.active) return movieDao.countRaw(vodCountQuery(VodTable.MOVIES, key, profileId, ids, query, filter, hiddenCats))
    return when (key) {
        LiveKey.All, LiveKey.Catchup ->
            if (hiddenCats.isEmpty()) movieDao.countAll(ids)
            else movieDao.countAllExcluding(ids, hiddenCats.toList())
        LiveKey.Favorites -> movieDao.countFavorites(profileId, ids)
        LiveKey.History -> movieDao.countHistory(profileId, ids)
        is LiveKey.Custom -> customCategoryDao.countMembers(profileId, MediaType.MOVIE, key.id, ids)
        is LiveKey.Folder -> movieDao.countByCategory(key.id)
    }
}

/** [movieCountFlow] for shows. */
fun seriesCountFlow(
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    hiddenCats: Set<Long>,
    seriesDao: SeriesDao,
    customCategoryDao: CustomCategoryDao,
    filter: VodFilter = VodFilter.NONE,
    query: String = "",
): Flow<Int> {
    val ids = sourceIds.ifEmpty { listOf(-1L) }
    if (filter.active) return seriesDao.countRaw(vodCountQuery(VodTable.SERIES, key, profileId, ids, query, filter, hiddenCats))
    return when (key) {
        LiveKey.All, LiveKey.Catchup ->
            if (hiddenCats.isEmpty()) seriesDao.countAll(ids)
            else seriesDao.countAllExcluding(ids, hiddenCats.toList())
        LiveKey.Favorites -> seriesDao.countFavorites(profileId, ids)
        LiveKey.History -> seriesDao.countHistory(profileId, ids)
        is LiveKey.Custom -> customCategoryDao.countMembers(profileId, MediaType.SERIES, key.id, ids)
        is LiveKey.Folder -> seriesDao.countByCategory(key.id)
    }
}

/** The two VOD tables the built queries select from, with the media type their user-data rows carry. */
enum class VodTable(val table: String, val mediaType: String) { MOVIES("movies", "MOVIE"), SERIES("series", "SERIES") }

/**
 * The year column the filter and the year order read: the provider's year, else the one parsed
 * from the title. The alias `x` is the VOD table in every built query.
 */
private const val YEAR_EXPR = "COALESCE(x.year, x.parsedYear)"

/**
 * The list for one selection with a year filter and/or the year order, as SQL built here instead of
 * a fixed DAO query per combination: the selection's own FROM/WHERE (All, a folder, Favorites,
 * History or a custom category), the search, the year bounds, then the order.
 *
 * Manual order (Move) is not applied here: a filtered or year-ordered list is a different view of
 * the folder, and the hand-made order belongs to the plain one.
 */
fun vodListQuery(
    table: VodTable,
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    query: String,
    sort: SettingsRepository.SortMode,
    filter: VodFilter,
): SupportSQLiteQuery {
    val (from, args) = vodFromWhere(table, key, profileId, sourceIds, query, filter, hiddenCats = emptySet())
    val order = when (sort) {
        SettingsRepository.SortMode.YEAR -> "$YEAR_EXPR DESC, x.name ASC"
        SettingsRepository.SortMode.ALPHA -> "x.name ASC"
        SettingsRepository.SortMode.RATING -> "x.rating DESC, x.name ASC"
        SettingsRepository.SortMode.DATE_ADDED -> "x.addedAt DESC, x.sortOrder DESC, x.id DESC"
        SettingsRepository.SortMode.PLAYLIST -> when (key) {
            LiveKey.All, LiveKey.Catchup -> "x.sourceId ASC, x.sortOrder ASC, x.name ASC"
            is LiveKey.Folder -> "x.sortOrder ASC, x.name ASC"
            LiveKey.Favorites -> "f.addedAt DESC"
            LiveKey.History -> "h.watchedAt DESC"
            is LiveKey.Custom -> "m.position, x.sortOrder, x.name"
        }
    }
    return SimpleSQLiteQuery("SELECT x.* $from ORDER BY $order", args.toTypedArray())
}

/** The count behind [vodListQuery]'s list; [hiddenCats] are left out of All, as the fixed count does. */
fun vodCountQuery(
    table: VodTable,
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    query: String,
    filter: VodFilter,
    hiddenCats: Set<Long>,
): SupportSQLiteQuery {
    val (from, args) = vodFromWhere(table, key, profileId, sourceIds, query, filter, hiddenCats)
    return SimpleSQLiteQuery("SELECT COUNT(*) $from", args.toTypedArray())
}

private fun vodFromWhere(
    table: VodTable,
    key: LiveKey,
    profileId: Long,
    sourceIds: List<Long>,
    query: String,
    filter: VodFilter,
    hiddenCats: Set<Long>,
): Pair<String, List<Any>> {
    val args = ArrayList<Any>()
    val sb = StringBuilder("FROM ${table.table} x ")
    val where = ArrayList<String>()
    fun inList(column: String, values: Collection<Long>): String {
        args.addAll(values)
        return "$column IN (" + values.joinToString(",") { "?" } + ")"
    }
    when (key) {
        LiveKey.All, LiveKey.Catchup -> {
            where += inList("x.sourceId", sourceIds)
            if (hiddenCats.isNotEmpty()) where += "(x.categoryId IS NULL OR NOT " + inList("x.categoryId", hiddenCats) + ")"
        }
        is LiveKey.Folder -> { where += "x.categoryId = ?"; args += key.id }
        LiveKey.Favorites -> {
            sb.append("INNER JOIN favorites f ON f.itemId = x.id AND f.mediaType = '${table.mediaType}' ")
            where += "f.profileId = ?"; args += profileId
            where += inList("x.sourceId", sourceIds)
        }
        LiveKey.History -> {
            sb.append("INNER JOIN watch_history h ON h.itemId = x.id AND h.mediaType = '${table.mediaType}' ")
            where += "h.profileId = ?"; args += profileId
            where += inList("x.sourceId", sourceIds)
        }
        is LiveKey.Custom -> {
            sb.append("INNER JOIN custom_category_members m ON m.itemId = x.id AND m.profileId = ? AND m.mediaType = '${table.mediaType}' AND m.contextKey = ? ")
            args += profileId; args += key.id
            where += inList("x.sourceId", sourceIds)
        }
    }
    if (query.isNotBlank()) { where += "x.name LIKE '%' || ? || '%'"; args += query }
    filter.yearFrom?.let { where += "$YEAR_EXPR >= ?"; args += it }
    filter.yearTo?.let { where += "$YEAR_EXPR <= ?"; args += it }
    sb.append("WHERE ").append(where.joinToString(" AND "))
    return sb.toString() to args
}
