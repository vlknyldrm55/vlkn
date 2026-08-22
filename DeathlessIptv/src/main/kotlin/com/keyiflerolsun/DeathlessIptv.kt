// ! https://codeberg.org/cloudstream/cloudstream-extensions-multilingual/src/branch/master/DeathlessIptv/src/main/kotlin/com/lagradost/DeathlessIptv.kt

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.io.InputStream

class DeathlessIptv : MainAPI() {

    override var mainUrl = "https://snowy-mountain-b566.volkan5569.workers.dev/"
    override var name = "DeathlessIptv"

    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasDownloadSupport = false

    override var lang = "tr"

    override val supportedTypes = setOf(TvType.Live)

    /**
     * M3U listesini indir
     */
    private suspend fun getPlaylist(): Playlist {
        val response = app.get(
            mainUrl,
            timeout = 20
        )

        return IptvPlaylistParser().parseM3U(response.text)
    }

    /**
     * ANA SAYFA
     */
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val playlist = getPlaylist()

        val groups = playlist.items
            .filter { !it.url.isNullOrBlank() }
            .groupBy {
                it.attributes["group-title"]
                    ?.takeIf { group -> group.isNotBlank() }
                    ?: "Diğer"
            }

        return newHomePageResponse(
            groups.map { (groupName, channels) ->

                val shows = channels.map { channel ->

                    val streamUrl = channel.url ?: ""
                    val title = channel.title ?: "Bilinmeyen Kanal"
                    val poster = channel.attributes["tvg-logo"] ?: ""
                    val country = channel.attributes["tvg-country"] ?: ""

                    newLiveSearchResponse(
                        title,

                        LoadData(
                            url = streamUrl,
                            title = title,
                            poster = poster,
                            group = groupName,
                            nation = country
                        ).toJson(),

                        type = TvType.Live
                    ) {
                        this.posterUrl = poster
                        this.lang = country
                    }
                }

                HomePageList(
                    groupName,
                    shows,
                    isHorizontalImages = true
                )
            },

            hasNext = false
        )
    }

    /**
     * ARAMA
     */
    override suspend fun search(query: String): List<SearchResponse> {

        val playlist = getPlaylist()

        return playlist.items
            .filter { !it.url.isNullOrBlank() }
            .filter {
                it.title
                    ?.contains(query, ignoreCase = true)
                    ?: false
            }
            .map { channel ->

                val streamUrl = channel.url ?: ""
                val title = channel.title ?: "Bilinmeyen Kanal"
                val poster = channel.attributes["tvg-logo"] ?: ""
                val group = channel.attributes["group-title"] ?: "Diğer"
                val country = channel.attributes["tvg-country"] ?: ""

                newLiveSearchResponse(
                    title,

                    LoadData(
                        url = streamUrl,
                        title = title,
                        poster = poster,
                        group = group,
                        nation = country
                    ).toJson(),

                    type = TvType.Live
                ) {
                    this.posterUrl = poster
                    this.lang = country
                }
            }
    }

    override suspend fun quickSearch(
        query: String
    ): List<SearchResponse> = search(query)

    /**
     * KANAL SAYFASI
     */
    override suspend fun load(
        url: String
    ): LoadResponse {

        val loadData = fetchDataFromUrlOrJson(url)

        val description =
            if (loadData.group.equals("NSFW", ignoreCase = true)) {
                "⚠️🔞 ${loadData.group} | ${loadData.nation} 🔞⚠️"
            } else {
                "${loadData.group} | ${loadData.nation}"
            }

        val playlist = getPlaylist()

        val recommendations = playlist.items
            .filter {
                it.attributes["group-title"] == loadData.group &&
                it.url != loadData.url &&
                !it.url.isNullOrBlank()
            }
            .map { channel ->

                val streamUrl = channel.url ?: ""
                val title = channel.title ?: "Bilinmeyen Kanal"
                val poster = channel.attributes["tvg-logo"] ?: ""
                val group = channel.attributes["group-title"] ?: "Diğer"
                val country = channel.attributes["tvg-country"] ?: ""

                newLiveSearchResponse(
                    title,

                    LoadData(
                        url = streamUrl,
                        title = title,
                        poster = poster,
                        group = group,
                        nation = country
                    ).toJson(),

                    type = TvType.Live
                ) {
                    this.posterUrl = poster
                    this.lang = country
                }
            }

        return newLiveStreamLoadResponse(
            loadData.title,
            loadData.url,
            url
        ) {
            this.posterUrl = loadData.poster
            this.plot = description
            this.tags = listOf(
                loadData.group,
                loadData.nation
            )

            this.recommendations = recommendations
        }
    }

    /**
     * GERÇEK STREAM LİNKİNİ CLOUDSTREAM OYNATICISINA VER
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        return try {

            val loadData = fetchDataFromUrlOrJson(data)

            Log.d(
                "DeathlessIptv",
                "Channel: ${loadData.title}"
            )

            Log.d(
                "DeathlessIptv",
                "URL: ${loadData.url}"
            )

            val playlist = getPlaylist()

            val channel = playlist.items.firstOrNull {
                it.url == loadData.url
            }

            /**
             * M3U içerisinde kanal bulunduysa header bilgilerini al.
             */
            val headers = channel?.headers ?: emptyMap()

            val referer =
                headers.entries
                    .firstOrNull {
                        it.key.equals("referer", ignoreCase = true)
                    }
                    ?.value
                    ?: ""

            val userAgent =
                headers.entries
                    .firstOrNull {
                        it.key.equals("user-agent", ignoreCase = true)
                    }
                    ?.value

            val finalHeaders =
                if (!userAgent.isNullOrBlank()) {
                    headers + mapOf(
                        "User-Agent" to userAgent
                    )
                } else {
                    headers
                }

            Log.d(
                "DeathlessIptv",
                "Referer: $referer"
            )

            Log.d(
                "DeathlessIptv",
                "Headers: $finalHeaders"
            )

            /**
             * Gerçek M3U8 / video adresini CloudStream'e ver.
             */
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = loadData.title,
                    url = loadData.url,
                    type = ExtractorLinkType.M3U8
                ) {

                    this.referer = referer
                    this.headers = finalHeaders

                    this.quality = Qualities.Unknown.value
                }
            )

            true

        } catch (e: Exception) {

            Log.e(
                "DeathlessIptv",
                "loadLinks ERROR: ${e.message}",
                e
            )

            false
        }
    }

    /**
     * JSON veya direkt URL'den LoadData oluştur
     */
    private suspend fun fetchDataFromUrlOrJson(
        data: String
    ): LoadData {

        if (data.trim().startsWith("{")) {
            return parseJson<LoadData>(data)
        }

        val playlist = getPlaylist()

        val channel = playlist.items.firstOrNull {
            it.url == data
        } ?: throw Exception(
            "M3U içerisinde kanal bulunamadı: $data"
        )

        return LoadData(
            url = channel.url ?: data,
            title = channel.title ?: "Bilinmeyen Kanal",
            poster = channel.attributes["tvg-logo"] ?: "",
            group = channel.attributes["group-title"] ?: "Diğer",
            nation = channel.attributes["tvg-country"] ?: ""
        )
    }

    data class LoadData(
        val url: String,
        val title: String,
        val poster: String,
        val group: String,
        val nation: String
    )
}


/* ============================================================
 * PLAYLIST MODELLERİ
 * ============================================================ */

data class Playlist(
    val items: List<PlaylistItem> = emptyList()
)

data class PlaylistItem(
    val title: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val url: String? = null,
    val userAgent: String? = null
)


/* ============================================================
 * M3U PARSER
 * ============================================================ */

class IptvPlaylistParser {

    fun parseM3U(content: String): Playlist {
        return parseM3U(content.byteInputStream())
    }

    @Throws(PlaylistParserException::class)
    fun parseM3U(input: InputStream): Playlist {

        val reader = input.bufferedReader()

        val firstLine = reader.readLine()
            ?: throw PlaylistParserException.InvalidHeader()

        if (!firstLine.trim().startsWith("#EXTM3U")) {
            throw PlaylistParserException.InvalidHeader()
        }

        val items = mutableListOf<PlaylistItem>()

        var currentItem: PlaylistItem? = null

        reader.forEachLine { rawLine ->

            val line = rawLine.trim()

            if (line.isEmpty()) return@forEachLine

            when {

                line.startsWith("#EXTINF", ignoreCase = true) -> {

                    val title = getTitle(line)
                    val attributes = getAttributes(line)

                    currentItem = PlaylistItem(
                        title = title,
                        attributes = attributes
                    )
                }

                line.startsWith(
                    "#EXTVLCOPT",
                    ignoreCase = true
                ) -> {

                    val item = currentItem ?: return@forEachLine

                    val headers =
                        item.headers.toMutableMap()

                    val userAgent =
                        getTagValue(
                            line,
                            "http-user-agent"
                        )

                    val referrer =
                        getTagValue(
                            line,
                            "http-referrer"
                        )

                    if (!userAgent.isNullOrBlank()) {
                        headers["User-Agent"] = userAgent
                    }

                    if (!referrer.isNullOrBlank()) {
                        headers["Referer"] = referrer
                    }

                    currentItem = item.copy(
                        headers = headers,
                        userAgent =
                            userAgent ?: item.userAgent
                    )
                }

                !line.startsWith("#") -> {

                    val item =
                        currentItem ?: return@forEachLine

                    val parts =
                        line.split("|", limit = 2)

                    val url =
                        parts[0].trim()

                    val urlParameters =
                        if (parts.size > 1)
                            parts[1]
                        else
                            ""

                    val headers =
                        item.headers.toMutableMap()

                    val userAgent =
                        getParameter(
                            urlParameters,
                            "user-agent"
                        )

                    val referer =
                        getParameter(
                            urlParameters,
                            "referer"
                        ) ?: getParameter(
                            urlParameters,
                            "referrer"
                        )

                    if (!userAgent.isNullOrBlank()) {
                        headers["User-Agent"] = userAgent
                    }

                    if (!referer.isNullOrBlank()) {
                        headers["Referer"] = referer
                    }

                    items.add(
                        item.copy(
                            url = url,
                            headers = headers,
                            userAgent =
                                userAgent ?: item.userAgent
                        )
                    )

                    currentItem = null
                }
            }
        }

        return Playlist(items)
    }

    /**
     * #EXTINF satırındaki kanal adını al
     */
    private fun getTitle(
        line: String
    ): String {

        val commaIndex = line.lastIndexOf(",")

        if (commaIndex == -1) {
            return "Bilinmeyen Kanal"
        }

        return line
            .substring(commaIndex + 1)
            .trim()
            .replace("\"", "")
    }

    /**
     * #EXTINF özelliklerini güvenli şekilde oku.
     *
     * Örnek:
     *
     * group-title="Spor"
     * tvg-logo="https://..."
     * tvg-country="TR"
     */
    private fun getAttributes(
        line: String
    ): Map<String, String> {

        val result = mutableMapOf<String, String>()

        val regex = Regex(
            """([\w-]+)\s*=\s*"([^"]*)""""
        )

        regex.findAll(line).forEach { match ->

            val key = match.groupValues[1]
            val value = match.groupValues[2]

            result[key] = value
        }

        return result
    }

    /**
     * EXTVLCOPT değeri
     */
    private fun getTagValue(
        line: String,
        key: String
    ): String? {

        val regex = Regex(
            """$key\s*=\s*(.*)""",
            RegexOption.IGNORE_CASE
        )

        return regex
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.removeSurrounding("\"")
    }

    /**
     * URL sonundaki:
     *
     * |User-Agent=...
     * |Referer=...
     */
    private fun getParameter(
        parameters: String,
        key: String
    ): String? {

        if (parameters.isBlank()) {
            return null
        }

        val regex = Regex(
            """(?:^|&)$key=([^&]*)""",
            RegexOption.IGNORE_CASE
        )

        return regex
            .find(parameters)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.removeSurrounding("\"")
    }
}


/* ============================================================
 * EXCEPTION
 * ============================================================ */

sealed class PlaylistParserException(
    message: String
) : Exception(message) {

    class InvalidHeader :
        PlaylistParserException(
            "Invalid file header. Header doesn't start with #EXTM3U"
        )
}
