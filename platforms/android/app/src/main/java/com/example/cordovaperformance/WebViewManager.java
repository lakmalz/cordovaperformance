package com.example.cordovaperformance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * Singleton WebView Manager for performance optimization
 * Preloads the web content once and reuses it throughout the application
 */
public class WebViewManager {
    private static final String TAG = "WebViewManager";
    private static WebViewManager instance;
    private SystemWebView webView;
    private SystemWebViewEngine webViewEngine;
    private CordovaWebView cordovaWebView;
    private boolean isPreloaded = false;
    private String baseUrl;

    private WebViewManager() {
    }

    public static synchronized WebViewManager getInstance() {
        if (instance == null) {
            instance = new WebViewManager();
        }
        return instance;
    }

    /**
     * Initialize and preload the WebView with the main content
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void preloadWebView(Context context, String url) {
        if (isPreloaded) {
            Log.d(TAG, "WebView already preloaded");
            return;
        }

        Log.d(TAG, "Preloading WebView...");
        this.baseUrl = url;

        // Create SystemWebView (Cordova's WebView)
        webView = new SystemWebView(context);
        
        // Configure WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Enable debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Create SystemWebViewEngine for proper Cordova integration
        webViewEngine = new SystemWebViewEngine(webView);

        // Set Cordova-compatible WebView clients
        webView.setWebViewClient(new SystemWebViewClient(webViewEngine) {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView preloading finished: " + url);
                isPreloaded = true;
            }
        });

        webView.setWebChromeClient(new SystemWebChromeClient(webViewEngine));

        // Load the main content
        webView.loadUrl(url);
    }

    /**
     * Get the preloaded WebView instance
     */
    public SystemWebView getWebView() {
        return webView;
    }

    /**
     * Check if WebView is preloaded
     */
    public boolean isPreloaded() {
        return isPreloaded;
    }

    /**
     * Detach WebView from current parent
     */
    public void detachWebView() {
        if (webView != null && webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
    }

    /**
     * Navigate to a specific view using fragment identifier
     */
    public void navigateToView(String viewName) {
        if (webView != null) {
            String jsCode = "if(window.navigateToView) { window.navigateToView('" + viewName + "'); }";
            webView.evaluateJavascript(jsCode, null);
            Log.d(TAG, "Navigating to: " + viewName);
        }
    }

    /**
     * Reload the WebView (use sparingly, defeats the purpose of singleton)
     */
    public void reload() {
        if (webView != null && baseUrl != null) {
            webView.loadUrl(baseUrl);
        }
    }

    /**
     * Cleanup when logging out or app is killed
     */
    public void destroy() {
        if (webView != null) {
            detachWebView();
            webView.destroy();
            webView = null;
        }
        isPreloaded = false;
        instance = null;
    }
}
