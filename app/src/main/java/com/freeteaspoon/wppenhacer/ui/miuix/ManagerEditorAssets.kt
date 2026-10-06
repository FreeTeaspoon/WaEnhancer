package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

internal const val MANAGER_EDITOR_BASE_URL = "https://appassets.androidplatform.net/assets/"

internal class ManagerEditorAssets(context: Context) : WebViewClient() {
    private val assets = WebViewAssetLoader.AssetsPathHandler(context)
    private val loader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/") { path ->
            assets.handle(path)?.apply {
                // WebView enforces a JavaScript MIME type for ES modules.
                if (path.endsWith(".mjs")) mimeType = "text/javascript"
            }
        }
        .build()

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        // loadDataWithBaseURL supplies the document as data:, which WebView must handle itself.
        if (request.url.scheme == "data") null
        else loader.shouldInterceptRequest(request.url)
            ?: WebResourceResponse("text/plain", "UTF-8", 404, "Not found", emptyMap(), "".byteInputStream())
}
