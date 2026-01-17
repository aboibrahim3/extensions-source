package eu.kanade.tachiyomi.extension.ar.mangaslayer

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class MangaSlayer : ParsedHttpSource() {

    override val name = "MangaSlayer"

    override val baseUrl = "https://mangaslayer.com"

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/manga-list?page=$page", headers)

    override fun popularMangaSelector() = "div.manga-item, div.col-md-4, div.col-sm-6"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.select("a").attr("href"))
            title = element.select("h3, .title").text().trim()
            thumbnail_url = element.select("img").attr("src")
        }
    }

    override fun popularMangaNextPageSelector() = "a.next-page, li.active + li a"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?q=$query&page=$page", headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1.title").text().trim()
            description = document.select("div.description, p.description").text().trim()
            genre = document.select("div.genres a, div.tags a").joinToString { it.text() }
            status = when {
                document.text().contains("مستمرة") -> SManga.ONGOING
                document.text().contains("مكتملة") -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun chapterListSelector() = "ul.chapters li, div.chapter-list div.chapter-item"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            val link = element.select("a").first()!!
            setUrlWithoutDomain(link.attr("href"))
            name = link.text().trim()
            date_upload = System.currentTimeMillis()
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select("div.reader-images img, #all-pages img").forEachIndexed { i, img ->
            val url = img.attr("src")
            if (url.isNotEmpty()) pages.add(Page(i, "", url))
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = ""
}
