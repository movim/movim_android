package com.movim.movim;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.google.firebase.messaging.Constants.TAG;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity {
	private WebView webview;
	private ProgressBar progressbar;
	private String firebaseToken = "";
	private static MainActivity instance;

	private ValueCallback<Uri[]> mUploadMessageArray;
	private final static int FILE_REQUEST_CODE = 1;
	private final static int CAMERA_REQUEST_CODE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.SplashTheme);

		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		webview = findViewById(R.id.webview);

		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setDomStorageEnabled(true);
		webview.getSettings().setMixedContentMode(0);
		webview.getSettings().setAppCacheEnabled(true);
		webview.getSettings().setMediaPlaybackRequiresUserGesture(false);

		FirebaseMessaging.getInstance().getToken()
				.addOnCompleteListener(new OnCompleteListener<String>() {
					@Override
					public void onComplete(@NonNull Task<String> task) {
						if (!task.isSuccessful()) {
							Log.w(TAG, "Fetching FCM registration token failed", task.getException());
							return;
						}
						firebaseToken = task.getResult();
					}
				});
		
		if (Build.VERSION.SDK_INT >= 21) {
			webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
			getWindow().setNavigationBarColor(Color.parseColor("#000000"));
		}

		if (Build.VERSION.SDK_INT >= 19) {
			// chromium, enable hardware acceleration
			webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		} else {
			// older android version, disable hardware acceleration
			webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}

		progressbar = findViewById(R.id.progress);
		progressbar.setIndeterminate(true);

		webview.addJavascriptInterface(this, "Android");
		webview.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onPermissionRequest(PermissionRequest request) {
				checkAndRequestPermissions();
				request.grant(request.getResources());
			}

			public void onProgressChanged(WebView view, int progress) {
				progressbar.setProgress(progress);
				if (progress < 100 && progress > 0 && progressbar.getVisibility() == ProgressBar.GONE) {
					progressbar.setIndeterminate(true);
					progressbar.setVisibility(ProgressBar.VISIBLE);
				}
				if (progress == 100) {
					progressbar.setVisibility(ProgressBar.GONE);

					if (!firebaseToken.isEmpty()) {
						webview.loadUrl("javascript:Presence.setFirebaseToken('" + firebaseToken + "')");
					}
				}
			}

			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMessageArray, WebChromeClient.FileChooserParams fileChooserParams) {
				if (mUploadMessageArray != null) {
					mUploadMessageArray.onReceiveValue(null);
				}

				mUploadMessageArray = uploadMessageArray;

				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.addCategory(Intent.CATEGORY_OPENABLE);
				i.setType("*/*");

				Intent ci = new Intent(Intent.ACTION_CHOOSER);
				ci.putExtra(Intent.EXTRA_INTENT, i);
				ci.putExtra(Intent.EXTRA_TITLE, "File Browser");
				startActivityForResult(ci, FILE_REQUEST_CODE);
				return true;
			}
		});

		webview.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				webview.loadUrl("file:///android_asset/error.html");
			}

			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.equals("movim://changepod")) {
					webview.loadUrl("file:///android_asset/list.html");
					return true;
				}

				String origin = Uri.parse(view.getUrl()).getHost();
				String aim = Uri.parse(url).getHost();

				if (origin.isEmpty() || origin.equals(aim)) {
					return false;
				}

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
				return true;
			}

			public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
				webview.loadUrl("file:///android_asset/ssl.html");
			}

			public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler, final String host,
					final String realm) {
				final String[] httpAuth = new String[2];
				final String[] viewAuth = view.getHttpAuthUsernamePassword(host, realm);
				final EditText usernameInput = new EditText(MainActivity.getInstance());
				final EditText passwordInput = new EditText(MainActivity.getInstance());
 
				httpAuth[0] = viewAuth != null ? viewAuth[0] : new String();
				httpAuth[1] = viewAuth != null ? viewAuth[1] : new String();

				usernameInput.setHint("Username");
				passwordInput.setHint("Password");
				passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

				LinearLayout ll = new LinearLayout(MainActivity.getInstance());
				ll.setOrientation(LinearLayout.VERTICAL);
				ll.addView(usernameInput);
				ll.addView(passwordInput);

				Builder authDialog = new AlertDialog.Builder(MainActivity.getInstance()).setTitle("Please login")
						.setView(ll).setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								httpAuth[0] = usernameInput.getText().toString();
								httpAuth[1] = passwordInput.getText().toString();
								view.setHttpAuthUsernamePassword(host, realm, httpAuth[0], httpAuth[1]);
								handler.proceed(httpAuth[0], httpAuth[1]);
								dialog.dismiss();
							}
						});

				if (!handler.useHttpAuthUsernamePassword()) {
					authDialog.show();
				} else {
					handler.proceed(httpAuth[0], httpAuth[1]);
				}
			}
		});

		webview.loadUrl("file:///android_asset/index.html");

		instance = this;
	}

	@Override
	protected void onStop () {
		super.onStop() ;
		startService( new Intent( this, NotificationService. class )) ;
	}

	private boolean checkAndRequestPermissions() {
		int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

		List<String> listPermissionsNeeded = new ArrayList<>();
		if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
			listPermissionsNeeded.add(Manifest.permission.CAMERA);
		}
		if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
			listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
		}
		if (!listPermissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), CAMERA_REQUEST_CODE);
			return false;
		}

		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
		}

		webview.reload();
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (intent.getAction() != null) {
			//this.notifs.remove(intent.getAction());
			webview.loadUrl(intent.getAction());
		}
	}

	// Prevent the webview from reloading on device rotation
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	};

	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mUploadMessageArray == null)
			return;

		mUploadMessageArray.onReceiveValue(new Uri[]{});
		mUploadMessageArray = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode != FILE_REQUEST_CODE || mUploadMessageArray == null) {
			return;
		}

		Uri[] results = null;
		if (resultCode == Activity.RESULT_OK) {
			String dataString = data.getDataString();
			if (dataString != null) {
				results = new Uri[]{Uri.parse(dataString)};
			}
		}
		mUploadMessageArray.onReceiveValue(results);
		mUploadMessageArray = null;
	}

	@JavascriptInterface
	public void clearFirebaseToken() {
		firebaseToken = "";
	}

	@JavascriptInterface
	public void openVisio(String url) {
		Intent intent = new Intent(this, VisioActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("url", url);
		startActivity(intent);
	}

	@JavascriptInterface
	public void showToast(String toast) {
		Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
	}

	@JavascriptInterface
	public void clearNotifications(String action) {
		Intent notificationService = new Intent(this, NotificationService.class);
		notificationService.setAction("clear");
		notificationService.putExtra("action", action);
		startService(notificationService);
	}

	@JavascriptInterface
	public void showNotification(String title, String body, String picture, String action) {
		Intent notificationService = new Intent(this, NotificationService.class);
		notificationService.setAction("notify");
		notificationService.putExtra("title", title);
		notificationService.putExtra("body", body);
		notificationService.putExtra("picture", picture);
		notificationService.putExtra("action", action);
		startService(notificationService);
	}

	/*private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() != null) {
				MainActivity.getInstance().notifs.remove(intent.getAction());
			}
			unregisterReceiver(this);
		}
	};*/

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