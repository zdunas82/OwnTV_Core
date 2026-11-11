package tv.own.owntv.core.database

/**
 * Poterx: how this SQLite build reads an FTS MATCH expression that mixes OR with other words.
 *
 * SQLite has two readings and picks one when it is compiled. The enhanced one takes parentheses and
 * binds an unwritten AND tighter than OR; the standard one refuses parentheses and binds OR tighter.
 * "a OR b c" is therefore "a, or both b and c" in one and "a or b, and c" in the other, and nothing
 * but a single run of ORs means the same in both. The search asks here before writing such a query
 * ([tv.own.owntv.core.content.SearchReader.ftsQueryAnyCase]).
 *
 * Set when a connection opens. Until then, and if the build cannot be asked, the standard reading
 * is assumed: read the enhanced way, a query written for it only finds more, never an error.
 */
object FtsSyntax {
    @Volatile
    var parenthesised: Boolean = false
        internal set

    internal const val PROBE = "SELECT sqlite_compileoption_used('ENABLE_FTS3_PARENTHESIS')"
}
