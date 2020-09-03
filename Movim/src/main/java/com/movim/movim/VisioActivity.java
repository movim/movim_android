package com.movim.movim;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class VisioActivity extends Activity {
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.SplashTheme);
        setContentView(R.layout.activity_visio);

        String url = getIntent().getStringExtra("url");

        webview = (WebView) findViewById(R.id.visioview);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setMixedContentMode(0);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.parseColor("#000000"));
        }

        webview.addJavascriptInterface(this, "Android");
        webview.setWebChromeClient(new WebChromeClient() {
            public void onCloseWindow(WebView view){
                super.onCloseWindow(view);
                finishAndRemoveTask();
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                webview.loadUrl("file:///android_asset/error.html");
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webview.loadUrl(url);
                return true;
            }
        });

        webview.loadUrl("https://" + url);
    }

    @JavascriptInterface
    public void closePopUpWebview() {
        this.finishAndRemoveTask();
    }
}