package com.movim.movim;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
	private WebView web_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web_view = (WebView)findViewById(R.id.webview);
        web_view.getSettings().setJavaScriptEnabled(true);
        web_view.setWebViewClient(new WebViewClient());
        web_view.loadUrl("https://pod.movim.eu/");
        web_view.getSettings().setDomStorageEnabled(true);
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	web_view.loadUrl("javascript:MovimTpl.toggleMenu()");
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	web_view.loadUrl("javascript:MovimTpl.back()");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}