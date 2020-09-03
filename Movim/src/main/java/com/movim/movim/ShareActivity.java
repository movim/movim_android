package com.movim.movim;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class ShareActivity extends Activity {
    private WebView webview;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setTheme(R.style.SplashTheme);
        setContentView(R.layout.activity_main);

        // Get and extract the first valid URL
        String rawText = intent.getStringExtra(Intent.EXTRA_TEXT);
        List<String> urls = this.extractLinks(rawText);

        String url = "";

        if (urls.size() > 0) {
            url = urls.get(0);
        } else {
            Toast.makeText(this, "No valid URL found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webview = (WebView) findViewById(R.id.webview);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.parseColor("#000000"));
        }

        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webview.loadUrl(url);
                return true;
            }
        });

        webview.loadUrl("file:///android_asset/share.html?" + Uri.encode(url));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        this.onNewIntent(getIntent());
    }

    private static List<String> extractLinks(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            if(!url.toLowerCase().contains("http://") && !url.toLowerCase().contains("https://"))
            {
                url = "https://" + url;
            }
            links.add(url);
        }

        return links;
    }
}