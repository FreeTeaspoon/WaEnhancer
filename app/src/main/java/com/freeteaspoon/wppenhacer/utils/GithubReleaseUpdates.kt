package com.freeteaspoon.wppenhacer.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal object GithubReleaseUpdates {
    fun isUpdateAvailable(installedName: String?, releaseTag: String, client: OkHttpClient, repository: String): Boolean {
        val installed = ReleaseVersion.parse(installedName) ?: return false
        val release = ReleaseVersion.parse(releaseTag) ?: return false
        if (installed.compareTo(release) != 0 || installed.sameBuild(release)) {
            return installed.shouldUpdateTo(release)
        }
        val installedHash = installed.hash ?: return false
        val releaseHash = release.hash ?: return false
        // Same-version development builds may already contain the published release.
        // GitHub reports ancestry from the installed commit to the release commit.
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repository/compare/$installedHash...$releaseHash")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            installed.shouldUpdateTo(release, JSONObject(response.body.string()).optString("status"))
        }
    }
}
