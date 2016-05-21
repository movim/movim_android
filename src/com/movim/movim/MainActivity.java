package com.movim.movim;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity {
    private WebView webview;
    private ProgressBar progressbar;
    private HashMap<String, List<String>> notifs;
    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.notifs = new HashMap<String, List<String>>();

        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        webview = (WebView) findViewById(R.id.webview);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setMixedContentMode(0);
        if (Build.VERSION.SDK_INT >= 21)
            webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
            webview.getSettings().setAllowUniversalAccessFromFileURLs(true);

        progressbar = (ProgressBar) findViewById(R.id.progress);
        progressbar.setIndeterminate(true);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(48, 63, 159));
        }

        webview.addJavascriptInterface(this, "Android");

        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressbar.setProgress(progress);
                if (progress < 100 && progress > 0 && progressbar.getVisibility() == ProgressBar.GONE) {
                    progressbar.setIndeterminate(true);
                    progressbar.setVisibility(ProgressBar.VISIBLE);
                }
                if (progress == 100) {
                    progressbar.setVisibility(ProgressBar.GONE);
                }

            }

        });
        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                webview.loadUrl("file:///android_asset/error.html");
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String origin = Uri.parse(view.getUrl()).getHost();
                String aim = Uri.parse(url).getHost();

                if (origin.isEmpty() || origin.equals(aim)) {
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Ignore SSL certificate errors
            }
        });

        webview.loadUrl("file:///android_asset/index.html");

        instance = this;
    }

    @Override
    public void onNewIntent(Intent intent){
        if(intent.getAction() != null) {
            this.notifs.remove(intent.getAction());
            webview.loadUrl(intent.getAction());
        }
    }

    public static MainActivity getInstance(){
        return instance;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
    @JavascriptInterface
    public void showNotification(String title, String body, String picture, String action) {
        Bitmap pictureBitmap = getBitmapFromURL(picture);

        Intent i = new Intent(this, MainActivity.class);
        if(action != null) {
            i.setAction(action);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // The deleteIntent declaration
        Intent deleteIntent = new Intent(action);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(receiver, new IntentFilter(action));

        //Integer counter;
        List<String> messages = null;

        // There is already pending notifications
        if(this.notifs.get(action) != null) {
            messages = this.notifs.get(action);
        } else {
            messages = new ArrayList<String>();
        }

        messages.add(body);

        if(messages.size() > 5) {
            messages.remove(0);
        }

        this.notifs.put(action, messages);

        // We create the inbox
        Notification.InboxStyle style = new Notification.InboxStyle();
        for (int j = 0; j < messages.size(); j++) {
            style.addLine(messages.get(j));
        }

        style.setBigContentTitle(title);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(pictureBitmap)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pi)
                .setDeleteIntent(pendingDeleteIntent)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#3F51B5"))
                .setNumber(messages.size())
                .setStyle(style)
                .setGroup("movim")
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(action, 0, notification);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null) {
                MainActivity.getInstance().notifs.remove(intent.getAction());
            }
            unregisterReceiver(this);
        }
    };

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            webview.loadUrl("javascript:MovimTpl.toggleMenu()");
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webview.loadUrl("javascript:MovimTpl.back()");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}