package com.eetu.youtubemusicapp.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.eetu.youtubemusicapp.service.WebAppInterface
import com.eetu.youtubemusicapp.service.WebPlayerProxy

/**
 * A WebView component optimized for YouTube Music.
 * It injects JavaScript and CSS to hide "Open App" banners and syncs state with Media3.
 * Includes custom loading and error states.
 */
@OptIn(UnstableApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView(
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        // Use a more standard mobile UA to ensure proper site behavior
                        userAgentString = userAgentString.replace("Version/\\d+\\.\\d+".toRegex(), "")
                        
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)
                        
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        
                        // Disable zoom as it can interfere with touch events on some sites
                        setSupportZoom(false)
                        displayZoomControls = false
                        builtInZoomControls = false

                        // Some web apps close menus if focus is lost during the tap.
                        // Forcing focus on tap can help.
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }

                    val playerProxy = WebPlayerProxy(this)
                    val jsInterface = WebAppInterface(playerProxy)
                    addJavascriptInterface(jsInterface, "AndroidMediaBridge")
                    
                    com.eetu.youtubemusicapp.service.PlaybackService.playerProxy = playerProxy

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            errorMessage = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            injectScripts(view)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            // Only handle main frame errors
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                errorMessage = error?.description?.toString() ?: "A network error occurred"
                            }
                        }
                    }
                    
                    webViewInstance = this
                    onWebViewCreated(this)
                    requestFocus()
                    loadUrl("https://music.youtube.com")
                }
            },
            update = {
                // No update logic needed for now
            }
        )

        // Custom Loading Screen
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tuning in...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Custom Error Screen
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Connection Interrupted",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            webViewInstance?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }

    // Cleanup when Composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            com.eetu.youtubemusicapp.service.PlaybackService.playerProxy = null
        }
    }
}

private fun injectScripts(webView: WebView?) {
    val css = """
        ytm-mealbar-promo-renderer,
        .ytm-mealbar-promo-renderer,
        ytm-upsell-dialog-renderer,
        .ytm-upsell-dialog-renderer,
        ytm-smart-app-banner,
        .ytm-smart-app-banner,
        .ytm-app-upsell,
        .music-app-upsell,
        #web-player-app-install-banner {
            display: none !important;
        }
    """.trimIndent().replace("\n", " ")

    val js = """
        (function() {
            // 1. Inject CSS using textContent to bypass Trusted Types policy
            var style = document.createElement('style');
            style.textContent = '$css';
            document.head.appendChild(style);
            
            // 2. Hide promos periodically - disabled as CSS should handle it
            // and DOM manipulations can interfere with menus
            /*
            setInterval(function() {
                var promos = document.querySelectorAll('ytm-mealbar-promo-renderer, ytm-upsell-dialog-renderer, ytm-smart-app-banner, .ytm-app-upsell');
                promos.forEach(function(p) { 
                    if (p.style.display !== 'none') p.style.display = 'none'; 
                });
            }, 3000);
            */

            // 3. Sync Playback and Metadata
            var lastTitle = "";
            var lastState = "";
            var lastUpdate = 0;
            var lastIsAd = false;

            var sync = function() {
                // If a menu is open, we might want to delay sync to prevent UI refresh
                if (document.querySelector('ytm-menu-renderer, .ytd-menu-popup-renderer')) {
                    return;
                }

                var video = document.querySelector('video');
                var adElement = document.querySelector('.ad-showing, .ad-interrupting');
                var isAd = !!adElement;
                
                if (!isAd) {
                    var adOverlay = document.querySelector('.ytmusic-ad-overlay, .ytp-ad-player-overlay');
                    if (adOverlay && adOverlay.offsetParent !== null) isAd = true;
                }
                
                // Secondary check: search for skip button or ad text
                if (!isAd) {
                    var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytmusic-skip-ad-button');
                    if (skipBtn && skipBtn.offsetParent !== null) isAd = true;
                }
                
                if (video) {
                    var currentState = video.paused + "_" + Math.floor(video.currentTime) + "_" + isAd;
                    if (currentState !== lastState) {
                        lastState = currentState;
                        AndroidMediaBridge.onPlaybackUpdate(
                            !video.paused, 
                            video.currentTime, 
                            video.duration || 0
                        );
                    }
                }

                var title = "";
                var artist = "";
                var artwork = "";
                var album = "";

                if (navigator.mediaSession && navigator.mediaSession.metadata) {
                    var meta = navigator.mediaSession.metadata;
                    title = meta.title || "";
                    artist = meta.artist || "";
                    album = meta.album || "";
                    artwork = meta.artwork && meta.artwork.length > 0 ? meta.artwork[meta.artwork.length - 1].src : "";
                }

                // Fallback to DOM scraping if metadata is empty or generic
                var isGenericTitle = !title || title === "Home" || title === "YouTube Music" || title === "Advertisement";
                if (isGenericTitle && !isAd) {
                    var titleEl = document.querySelector('ytm-player-bar .title, .ytmusic-player-bar .title, yt-formatted-string.title, .title.ytmusic-player-bar, .content-info-wrapper .title');
                    if (titleEl) title = titleEl.textContent.trim();
                    
                    if (!title || title === "YouTube Music") {
                        // Use document.title as last resort
                        var docTitle = document.title || "";
                        if (docTitle && docTitle !== "YouTube Music") {
                            var parts = docTitle.split(" - ");
                            if (parts.length > 0 && parts[0] !== "YouTube Music" && parts[0] !== "Home") {
                                title = parts[0];
                            }
                        }
                    }
                }
                
                if ((!artist || artist.includes("Video will play after ad")) && !isAd) {
                    var artistEl = document.querySelector('.ytmusic-player-bar .byline, .subtitle.ytmusic-player-bar, .byline.ytmusic-player-bar');
                    if (artistEl) artist = artistEl.textContent.trim();
                }
                
                if (!artwork && !isAd) {
                    var imgEl = document.querySelector('.ytmusic-player-bar img, #song-image img');
                    if (imgEl) artwork = imgEl.src;
                }

                if (isAd) {
                    title = "Advertisement";
                    artist = "YouTube Music";
                    // Try to get actual ad title if possible
                    var adTitle = document.querySelector('.ytp-ad-title-user-container, .video-ads .ytp-ad-text');
                    if (adTitle) artist = "Ad: " + adTitle.textContent.trim();
                }

                // Clean up title
                if (!isAd && (title === "Home" || title === "YouTube Music")) {
                    title = "";
                }

                var now = Date.now();
                var shouldUpdate = (title && title !== lastTitle) || (isAd !== lastIsAd) || (title && now - lastUpdate > 5000);
                
                if (shouldUpdate) {
                    lastTitle = title;
                    lastIsAd = isAd;
                    lastUpdate = now;
                    AndroidMediaBridge.onMetadataUpdate(
                        title,
                        artist,
                        album,
                        artwork
                    );
                }
            };

            setInterval(sync, 2000);
            
            var video = document.querySelector('video');
            if (video) {
                video.addEventListener('play', function() { setTimeout(sync, 100); });
                video.addEventListener('pause', function() { setTimeout(sync, 100); });
                video.addEventListener('ended', function() { setTimeout(sync, 100); });
            }
        })();
    """.trimIndent()
    
    webView?.evaluateJavascript(js, null)
}
