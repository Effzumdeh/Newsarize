package com.example.newsarize.data.network

import android.util.Xml
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: Long
)

class RssService {

    private val client = HttpClient(Android) {
        // Simple Ktor setup
    }

    suspend fun fetchFeed(url: String): List<RssItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get(url)
                val xmlString: String = response.body()
                parseXml(xmlString)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun parseXml(xml: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentItem: MutableMap<String, String>? = null
        var text = ""

        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        // Support both RSS 2.0 (<item>) and Atom (<entry>)
                        if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                            currentItem = mutableMapOf()
                        } else if (tagName.equals("link", ignoreCase = true) && currentItem != null) {
                            // Atom format typically stores the URL in the 'href' attribute of a self-closing <link> tag
                            val rel = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href")
                            // Only capture alternate links or if rel is missing (default)
                            if (href != null && (rel == null || rel.equals("alternate", ignoreCase = true))) {
                                currentItem["link"] = href
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        text = parser.text
                    }

                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                            currentItem?.let {
                                // Fallback queries for both standards
                                val pubDateString = it["pubdate"] ?: it["published"] ?: it["updated"] ?: ""
                                val parsedDate = parseDate(pubDateString)
                                
                                val rawDesc = it["description"] ?: it["content"] ?: it["summary"] ?: ""
                                
                                // Some Atom links are extracted inside TEXT, others in START_TAG href.
                                // We keep whatever was written to "link"
                                val finalLink = it["link"] ?: ""

                                // Only keep items from today for summaries
                                if (isToday(parsedDate)) {
                                    items.add(
                                        RssItem(
                                            title = it["title"] ?: "Kein Titel",
                                            link = finalLink,
                                            description = removeHtmlTags(rawDesc),
                                            pubDate = parsedDate
                                        )
                                    )
                                }
                            }
                        } else if (currentItem != null) {
                            // Only overwrite if it hasn't been set by an attribute (like the Atom <link href=...>)
                            // and ignore empty text strings to avoid overwriting attributes with newline spaces
                            val lowerKey = tagName.lowercase(Locale.ROOT)
                            if (text.isNotBlank() || currentItem[lowerKey] == null) {
                                currentItem[lowerKey] = text.trim()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }

        return items
    }

    private fun removeHtmlTags(html: String): String {
        return html.replace(Regex("<.*?>"), "").trim()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yy HH:mm:ss Z",
            "EEE, dd MMM yy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssXXX", // Used by Caschys Blog / ISO-8601 standard
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        
        // Try English Locale first, then German (since we target Tagesschau/Heise)
        val locales = listOf(Locale.ENGLISH, Locale.GERMAN)

        for (locale in locales) {
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, locale)
                    val date: Date? = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (e: Exception) {
                    // Ignore and try next format
                }
            }
        }
        
        // If everything fails, log it and return current time as fallback
        println("Failed to parse date: $dateStr")
        return System.currentTimeMillis()
    }

    private fun isToday(timestamp: Long): Boolean {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val itemDay = timestamp / (1000 * 60 * 60 * 24)
        return currentDay == itemDay
    }
}
