package com.example.cordovaperformance;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.splashscreen.SplashScreen;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginEntry;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Cordova 9.x Production Version with WebViewManager Integration
 */
public class CordovaActivity extends AppCompatActivity {
    
    public static String TAG = "CordovaActivity";
    public static final String BACKGROUND_COLOR = "#FFFFFF";
    
    // The WebView for our app
    protected CordovaWebView appView;
    protected ProgressBar progressBar;
    
    // WebViewManager for singleton WebView pattern
    protected WebViewManager webViewManager;
    
    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;
    
    // Keep app running when pause is received. (default = true)
    // If true, then the Javascript and native code continue to run in the background 
    // when another application (activity) is started.
    protected boolean keepRunning = true;
    
    protected boolean immersiveMode;
    
    // Read from config.xml:
    protected CordovaPreferences preferences;
    public String launchUrl;
    protected ArrayList<PluginEntry> pluginEntries;
    protected CordovaInterfaceImpl cordovaInterface;
    
    protected CordovaErrorHandler cordovaErrorHandler;
    
    private SplashScreen splashScreen;
    private ICordovaActivityFlavorDelegate cordovaActivityFlavorDelegate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        splashScreen = SplashScreen.installSplashScreen(this);
        
        // need to activate preferences before super.onCreate to avoid "requestFeature() must be called before adding content" exception
        loadConfig();
        
        String logLevel = preferences.getString("LogLevel", "ERROR");
        LOG.setLogLevel(logLevel);
        LOG.i(TAG, "Apache Cordova native platform version " + CordovaWebView.CORDOVA_VERSION + " is starting");
        LOG.d(TAG, "CordovaActivity.onCreate()");
        
        if (!preferences.getBoolean("ShowTitle", false)) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        
        if (preferences.getBoolean("SetFullscreen", false)) {
            LOG.d(TAG, "The SetFullscreen configuration is deprecated in favor of Fullscreen, and will be removed in a future version.");
            preferences.set("Fullscreen", true);
        }
        
        if (preferences.getBoolean("Fullscreen", false)) {
            // NOTE: Use the FullscreenNotImmersive configuration key to set the activity in a REAL fullscreen
            // (as was the case in previous cordova versions)
            if (preferences.getBoolean("FullscreenNotImmersive", false)) {
                immersiveMode = true;
                setImmersiveVisibility();
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        
        super.onCreate(savedInstanceState);
        
        // Initialize WebViewManager singleton
        webViewManager = WebViewManager.getInstance();
        LOG.d(TAG, "📦 WebViewManager initialized (isPreloaded: " + webViewManager.isPreloaded() + ")");
        
        cordovaInterface = makeCordovaInterface();
        if (savedInstanceState != null) {
            cordovaInterface.restoreInstanceState(savedInstanceState);
        }
        
        // Delegate for flavor based implementations
        cordovaActivityFlavorDelegate = new CordovaActivityFlavorDelegateImpl(this);
    }
    
    protected void init() {
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                (int) pxToDp(80), (int) pxToDp(80), Gravity.CENTER));
        progressBar.getIndeterminateDrawable().setColorFilter(
                Color.parseColor("#3c5475"), android.graphics.PorterDuff.Mode.SRC_IN);
        
        appView = makeWebView();
        appView.getView().setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));
        createViews();
        
        if (!appView.isInitialized()) {
            appView.init(cordovaInterface, pluginEntries, preferences);
        }
        
        cordovaInterface.onCordovaInit(appView.getPluginManager());
        
        // Hook: Notify subclass that WebView is ready
        onWebViewInitialized();
        
        // Setup the splash screen based on preference settings
        cordovaInterface.pluginManager.postMessage("setupSplashScreen", splashScreen);
        
        // Wire the hardware volume controls to control media if desired.
        String volumePref = preferences.getString("DefaultVolumeStream", "");
        if ("media".equals(volumePref.toLowerCase(Locale.ENGLISH))) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }
    
    /**
     * Hook method called after WebView is initialized.
     * Override this in MainActivity for post-initialization setup.
     */
    protected void onWebViewInitialized() {
        // Default: do nothing
    }
    
    protected void loadConfig() {
        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(this);
        preferences = parser.getPreferences();
        preferences.setPreferencesBundle(getIntent().getExtras());
        launchUrl = parser.getLaunchUrl();
        pluginEntries = parser.getPluginEntries();
        Config.parser = parser;
    }

    protected void createViews() {
        // Why are we setting a constant as the ID? This should be investigated
        appView.getView().setId(100);
        appView.getView().setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        appView.getView().setVisibility(View.INVISIBLE);
        
        CoordinatorLayout coordinatorLayout = new CoordinatorLayout(this);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        frameLayout.addView(appView.getView());
        frameLayout.addView(progressBar);
        cordovaActivityFlavorDelegate.addViewsByFlavor(frameLayout);
        
        coordinatorLayout.setFitsSystemWindows(true);
        coordinatorLayout.addView(frameLayout);
        setContentView(coordinatorLayout);
        
        if (preferences.contains("BackgroundColor")) {
            try {
                int backgroundColor = preferences.getInteger("BackgroundColor", Color.BLACK);
                // Background of activity:
                appView.getView().setBackgroundColor(backgroundColor);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        
        appView.getView().requestFocusFromTouch();
    }

    protected CordovaWebView makeWebView() {
        // Try to use preloaded WebView from WebViewManager first
        if (webViewManager != null && webViewManager.isPreloaded()) {
            CordovaWebView preloadedWebView = webViewManager.getCordovaWebView();
            if (preloadedWebView != null) {
                LOG.d(TAG, "⚡ Using preloaded WebView from WebViewManager");
                // Detach from previous parent before reuse
                webViewManager.detachWebView();
                return preloadedWebView;
            }
        }
        
        // Hook: Check if subclass wants to provide a preloaded WebView
        CordovaWebView preloadedWebView = getPreloadedWebView();
        if (preloadedWebView != null) {
            LOG.d(TAG, "Using preloaded WebView from subclass");
            return preloadedWebView;
        }
        
        // Default: Create new WebView (Cordova 9.x uses flavor delegate)
        LOG.d(TAG, "📦 Creating new WebView (no preload available)");
        return cordovaActivityFlavorDelegate.makeWebViewByFlavor();
    }
    
    /**
     * Hook method for subclasses to provide a preloaded WebView.
     * Override this in MainActivity to integrate with WebViewManager.
     * 
     * @return Preloaded CordovaWebView or null to create a new one
     */
    protected CordovaWebView getPreloadedWebView() {
        return null; // Default: no preloaded WebView
    }
    
    protected CordovaWebViewEngine makeWebViewEngine() {
        return CordovaWebViewImpl.createEngine(this, preferences);
    }
    
    protected CordovaInterfaceImpl makeCordovaInterface() {
        return new CordovaInterfaceImpl(this) {
            @Override
            public Object onMessage(String id, Object data) {
                // Plumb this to CordovaActivity.onMessage for backwards compatibility
                return CordovaActivity.this.onMessage(id, data);
            }
        };
    }
    
    public void setUpCordovaErrorHandler(CordovaErrorHandler cordovaErrorHandler) {
        this.cordovaErrorHandler = cordovaErrorHandler;
    }

    public void loadUrl(String url) {
        LOG.d(TAG, "Loading URL: " + url);
        if (appView == null) {
            init();
        }

        this.keepRunning = preferences.getBoolean("KeepRunning", true);
        appView.loadUrlIntoView(url, true);
    }
    
    /**
     * Convert pixels to DP
     */
    protected float pxToDp(int px) {
        return px / getResources().getDisplayMetrics().density;
    }
    
    /**
     * Set immersive mode for fullscreen
     */
    protected void setImmersiveVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LOG.d(TAG, "Paused the activity.");

        if (this.appView != null) {
            // CB-9382 If there is an activity that started for result and main activity is waiting for callback
            // result, we shouldn't stop WebView Javascript timers, as activity for result might be using them
            boolean keepRunning = this.keepRunning || this.cordovaInterface.activityResultCallback != null;
            this.appView.handlePause(keepRunning);
        }
        
        // Detach WebView from parent but keep it in memory for reuse
        if (webViewManager != null && webViewManager.isPreloaded()) {
            webViewManager.detachWebView();
            LOG.d(TAG, "🔓 WebView detached (persists for reuse)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LOG.d(TAG, "Resumed the activity.");

        if (this.appView != null) {
            this.appView.handleResume(this.keepRunning);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOG.d(TAG, "Started the activity.");

        if (this.appView != null) {
            this.appView.handleStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LOG.d(TAG, "Stopped the activity.");

        if (this.appView != null) {
            this.appView.handleStop();
        }
    }

    @Override
    protected void onDestroy() {
        LOG.d(TAG, "CordovaActivity.onDestroy()");
        super.onDestroy();

        // Don't destroy WebView if it's managed by WebViewManager (singleton pattern)
        // Only destroy if WebViewManager is not being used
        if (webViewManager != null && webViewManager.isPreloaded()) {
            // Detach but don't destroy - WebView persists across activities
            webViewManager.detachWebView();
            LOG.d(TAG, "🔓 WebView detached (will persist)");
        } else if (this.appView != null) {
            // Normal destroy for non-singleton WebViews
            appView.handleDestroy();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Forward to plugins
        if (this.appView != null) {
            this.appView.onNewIntent(intent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && immersiveMode) {
            setImmersiveVisibility();
        }
    }

    /**
     * Called when a message is sent to plugin.
     *
     * @param id   The message id
     * @param data The message data
     * @return Object or null
     */
    public Object onMessage(String id, Object data) {
        if ("onReceivedError".equals(id)) {
            if (cordovaErrorHandler != null) {
                cordovaErrorHandler.handleError((JSONObject) data);
            }
        } else if ("exit".equals(id)) {
            finish();
        }
        return null;
    }
    
    /**
     * Navigate to a specific view using fragment identifier.
     * Uses WebViewManager for fragment-based navigation.
     * 
     * @param viewName The fragment identifier (e.g., "home", "settings", "profile")
     */
    public void navigateToView(String viewName) {
        if (webViewManager != null) {
            webViewManager.navigateToView(viewName);
            LOG.d(TAG, "📍 Navigated to: " + viewName);
        } else {
            LOG.w(TAG, "⚠️  WebViewManager not available for navigation");
        }
    }
    
    /**
     * Execute JavaScript in the WebView.
     * Uses WebViewManager if available, falls back to appView.
     * 
     * @param script JavaScript code to execute
     */
    public void executeJavaScript(String script) {
        if (webViewManager != null && webViewManager.isPreloaded()) {
            webViewManager.executeJavaScript(script);
        } else if (appView != null) {
            appView.loadUrl("javascript:" + script);
        }
    }
    
    /**
     * Clear and logout - destroys the singleton WebView.
     * Call this when user logs out to ensure clean state.
     */
    public void logout() {
        if (webViewManager != null) {
            webViewManager.clearWebView();
            LOG.d(TAG, "🗑️  WebView destroyed on logout");
        }
        finish();
    }
    
    /**
     * Check if WebView is preloaded and ready to use.
     * 
     * @return true if WebView is preloaded, false otherwise
     */
    public boolean isWebViewPreloaded() {
        return webViewManager != null && webViewManager.isPreloaded();
    }

}