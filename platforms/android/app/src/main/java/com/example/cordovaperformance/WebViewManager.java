package com.example.cordovaperformance;

import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;
import java.util.ArrayList;

/**
 * Singleton WebViewManager for performance optimization
 * Manages a single WebView instance throughout the app lifecycle
 * Preloads content and uses fragment identifiers for navigation
 */
public class WebViewManager {
    private static final String TAG = "WebViewPerformance";
    private static WebViewManager instance;
    private CordovaWebView cordovaWebView;
    private SystemWebView systemWebView;
    private boolean isPreloaded = false;
    private String baseUrl = "file:///android_asset/www/index.html";
    
    // Performance tracking
    private long preloadStartTime = 0;
    private long preloadEndTime = 0;

    private WebViewManager() {
        // Private constructor for singleton
    }

    public static synchronized WebViewManager getInstance() {
        if (instance == null) {
            instance = new WebViewManager();
        }
        return instance;
    }

    /**
     * Initialize and preload the WebView with default index.html
     */
    public void preloadWebView(AppCompatActivity activity) {
        preloadWebView(activity, null);
    }

    /**
     * Initialize and preload the WebView with custom URL
     * @param activity The activity context
     * @param url Custom URL to load, if null uses default baseUrl
     */
    public void preloadWebView(AppCompatActivity activity, String url) {
        if (isPreloaded && cordovaWebView != null) {
            Log.d(TAG, "⚡ WebView already preloaded - reusing existing instance");
            return;
        }

        // Start timing
        preloadStartTime = System.currentTimeMillis();
        Log.d(TAG, "🚀 Starting WebView preload...");

        try {
            // Use custom URL if provided, otherwise use default
            String urlToLoad = (url != null && !url.isEmpty()) ? url : baseUrl;
            Log.d(TAG, "📄 URL to load: " + urlToLoad);
            
            long webViewCreateStart = System.currentTimeMillis();
            // Create SystemWebView with Activity context
            systemWebView = new SystemWebView(activity);
            long webViewCreateEnd = System.currentTimeMillis();
            Log.d(TAG, "⏱️  WebView creation time: " + (webViewCreateEnd - webViewCreateStart) + "ms");
            
            long settingsStart = System.currentTimeMillis();
            // Configure WebView settings for performance
            WebSettings settings = systemWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            long settingsEnd = System.currentTimeMillis();
            Log.d(TAG, "⏱️  Settings configuration time: " + (settingsEnd - settingsStart) + "ms");
            
            long cordovaInitStart = System.currentTimeMillis();
            // Create Cordova WebView with Activity
            SystemWebViewEngine webViewEngine = new SystemWebViewEngine(systemWebView);
            CordovaInterfaceImpl cordovaInterface = new CordovaInterfaceImpl(activity);
            cordovaWebView = new CordovaWebViewImpl(webViewEngine);
            
            // Initialize with required parameters
            CordovaPreferences preferences = new CordovaPreferences();
            ArrayList<PluginEntry> pluginEntries = new ArrayList<>();
            cordovaWebView.init(cordovaInterface, pluginEntries, preferences);
            long cordovaInitEnd = System.currentTimeMillis();
            Log.d(TAG, "⏱️  Cordova initialization time: " + (cordovaInitEnd - cordovaInitStart) + "ms");

            long loadUrlStart = System.currentTimeMillis();
            // Load the URL
            cordovaWebView.loadUrlIntoView(urlToLoad, false);
            long loadUrlEnd = System.currentTimeMillis();
            Log.d(TAG, "⏱️  LoadUrl call time: " + (loadUrlEnd - loadUrlStart) + "ms");
            
            isPreloaded = true;
            preloadEndTime = System.currentTimeMillis();
            
            long totalTime = preloadEndTime - preloadStartTime;
            Log.d(TAG, "✅ WebView preload completed!");
            Log.d(TAG, "📊 Total preload time: " + totalTime + "ms");
            Log.d(TAG, "========================================");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during WebView preload", e);
            e.printStackTrace();
        }
    }

    /**
     * Get the total preload time in milliseconds
     */
    public long getPreloadTime() {
        if (preloadStartTime > 0 && preloadEndTime > 0) {
            return preloadEndTime - preloadStartTime;
        }
        return 0;
    }

    /**
     * Get the preloaded WebView instance
     */
    public CordovaWebView getCordovaWebView() {
        return cordovaWebView;
    }

    /**
     * Get the system WebView for adding to layouts
     */
    public SystemWebView getSystemWebView() {
        return systemWebView;
    }

    /**
     * Get WebView (for backward compatibility)
     */
    public SystemWebView getWebView() {
        return systemWebView;
    }

    /**
     * Navigate to a different view using fragment identifier
     */
    public void navigateToView(String fragmentId) {
        if (systemWebView != null) {
            String javascript = "window.location.hash = '" + fragmentId + "';";
            systemWebView.evaluateJavascript(javascript, null);
        }
    }

    /**
     * Navigate using direct JavaScript call
     */
    public void executeJavaScript(String script) {
        if (systemWebView != null) {
            systemWebView.evaluateJavascript(script, null);
        }
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
        if (systemWebView != null && systemWebView.getParent() != null) {
            ((ViewGroup) systemWebView.getParent()).removeView(systemWebView);
        }
    }

    /**
     * Reset WebView to home view
     */
    public void resetToHome() {
        navigateToView("home");
    }

    /**
     * Clear WebView on logout
     */
    public void clearWebView() {
        if (systemWebView != null) {
            systemWebView.clearCache(true);
            systemWebView.clearHistory();
            systemWebView.clearFormData();
            systemWebView.loadUrl("about:blank");
        }
        isPreloaded = false;
        cordovaWebView = null;
        systemWebView = null;
        instance = null;
    }

    /**
     * Destroy the WebView instance (for logout or app kill)
     */
    public void destroy() {
        clearWebView();
    }
}
