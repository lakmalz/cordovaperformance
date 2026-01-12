package com.example.cordovaperformance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.cordova.engine.SystemWebView;

/**
 * LoginActivity - Entry point of the application
 * Preloads WebView for performance optimization
 * Shows the preloaded WebView in a FrameLayout for development purposes
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginPerformance";
    private FrameLayout webViewContainer;
    private Button btnLogin;
    private WebViewManager webViewManager;
    
    // Performance tracking
    private long activityStartTime = 0;
    private long displayStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityStartTime = System.currentTimeMillis();
        Log.d(TAG, "========================================");
        Log.d(TAG, "🏁 LoginActivity onCreate started");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webViewContainer = findViewById(R.id.webViewContainer);
        btnLogin = findViewById(R.id.btnLogin);

        long layoutInflateTime = System.currentTimeMillis() - activityStartTime;
        Log.d(TAG, "⏱️  Layout inflation time: " + layoutInflateTime + "ms");

        // Initialize and preload WebView
        initializeWebView();

        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "🔄 Navigating to MainActivity...");
                // Navigate to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                // Don't finish LoginActivity yet for dev purposes
                // finish();
            }
        });
        
        long totalActivityTime = System.currentTimeMillis() - activityStartTime;
        Log.d(TAG, "✅ LoginActivity onCreate completed: " + totalActivityTime + "ms");
        Log.d(TAG, "========================================");
    }

    private void initializeWebView() {
        long initStart = System.currentTimeMillis();
        Log.d(TAG, "🌐 Initializing WebView...");
        
        // Get singleton instance
        webViewManager = WebViewManager.getInstance();

        // Preload WebView if not already preloaded
        if (!webViewManager.isPreloaded()) {
            // Option 1: Use default URL (file:///android_asset/www/index.html)
            webViewManager.preloadWebView(this);
            
            // Option 2: Pass custom URL if needed
            // webViewManager.preloadWebView(this, "file:///android_asset/www/custom.html");
        }

        // Show the preloaded WebView in the container for dev purposes
        displayPreloadedWebView();
        
        long initEnd = System.currentTimeMillis();
        long totalWebViewTime = initEnd - initStart;
        Log.d(TAG, "⏱️  Total WebView initialization + display: " + totalWebViewTime + "ms");
        
        // Get and log the preload time from WebViewManager
        long preloadTime = webViewManager.getPreloadTime();
        if (preloadTime > 0) {
            Log.d(TAG, "📈 WebView preload time: " + preloadTime + "ms");
        }
    }

    private void displayPreloadedWebView() {
        displayStartTime = System.currentTimeMillis();
        Log.d(TAG, "🎨 Displaying preloaded WebView...");
        
        // Get the preloaded WebView
        WebView webView = webViewManager.getSystemWebView();

        if (webView != null) {
            // Detach from previous parent if any
            webViewManager.detachWebView();

            // Add to container
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            webViewContainer.addView(webView, params);

            // Navigate to home view (default)
            webViewManager.navigateToView("home");
            
            long displayTime = System.currentTimeMillis() - displayStartTime;
            Log.d(TAG, "⏱️  WebView display time: " + displayTime + "ms");
            Log.d(TAG, "✅ WebView attached and visible");
        } else {
            Log.e(TAG, "❌ WebView is null - cannot display");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-attach WebView if returning to this activity
        if (webViewManager != null && webViewManager.getWebView() != null) {
            displayPreloadedWebView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detach WebView when leaving activity (will be reused in MainActivity)
        if (webViewManager != null) {
            webViewManager.detachWebView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't destroy WebView here - it needs to persist
        // Only detach it
        if (webViewManager != null) {
            webViewManager.detachWebView();
        }
    }
}
