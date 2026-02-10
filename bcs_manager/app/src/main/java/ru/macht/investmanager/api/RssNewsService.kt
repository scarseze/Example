package ru.macht.investmanager.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.http.GET
import retrofit2.http.Url

interface RssNewsService {
    @GET
    suspend fun getRssFeed(@Url url: String): RssFeed
}

@Root(name = "rss", strict = false)
data class RssFeed @JvmOverloads constructor(
    @field:Element(name = "channel", required = false)
    var channel: RssChannel? = null
)

@Root(name = "channel", strict = false)
data class RssChannel @JvmOverloads constructor(
    @field:ElementList(name = "item", inline = true, required = false)
    var items: List<RssItem>? = null
)

@Root(name = "item", strict = false)
data class RssItem @JvmOverloads constructor(
    @field:Element(name = "title", required = false)
    var title: String? = null,
    @field:Element(name = "description", required = false)
    var description: String? = null,
    @field:Element(name = "link", required = false)
    var link: String? = null,
    @field:Element(name = "pubDate", required = false)
    var pubDate: String? = null
)