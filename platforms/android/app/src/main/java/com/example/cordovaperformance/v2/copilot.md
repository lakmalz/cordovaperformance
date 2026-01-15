Understood! Let me correct the architecture. `LoginActivity` should remain a pure native Android activity (extending `AppCompatActivity`), and only `MainActivity` extends `CordovaActivity`.

Here's the corrected approach:

## 1. Keep CordovaActivity with WebViewManager integration:

````java
// ...existing code...

public class CordovaActivity extends AppCompatActivity {
    
    public static String TAG = "CordovaActivity";
    public static final String BACKGROUND_COLOR = "#FFFFFF";
    
    // The WebView for our app
    protected CordovaWebView appView;
    protected ProgressBar progressBar;
    
    // Singleton WebView Manager
    protected WebViewManager webViewManager;
    protected boolean useSingletonWebView = true; // Flag to enable/disable singleton pattern
    
    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;
    
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
        splashScreen = SplashScreen.installSplashScreen(this);
        
        // Initialize WebViewManager singleton
        webViewManager = WebViewManager.getInstance();
        
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
        
        cordovaInterface = makeCordovaInterface();
        if (savedInstanceState != null) {
            cordovaInterface.restoreInstanceState(savedInstanceState);
        }
        
        cordovaActivityFlavorDelegate = new CordovaActivityFlavorDelegateImpl(this);
    }
    
    protected void init() {
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                (int) pxToDp(80), (int) pxToDp(80), Gravity.CENTER));
        progressBar.getIndeterminateDrawable().setColorFilter(
                Color.parseColor("#3c5475"), android.graphics.PorterDuff.Mode.SRC_IN);
        
        // Use singleton WebView if enabled, otherwise create new one
        if (useSingletonWebView && webViewManager.isPreloaded()) {
            LOG.d(TAG, "Using singleton WebView");
            appView = webViewManager.getCordovaWebView();
            
            // Detach from previous parent if attached
            webViewManager.detachWebView();
        } else {
            LOG.d(TAG, "Creating new WebView instance");
            appView = makeWebView();
        }
        
        appView.getView().setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));
        createViews();
        
        if (!appView.isInitialized()) {
            appView.init(cordovaInterface, pluginEntries, preferences);
        }
        
        cordovaInterface.onCordovaInit(appView.getPluginManager());
        
        cordovaInterface.pluginManager.postMessage("setupSplashScreen", splashScreen);
        
        String volumePref = preferences.getString("DefaultVolumeStream", "");
        if ("media".equals(volumePref.toLowerCase(Locale.ENGLISH))) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }
    
    /**
     * Navigate to a view using fragment identifier
     */
    protected void navigateToView(String viewName) {
        if (useSingletonWebView) {
            webViewManager.navigateToView(viewName);
        } else {
            if (appView != null) {
                appView.loadUrl("javascript:navigateToView('" + viewName + "')");
            }
        }
    }
    
    /**
     * Execute JavaScript in the WebView
     */
    protected void executeJavaScript(String script) {
        if (useSingletonWebView) {
            webViewManager.executeJavaScript(script);
        } else {
            if (appView != null) {
                appView.loadUrl("javascript:" + script);
            }
        }
    }
    
    /**
     * Reset WebView to home view
     */
    protected void resetToHome() {
        if (useSingletonWebView) {
            webViewManager.resetToHome();
        } else {
            navigateToView("home");
        }
    }
    
    /**
     * Clear singleton WebView on logout
     */
    protected void clearSingletonWebView() {
        if (useSingletonWebView) {
            LOG.d(TAG, "Clearing singleton WebView");
            webViewManager.clearWebView();
        }
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
    
    // ...existing code for loadConfig, createViews, makeWebView, makeWebViewEngine, makeCordovaInterface...
    // ...existing code for setUpCordovaErrorHandler, loadUrl...
    // ...existing code for onPause, onResume, onStart, onStop, onDestroy, onNewIntent, onWindowFocusChanged, onMessage...

    @Override
    protected void onDestroy() {
        LOG.d(TAG, "CordovaActivity.onDestroy()");
        super.onDestroy();

        // Detach WebView but don't destroy it (for singleton pattern)
        if (useSingletonWebView && webViewManager.isPreloaded()) {
            webViewManager.detachWebView();
        } else if (this.appView != null) {
            appView.handleDestroy();
        }
    }
}
````

## 2. LoginActivity (Pure Native - extends AppCompatActivity):

````java
package com.example.poccordovax;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.cordova.LOG;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private FrameLayout webViewPreviewContainer;
    
    private WebViewManager webViewManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if already logged in
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }
        
        setContentView(R.layout.activity_login);
        
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        webViewPreviewContainer = findViewById(R.id.webview_preview_container);
        
        // Initialize WebViewManager singleton
        webViewManager = WebViewManager.getInstance();
        
        // Preload WebView in background after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            preloadWebView();
        }, 500);
        
        loginButton.setOnClickListener(v -> handleLogin());
    }
    
    private void preloadWebView() {
        if (!webViewManager.isPreloaded()) {
            LOG.d(TAG, "Preloading WebView from LoginActivity");
            webViewManager.preloadWebView(this);
            
            // Optionally show preview in container
            if (webViewPreviewContainer != null) {
                webViewPreviewContainer.addView(webViewManager.getSystemWebView());
            }
        }
    }
    
    private void handleLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simulate authentication
        if (username.equals("admin") && password.equals("admin")) {
            // Save session
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.createSession(username, "user123", "token_abc");
            
            LOG.d(TAG, "Login successful for user: " + username);
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
            
            // Navigate to MainActivity
            navigateToMainActivity();
        } else {
            LOG.d(TAG, "Login failed - Invalid credentials");
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Detach WebView preview if shown
        if (webViewPreviewContainer != null && webViewManager.isPreloaded()) {
            webViewManager.detachWebView();
        }
    }
}
````

## 3. MainActivity (extends CordovaActivity):

````java
package com.example.poccordovax;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.apache.cordova.LOG;

public class MainActivity extends CordovaActivity {
    
    private SessionManager sessionManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        // Initialize with singleton WebView
        init();
        loadUrl(launchUrl);
        
        // Navigate to home view by default
        resetToHome();
        
        LOG.d(TAG, "MainActivity initialized with user: " + sessionManager.getUserName());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_home) {
            navigateToView("home");
            return true;
        } else if (id == R.id.menu_settings) {
            navigateToView("settings");
            return true;
        } else if (id == R.id.menu_profile) {
            navigateToView("profile");
            return true;
        } else if (id == R.id.menu_logout) {
            handleLogout();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void handleLogout() {
        LOG.d(TAG, "User logging out: " + sessionManager.getUserName());
        
        // Clear session
        sessionManager.clearSession();
        
        // Clear singleton WebView
        clearSingletonWebView();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Execute JavaScript to check current view
        executeJavaScript("if (typeof getCurrentView === 'function') { getCurrentView(); } else { 'home'; }");
        
        // For now, navigate to home or exit
        resetToHome();
    }
}
````

## Key Points:

1. **LoginActivity** = Pure native (extends `AppCompatActivity`)
   - Handles native login UI
   - Preloads WebView in background using `WebViewManager`
   - No Cordova dependencies

2. **CordovaActivity** = Base class with WebViewManager integration
   - Provides WebView singleton methods
   - All Cordova activities extend this

3. **MainActivity** = Cordova app (extends `CordovaActivity`)
   - Uses the preloaded singleton WebView
   - Handles navigation and logout

This separation keeps your native login screen independent while allowing the Cordova part to benefit from the singleton WebView pattern!

Similar code found with 1 license type