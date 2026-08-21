// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Diziyo : MainAPI() {
    override var mainUrl              = "https://www.diziyo.so"
    override var name                 = "Diziyo"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler/page/" to "Tüm Diziler",
        "${mainUrl}/trend-diziler/page/" to "Trend Diziler",
        "${mainUrl}/son-eklenen-bolumler/page/" to "Son Bölümler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}$page"
        val document = app.get(url, headers = getHeaders(mainUrl)).document
        val home = mutableListOf<SearchResponse>()

        document.select("div.poster, div.episodes-list a, div.serie-card").forEach { element ->
            val title = element.selectFirst("h2, h3, .title, .name")?.text()?.trim() ?: return@forEach
            val href = fixUrlNull(element.selectFirst("a")?.attr("href")) ?: return@forEach
            val posterUrl = fixUrlNull(element.selectFirst("img")?.attr("data-src") ?: element.selectFirst("img")?.attr("src"))

            home.add(newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            })
        }

        return newHomePageResponse(request.name, home, hasNext = home.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "${mainUrl}/?s=${query}"
        val document = app.get(url, headers = getHeaders(mainUrl)).document

        return document.select("div.search-result, div.poster, article").mapNotNull { element ->
            val title = element.selectFirst("h2, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = fixUrlNull(element.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val posterUrl = fixUrlNull(element.selectFirst("img")?.attr("data-src") ?: element.selectFirst("img")?.attr("src"))

            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = getHeaders(mainUrl)).document
        val title = document.selectFirst("h1, .entry-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.poster img, .series-poster img")?.attr("src"))
        val description = document.selectFirst("div.description, .overview, p.story")?.text()?.trim()

        val episodes = mutableListOf<com.lagradost.cloudstream3.Episode>()
        document.select("div.seasons-wrapper div.episode-item, ul.episodes-list li").forEach { ep ->
            val epHref = fixUrlNull(ep.selectFirst("a")?.attr("href")) ?: return@forEach
            val epName = ep.selectFirst(".ep-title, .name, a")?.text()?.trim() ?: "Bölüm"
            val seasonNum = ep.attr("data-season").toIntOrNull() ?: 1
            val episodeNum = ep.attr("data-episode").toIntOrNull() ?: 1

            episodes.add(newEpisode(epHref) {
                this.name = epName
                this.season = seasonNum
                this.episode = episodeNum
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = getHeaders(mainUrl)).document

        document.select("iframe[src]").forEach { iframe ->
            val iframeUrl = fixUrlNull(iframe.attr("src")) ?: return@forEach
            loadExtractor(iframeUrl, data, subtitleCallback, callback)
        }

        return true
    }

    private fun getHeaders(baseUrl: String): Map<String, String> {
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en;q=0.8",
            "Referer" to baseUrl
        )
    }
}
