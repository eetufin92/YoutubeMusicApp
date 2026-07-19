package com.eetu.youtubemusicapp.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    val isSystemDarkTheme = isSystemInDarkTheme()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isDarkThemePage by remember { mutableStateOf(true) }

    val systemBackgroundColor = MaterialTheme.colorScheme.background
    val backgroundColor = if (isDarkThemePage) Color.Black else Color.White
    val webViewBackgroundColor = if (isDarkThemePage) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            factory = { ctx ->
                object : WebView(ctx) {
                    override fun onWindowVisibilityChanged(visibility: Int) {
                        if (visibility != android.view.View.GONE) {
                            super.onWindowVisibilityChanged(visibility)
                        }
                    }
                }.apply {
                    setBackgroundColor(webViewBackgroundColor)
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
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (shouldRedirectToMusic(url)) {
                                view?.loadUrl(getRedirectedUrl(url))
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            errorMessage = null

                            isDarkThemePage = url != null && Uri.parse(url).host?.equals("music.youtube.com", ignoreCase = true) == true

                            if (url != null && shouldRedirectToMusic(url)) {
                                view?.loadUrl(getRedirectedUrl(url))
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            injectScripts(view, isSystemDarkTheme)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            injectScripts(view, isSystemDarkTheme)
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
            update = { webView ->
                webView.setBackgroundColor(webViewBackgroundColor)
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
                            val currentUrl = webViewInstance?.url
                            if (currentUrl.isNullOrEmpty() || currentUrl == "about:blank") {
                                webViewInstance?.loadUrl("https://music.youtube.com")
                            } else {
                                webViewInstance?.reload()
                            }
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

private fun injectScripts(webView: WebView?, isSystemDarkTheme: Boolean) {
    val css = """
        ytm-mealbar-promo-renderer,
        .ytm-mealbar-promo-renderer,
        ytm-upsell-dialog-renderer,
        .ytm-upsell-dialog-renderer,
        ytm-smart-app-banner,
        .ytm-smart-app-banner,
        .ytm-app-upsell,
        .music-app-upsell,
        #web-player-app-install-banner,
        ytmusic-app-promo-banner,
        .ytmusic-app-promo-banner,
        ytmusic-mealbar-promo-renderer,
        .ytmusic-mealbar-promo-renderer,
        ytmusic-upsell-dialog-renderer,
        .ytmusic-upsell-dialog-renderer,
        #app-install-banner,
        .app-install-banner,
        #promotion-app,
        .promotion-app,
        .upsell-banner,
        #promotion-banner,
        yt-app-launcher-banner,
        .yt-app-launcher-banner,
        ytm-app-install-banner,
        .ytm-app-install-banner,
        .mobile-topbar-header-content.ytd-app-promo,
        .ytd-app-promo,
        ytm-pwa-install-banner,
        .ytm-app-update-banner,
        .upsell-dialog-renderer,
        .modern-sharing-item-install-app,
        .ytm-pwa-install-banner-header,
        ytm-promoted-sparkles-web-renderer,
        ytm-brand-teaser-renderer,
        ytm-app-promo-renderer,
        .ytm-app-promo-renderer,
        ytm-item-section-renderer[section-identifier="app-promo"],
        .topbar-header-content .yt-spec-button-shape-next--call-to-action,
        .ytm-topbar-header-container .yt-spec-button-shape-next--call-to-action,
        .ytm-player-overlay-renderer .yt-spec-button-shape-next--call-to-action,
        .ytm-player-overlay-renderer .ytm-app-promo-renderer,
        [aria-label*="Open App"],
        [aria-label*="open app"],
        [aria-label*="Avaa sovellus"],
        [aria-label*="avaa sovellus"],
        [aria-label*="Avaa sovelluksessa"],
        [aria-label*="avaa sovelluksessa"],
        [aria-label*="Käytä sovellusta"],
        [aria-label*="käytä sovellusta"],
        .yt-spec-button-shape-next--call-to-action[aria-label*="App"],
        .yt-spec-button-shape-next--call-to-action[aria-label*="app"],
        .yt-spec-button-shape-next--call-to-action[aria-label*="Avaa"],
        .yt-spec-button-shape-next--call-to-action[aria-label*="avaa"],
        ytm-companion-ad-renderer,
        .ytp-overflow-button,
        .ytp-button[aria-label*="App"],
        .ytp-button[aria-label*="app"],
        .ytp-button[aria-label*="Avaa"],
        .ytp-button[aria-label*="avaa"],
        a[href*="play.google.com/store"],
        a[href*="market://"],
        a[href*="youtubemusic://"],
        a[href*="youtube://"],
        a[href^="intent://"],
        button[onclick*="intent://"] {
            display: none !important;
        }
    """.trimIndent().replace("\n", " ")

    val js = """
        (function() {
            if (window._promo_script_injected) return;
            window._promo_script_injected = true;

            // Pass system dark mode preference via dark attribute on html element
            if ($isSystemDarkTheme) {
                document.documentElement.setAttribute('dark', 'true');
            } else {
                document.documentElement.removeAttribute('dark');
            }

            // 1. Inject CSS using textContent to bypass Trusted Types policy
            var styleId = 'ytm-cosmetic-overrides';
            var style = document.getElementById(styleId);
            if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                style.textContent = '$css';
                (document.head || document.documentElement).appendChild(style);
            }
            
            // 2. Hide promos periodically (both CSS selectors and text-based, in English & Finnish)
            setInterval(function() {
                var selectors = [
                    'ytm-mealbar-promo-renderer', '.ytm-mealbar-promo-renderer',
                    'ytm-upsell-dialog-renderer', '.ytm-upsell-dialog-renderer',
                    'ytm-smart-app-banner', '.ytm-smart-app-banner',
                    '.ytm-app-upsell', '.music-app-upsell',
                    '#web-player-app-install-banner', 'ytmusic-app-promo-banner',
                    '.ytmusic-app-promo-banner', 'ytmusic-mealbar-promo-renderer',
                    '.ytmusic-mealbar-promo-renderer', 'ytmusic-upsell-dialog-renderer',
                    '.ytmusic-upsell-dialog-renderer', '#app-install-banner',
                    '.app-install-banner', '#promotion-app', '.promotion-app',
                    '.upsell-banner', '#promotion-banner', 'yt-app-launcher-banner',
                    '.yt-app-launcher-banner', 'ytm-app-install-banner',
                    '.ytm-app-install-banner', '.ytd-app-promo', 'ytm-pwa-install-banner'
                ];
                selectors.forEach(function(sel) {
                    var els = document.querySelectorAll(sel);
                    els.forEach(function(el) {
                        if (el.style.display !== 'none') {
                            el.style.setProperty('display', 'none', 'important');
                        }
                    });
                });

                // Check text buttons/links (English and Finnish variations)
                var tags = document.querySelectorAll('button, a, span.yt-core-attributed-string');
                tags.forEach(function(el) {
                    var text = el.textContent.trim().toLowerCase();
                    if (text === 'open app' || text === 'get app' || text === 'install app' || text === 'open in app' || text === 'get the app' ||
                        text === 'avaa' || text === 'avaa sovellus' || text === 'avaa sovelluksessa' || text === 'asenna' || text === 'lataa' || text === 'käytä sovellusta' || text === 'lataa sovellus' || text === 'hanki sovellus' ||
                        text.indexOf('open app') !== -1 || text.indexOf('avaa sovellus') !== -1 || text.indexOf('avaa sovelluksessa') !== -1 || text.indexOf('käytä sovellusta') !== -1) {
                        if (el.style.display !== 'none') {
                            el.style.setProperty('display', 'none', 'important');
                        }
                        var parent = el.parentElement;
                        if (parent && (parent.tagName === 'YTM-APP-PROMO-RENDERER' || parent.classList.contains('ytm-app-promo-renderer') || parent.classList.contains('app-promo') || parent.classList.contains('ytd-app-promo'))) {
                            parent.style.setProperty('display', 'none', 'important');
                        }
                    }
                });
            }, 1000);

            // 3. Sync Playback and Metadata
            var lastTitle = "";
            var lastState = "";
            var lastUpdate = 0;
            var lastIsAd = false;

            var sync = function() {
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
                var playerBar = document.querySelector('ytmusic-player-bar, ytm-player-bar, ytm-player-bar-renderer, #player-bar, .ytmusic-player-bar');
                
                if (playerBar && !isAd) {
                    if (isGenericTitle) {
                        var titleEl = playerBar.querySelector('.title, .song-title, [class*="title"]');
                        if (titleEl) title = titleEl.textContent.trim();
                    }
                    if (!artist) {
                        var artistEl = playerBar.querySelector('.byline, .artist, .subtitle, a');
                        if (artistEl) artist = artistEl.textContent.trim();
                    }
                    if (!artwork) {
                        var imgEl = playerBar.querySelector('img, #song-image img');
                        if (imgEl) artwork = imgEl.src;
                    }
                }
                
                if (!isAd && (isGenericTitle || !title || title === "YouTube Music" || title === "Home")) {
                    // Use document.title as last resort
                    var docTitle = document.title || "";
                    if (docTitle && docTitle !== "YouTube Music" && docTitle !== "Home") {
                        var parts = docTitle.split(" - ");
                        if (parts.length > 0 && parts[0] !== "YouTube Music" && parts[0] !== "Home") {
                            title = parts[0];
                        }
                    }
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

private fun shouldRedirectToMusic(url: String): Boolean {
    val uri = Uri.parse(url)
    val host = uri.host ?: return false
    val path = uri.path ?: ""

    // Exclude cookie consent and account subdomains to prevent redirection loops
    if (host.equals("consent.youtube.com", ignoreCase = true) ||
        host.equals("accounts.youtube.com", ignoreCase = true)) {
        return false
    }

    // Check if host is a YouTube domain but NOT YouTube Music
    val isYouTubeHost = (host == "youtube.com" || host.endsWith(".youtube.com")) &&
            !host.equals("music.youtube.com", ignoreCase = true)

    if (!isYouTubeHost) return false

    // Ignore sign-in/auth/consent flows so they can complete normally
    val excludePaths = listOf(
        "/signin",
        "/accounts",
        "/logout",
        "/signout",
        "/o/oauth2",
        "/upgrade",
        "/co/"
    )

    return excludePaths.none { path.contains(it, ignoreCase = true) }
}

private fun getRedirectedUrl(url: String): String {
    val uri = Uri.parse(url)
    val path = uri.path ?: ""

    // Preserve key video, playlist, and channel paths if they match YouTube Music equivalents
    val preservePath = path.startsWith("/watch") ||
            path.startsWith("/playlist") ||
            path.startsWith("/channel") ||
            path.startsWith("/@")

    return if (preservePath) {
        uri.buildUpon()
            .authority("music.youtube.com")
            .build()
            .toString()
    } else {
        "https://music.youtube.com"
    }
}
