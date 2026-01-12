package com.example.cordovaperformance;

import android.content.Intent;
import android.os.Bundle;
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
    private static final String TAG = "LoginActivity";
    private FrameLayout webViewContainer;
    private Button btnLogin;
    private WebViewManager webViewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webViewContainer = findViewById(R.id.webViewContainer);
        btnLogin = findViewById(R.id.btnLogin);

        // Initialize and preload WebView
        initializeWebView();

        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                // Don't finish LoginActivity yet for dev purposes
                // finish();
            }
        });
    }

    private void initializeWebView() {
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
    }

    private void displayPreloadedWebView() {
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
