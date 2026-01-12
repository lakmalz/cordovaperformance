/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.example.cordovaperformance;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

/**
 * MainActivity - Uses the preloaded singleton WebView
 * No reloading of web content, just reusing the already loaded WebView
 */
public class MainActivity extends CordovaActivity
{
    private static final String TAG = "MainPerformance";
    private WebViewManager webViewManager;
    private SystemWebView webView;
    
    // Performance tracking
    private long activityStartTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        activityStartTime = System.currentTimeMillis();
        Log.d(TAG, "========================================");
        Log.d(TAG, "🏁 MainActivity onCreate started");
        
        super.onCreate(savedInstanceState);

        // Get the singleton WebView manager
        webViewManager = WebViewManager.getInstance();

        // Use the preloaded WebView instead of loading again
        if (webViewManager.isPreloaded()) {
            Log.d(TAG, "⚡ Using preloaded WebView - NO RELOAD!");
            usePreloadedWebView();
        } else {
            // Fallback: If somehow WebView wasn't preloaded, load it now
            Log.w(TAG, "⚠️  WebView not preloaded, loading now...");
            long fallbackStart = System.currentTimeMillis();
            super.onCreate(savedInstanceState);
            loadUrl(launchUrl);
            long fallbackTime = System.currentTimeMillis() - fallbackStart;
            Log.d(TAG, "⏱️  Fallback load time: " + fallbackTime + "ms");
        }
        
        long totalActivityTime = System.currentTimeMillis() - activityStartTime;
        Log.d(TAG, "✅ MainActivity onCreate completed: " + totalActivityTime + "ms");
        Log.d(TAG, "========================================");
    }

    private void usePreloadedWebView() {
        long reuseStart = System.currentTimeMillis();
        Log.d(TAG, "🔄 Reusing preloaded WebView...");
        
        // Get the preloaded WebView
        webView = webViewManager.getSystemWebView();

        if (webView != null) {
            long detachStart = System.currentTimeMillis();
            // Detach from previous parent
            webViewManager.detachWebView();
            long detachTime = System.currentTimeMillis() - detachStart;
            Log.d(TAG, "⏱️  Detach time: " + detachTime + "ms");

            long attachStart = System.currentTimeMillis();
            // Create a container for the WebView
            FrameLayout container = new FrameLayout(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );

            // Add WebView to container
            container.addView(webView, params);

            // Set the container as content view
            setContentView(container);
            long attachTime = System.currentTimeMillis() - attachStart;
            Log.d(TAG, "⏱️  Attach time: " + attachTime + "ms");

            // Navigate to home view by default
            webViewManager.navigateToView("settings");

            long totalReuseTime = System.currentTimeMillis() - reuseStart;
            Log.d(TAG, "⏱️  Total WebView reuse time: " + totalReuseTime + "ms");
            Log.d(TAG, "✅ Preloaded WebView attached to MainActivity");
            
            // Show performance comparison
            long preloadTime = webViewManager.getPreloadTime();
            if (preloadTime > 0) {
                Log.d(TAG, "📊 Performance Comparison:");
                Log.d(TAG, "   Initial preload: " + preloadTime + "ms");
                Log.d(TAG, "   Reuse in MainActivity: " + totalReuseTime + "ms");
                Log.d(TAG, "   🚀 Performance gain: " + (preloadTime - totalReuseTime) + "ms faster!");
            }
        } else {
            Log.e(TAG, "❌ WebView is null - cannot reuse");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If WebView was detached, reattach it
        if (webViewManager != null && webView != null && webView.getParent() == null) {
            usePreloadedWebView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detach but don't destroy - WebView persists
        if (webViewManager != null) {
            webViewManager.detachWebView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Don't destroy WebView - it needs to persist across activities
        // Only destroy on logout or app kill
        if (webViewManager != null) {
            webViewManager.detachWebView();
        }
    }

    @Override
    public void onBackPressed() {
        // Handle back button - navigate within WebView if possible
        if (webView != null) {
            webView.evaluateJavascript(
                "if(window.handleBackButton) { window.handleBackButton(); } else { window.history.back(); }", 
                null
            );
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Navigate to a specific view using fragment identifier
     */
    public void navigateToView(String viewName) {
        if (webViewManager != null) {
            webViewManager.navigateToView(viewName);
        }
    }

    /**
     * Call this when user logs out to destroy the WebView
     */
    public void logout() {
        if (webViewManager != null) {
            webViewManager.destroy();
        }
        finish();
    }
}
