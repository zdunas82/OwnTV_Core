package tv.own.owntv.core.content

/**
 * What narrows a Movies or Series list beyond its category and search: the release year, as a
 * closed or half-open range. Both bounds null is no filter at all ([NONE]).
 *
 * The year is the provider's (`year`), else the one parsed from the title ("Dune (2021)"), the same
 * fallback Trending uses to match titles — a title with neither is left out of a filtered list.
 */
data class VodFilter(val yearFrom: Int? = null, val yearTo: Int? = null) {
    val active: Boolean get() = yearFrom != null || yearTo != null

    companion object {
        val NONE = VodFilter()
    }
}
