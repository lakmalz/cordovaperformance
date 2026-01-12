/**
 * Cordova Performance App with Fragment-based Navigation
 * Uses singleton WebView with view switching via fragment identifiers
 * No page reloads - all navigation happens within the same DOM
 */

var app = {
    currentView: 'home',

    // Application Constructor
    initialize: function() {
        console.log('App initializing...');
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
        
        // Initialize navigation immediately (works even before deviceready)
        this.initializeNavigation();
    },

    // deviceready Event Handler
    onDeviceReady: function() {
        console.log('Device is ready!');
        this.receivedEvent('deviceready');
        
        // Set up back button handler for Android
        document.addEventListener("backbutton", this.onBackButton.bind(this), false);
    },

    // Update DOM on a Received Event
    receivedEvent: function(id) {
        console.log('Received Event: ' + id);
    },

    // Initialize navigation system
    initializeNavigation: function() {
        console.log('Initializing navigation...');
        
        // Handle hash change events
        window.addEventListener('hashchange', this.handleHashChange.bind(this));
        
        // Check initial hash
        var hash = window.location.hash.substring(1);
        if (hash) {
            this.showView(hash);
        } else {
            this.showView('home');
        }
        
        // Expose navigation function globally for native calls
        window.navigateToView = this.navigateToView.bind(this);
        window.handleBackButton = this.onBackButton.bind(this);
        
        console.log('Navigation initialized. Current view: ' + this.currentView);
    },

    // Handle hash change
    handleHashChange: function() {
        var hash = window.location.hash.substring(1);
        if (hash && hash !== this.currentView) {
            this.showView(hash);
        }
    },

    // Navigate to a view (can be called from native or web)
    navigateToView: function(viewName) {
        console.log('Navigating to: ' + viewName);
        
        // Update hash (this will trigger hashchange event)
        window.location.hash = viewName;
    },

    // Show a specific view
    showView: function(viewName) {
        console.log('Showing view: ' + viewName);
        
        // Hide all views
        var views = document.querySelectorAll('.view');
        views.forEach(function(view) {
            view.classList.remove('active');
        });
        
        // Show target view
        var targetView = document.getElementById('view-' + viewName);
        if (targetView) {
            targetView.classList.add('active');
            this.currentView = viewName;
            
            // Log performance - view switch without reload!
            console.log('View switched to: ' + viewName + ' (NO RELOAD)');
        } else {
            console.error('View not found: ' + viewName);
        }
    },

    // Handle back button
    onBackButton: function() {
        console.log('Back button pressed. Current view: ' + this.currentView);
        
        if (this.currentView !== 'home') {
            // Navigate back to home
            this.navigateToView('home');
        } else {
            // On home screen, exit app
            if (navigator.app && navigator.app.exitApp) {
                navigator.app.exitApp();
            }
        }
    }
};

// Initialize app
app.initialize();

// Performance logging
console.log('=== PERFORMANCE MODE ===');
console.log('WebView preloaded and reused - NO RELOADS!');
console.log('Navigation uses fragment identifiers (#home, #settings, #profile)');
console.log('========================');
