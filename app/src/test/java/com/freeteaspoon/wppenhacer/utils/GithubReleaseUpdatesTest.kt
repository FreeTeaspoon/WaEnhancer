package com.freeteaspoon.wppenhacer.utils

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GithubReleaseUpdatesTest {
    @Test fun numericVersionChangesNeedNoCompareRequest() {
        val client = OkHttpClient.Builder().addInterceptor { error("Unexpected network request") }.build()
        assertTrue(GithubReleaseUpdates.isUpdateAvailable("1.6.5 (ffffffff)", "1.6.6-00000000", client, "owner/repo"))
        assertFalse(GithubReleaseUpdates.isUpdateAvailable("1.6.6 (00000000)", "1.6.5-ffffffff", client, "owner/repo"))
        assertFalse(GithubReleaseUpdates.isUpdateAvailable("1.6.6 (ABCDEF12)", "1.6.6-abcdef12", client, "owner/repo"))
        assertFalse(GithubReleaseUpdates.isUpdateAvailable("unknown", "invalid", client, "owner/repo"))
    }

    @Test fun comparisonUsesInstalledCommitAsBaseAndFailsClosed() {
        for ((status, code) in listOf("ahead" to 200, "behind" to 200, "diverged" to 200, "ahead" to 403, "ahead" to 404)) {
            var requestedPath: String? = null
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                requestedPath = chain.request().url.encodedPath
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(code).message("test").body("""{"status":"$status"}""".toResponseBody()).build()
            }.build()
            assertEquals(code == 200 && status == "ahead", GithubReleaseUpdates.isUpdateAvailable(
                "1.6.5 (F4DCF9C5)", "1.6.5-c35a30a6", client, "FreeTeaspoon/WaEnhancer"
            ))
            assertEquals("/repos/FreeTeaspoon/WaEnhancer/compare/f4dcf9c5...c35a30a6", requestedPath)
        }
    }
}
