package tv.own.owntv.core.download

/**
 * Poterx: telling the file from a page sent in its place, and a slow download from a failing one.
 *
 * Some panels refuse a request — a connection too many, too many requests in a row — not with
 * 429/458/509 but with "200 OK" and a small error page (seen: nginx's "404 Not Found" page, 546
 * bytes, under status 200). Upstream took that for the file. As the opening answer it replaced the
 * partial file and was shown as a finished download of 0.0 MB. As the answer to one block it failed
 * the attempt at once, and three of those within seconds failed a download that was half-way there.
 *
 * Public only so that the app's unit tests — the ones CI runs — can reach it.
 */
object DownloadAnswers {

    /** How much of a body is read before the file on disk is touched. */
    const val HEAD_BYTES = 64 * 1024

    /** What an attempt has to add to the file to count as having got somewhere. */
    const val PROGRESS_BYTES = 1L * 1024 * 1024

    /**
     * Whether a body that is about to replace the file is a page instead. [length] bytes of [head]
     * were read, and [ended] says the body stopped there.
     *
     * A body that ends within [HEAD_BYTES] is not a film, whatever its headers say; neither is one
     * that opens with markup. The content type is deliberately not asked: a panel that serves films
     * from a script without naming a type sends them as `text/html`.
     */
    fun isPage(head: ByteArray, length: Int, ended: Boolean): Boolean {
        if (ended) return true
        var i = if (length >= 3 && head[0] == BOM_0 && head[1] == BOM_1 && head[2] == BOM_2) 3 else 0
        while (i < length && head[i] in BLANK) i++
        return i < length && head[i] == MARKUP
    }

    /**
     * The attempt number a download goes on from once an attempt has failed.
     *
     * An attempt that added to the file is a slower way forward, not one of the three tries: the
     * count starts again, so a large file gets through a provider that drops the connections every
     * few hundred megabytes. Each restart needs the file to have grown, so it cannot go on for ever.
     */
    fun attemptAfter(attempt: Int, bytesBefore: Long, bytesAfter: Long): Int =
        if (bytesAfter - bytesBefore >= PROGRESS_BYTES) 1 else attempt

    // The UTF-8 byte order mark some pages open with.
    private val BOM_0 = 0xEF.toByte()
    private val BOM_1 = 0xBB.toByte()
    private val BOM_2 = 0xBF.toByte()
    private val MARKUP = '<'.code.toByte()
    private val BLANK = byteArrayOf(' '.code.toByte(), '\t'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
}
