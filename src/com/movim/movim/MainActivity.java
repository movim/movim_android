package com.movim.movim;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

@SuppressLint("SetJavaScriptEnabled") public class MainActivity extends Activity {
	private WebView webview;
	private ProgressBar progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);

        webview = (WebView)findViewById(R.id.webview);
        
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);

        progressbar = (ProgressBar)findViewById(R.id.progress);
        progressbar.setIndeterminate(true);

        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
               progressbar.setProgress(progress);
               if(progress < 100 && progress > 0 && progressbar.getVisibility() == ProgressBar.GONE){
            	   progressbar.setIndeterminate(true);
                   progressbar.setVisibility(ProgressBar.VISIBLE);
               }
               if(progress == 100) {
            	   progressbar.setVisibility(ProgressBar.GONE);
               }
               
            }
        });
        webview.setWebViewClient(new WebViewClient());
        webview.loadUrl("https://pod.movim.eu/");
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