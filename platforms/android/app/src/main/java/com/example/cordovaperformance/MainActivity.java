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
import android.widget.FrameLayout;

import org.apache.cordova.*;
import org.apache.cordova.engine.SystemWebView;

/**
 * MainActivity - Uses the preloaded singleton WebView
 * No reloading of web content, just reusing the already loaded WebView
 */
public class MainActivity extends CordovaActivity
{
    private static final String TAG = "MainActivity";
    private WebViewManager webViewManager;
    private SystemWebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get the singleton WebView manager
        webViewManager = WebViewManager.getInstance();

        // Use the preloaded WebView instead of loading again
        if (webViewManager.isPreloaded()) {
            Log.d(TAG, "Using preloaded WebView - No reload!");
            usePreloadedWebView();
        } else {
            // Fallback: If somehow WebView wasn't preloaded, load it now
            Log.w(TAG, "WebView not preloaded, loading now...");
            super.onCreate(savedInstanceState);
            loadUrl(launchUrl);
        }
    }

    private void usePreloadedWebView() {
        // Get the preloaded WebView
        webView = webViewManager.getWebView();

        if (webView != null) {
            // Detach from previous parent
            webViewManager.detachWebView();

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

            // Navigate to home view by default
            webViewManager.navigateToView("home");

            Log.d(TAG, "Preloaded WebView attached to MainActivity");
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
