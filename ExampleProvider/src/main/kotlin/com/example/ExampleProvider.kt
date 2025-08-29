package com.simkl.cloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.search

class SimklExtension : MainAPI() {
    override var mainUrl = "https://api.simkl.com"
    override var name = "Simkl"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val hasDownloadSupport = false

    private fun getAuthToken(): String? {
        // Use Cloudstream account manager to retrieve stored Simkl token
        return AccountManager.getKey("simkl_access_token")
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val token = getAuthToken() ?: throw ErrorLoadingException("Not logged in to Simkl")

        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )

        // Call Simkl API to fetch lists
        val json = app.get("$mainUrl/sync/all-items", headers = headers).parsedSafe<SimklResponse>()
            ?: throw ErrorLoadingException("Failed to load list")

        val watching = json.items.filter { it.status == "watching" }.map {
            it.toSearchResponse()
        }

        val completed = json.items.filter { it.status == "completed" }.map {
            it.toSearchResponse()
        }

        val planToWatch = json.items.filter { it.status == "plan_to_watch" }.map {
            it.toSearchResponse()
        }

        return HomePageResponse(
            listOf(
                HomePageList("Watching", watching),
                HomePageList("Completed", completed),
                HomePageList("Plan to Watch", planToWatch),
            )
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        // When a poster is clicked, search across all extensions
        val title = url.removePrefix("simkl:") // Our encoded id/title
        val results = AppUtils.search(title, TvType.Movie) // search both movie/series
        return results.firstOrNull()
    }
}

// ---- Models ----
data class SimklResponse(
    val items: List<SimklItem>
)

data class SimklItem(
    val show: SimklShow?,
    val movie: SimklMovie?,
    val status: String?
) {
    fun toSearchResponse(): SearchResponse {
        val title = show?.title ?: movie?.title ?: "Unknown"
        val poster = show?.poster ?: movie?.poster
        return newMovieSearchResponse(title, "simkl:$title", TvType.Movie) {
            this.posterUrl = poster
        }
    }
}

data class SimklShow(val title: String?, val poster: String?)
data class SimklMovie(val title: String?, val poster: String?)
