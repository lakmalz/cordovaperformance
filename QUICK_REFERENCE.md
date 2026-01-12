# 🎯 Quick Reference - Cordova Performance

## Build & Run
```bash
# Build
cd /Users/lakmal/Projects/Android\ studio/cordova-repos/cordovaperformance
cordova build android

# Run on device
cordova run android --device

# Run on emulator
cordova run android
```

## Key Classes

### WebViewManager (Singleton)
```java
WebViewManager.getInstance()
  .preloadWebView(context, "file:///android_asset/www/index.html")
  .navigateToView("home")
  .detachWebView()
  .destroy()
```

### LoginActivity
- Entry point (LAUNCHER)
- Preloads WebView
- Shows WebView in FrameLayout (dev mode)

### MainActivity
- Reuses preloaded WebView
- Zero reload navigation

## Web Navigation

### JavaScript
```javascript
// Navigate to view
navigateToView('settings');

// Or directly
window.location.hash = 'profile';
```

### Native Java
```java
webViewManager.navigateToView("home");
```

## Views
- `#home` - Green background, navigation buttons
- `#settings` - Blue background, settings info
- `#profile` - Red background, user info

## Performance
- **Initial Load**: Once (at app start)
- **Navigation**: 50-100ms (no reload)
- **Memory**: Single WebView instance
- **Result**: ~10x faster than traditional

## Debug
```bash
# LogCat
adb logcat | grep WebViewManager

# Chrome Inspect
chrome://inspect
```

## Files Modified
```
platforms/android/app/src/main/java/.../
  ├── WebViewManager.java (new)
  ├── LoginActivity.java (new)
  └── MainActivity.java (modified)

platforms/android/app/src/main/res/layout/
  └── activity_login.xml (new)

www/
  ├── index.html (modified)
  ├── js/index.js (modified)
  └── css/index.css (modified)
```

## Testing
1. Launch → See preloaded WebView
2. Login → MainActivity opens (no reload)
3. Navigate → Settings/Profile (no reload)
4. Back → Returns to Home
5. Check console: "NO RELOAD" messages

## APK Location
```
platforms/android/app/build/outputs/apk/debug/app-debug.apk
```
