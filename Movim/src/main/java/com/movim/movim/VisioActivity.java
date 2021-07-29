package com.movim.movim;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

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
        webview.getSettings().setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.parseColor("#000000"));
        }

        webview.addJavascriptInterface(this, "Android");
        webview.setWebChromeClient(new WebChromeClient() {
            public void onCloseWindow(WebView view){
                super.onCloseWindow(view);
                endCall();
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

        String channelId = "channel-movim";
        String groupId = "movim";

        /* Persistent notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent i = new Intent(this, VisioActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_phone)
                .setOngoing(true)
                .setContentTitle("Call")
                .setContentText("…in progress")
                .setContentIntent(pi)
                .setLights(Color.parseColor("#3F51B5"), 1000, 5000)
                .setGroup(groupId)
                .build();
        notificationManager.notify("call", 0, notification);

        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().updateNotifications();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        this.endCall();

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel("call", 0);
        MainActivity.getInstance().updateNotifications();
    }

    private void endCall() {
        this.finishAndRemoveTask();

        webview.destroy();
        webview = null;
    }

    @JavascriptInterface
    public void closePopUpWebview() {
        this.endCall();
    }
}