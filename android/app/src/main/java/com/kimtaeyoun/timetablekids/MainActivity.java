package com.kimtaeyoun.timetablekids;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * 연아·연우 시간표 — 저장소의 웹앱(index.html·manifest.json·sw.js)을
 * assets/www 에 그대로 담아 오프라인에서도 열리고,
 * 구글 시트(Apps Script 웹 앱)와는 인터넷이 될 때 동기화되는 WebView 앱.
 */
public class MainActivity extends Activity {

    private static final String TAG = "TimetableKids";
    private static final String START_URL = "file:///android_asset/www/index.html";
    private static final int BG_COLOR = 0xFFF4F6FB;

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG_COLOR);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(BG_COLOR);
        configureWebView(webView);
        root.addView(webView);

        setContentView(root);
        webView.loadUrl(START_URL);
    }

    private void configureWebView(WebView view) {
        WebSettings s = view.getSettings();

        s.setJavaScriptEnabled(true);
        // GAS_SCHEDULE_URL · CACHED_SCHEDULE · CACHED_TUITION 을 localStorage 에 보관한다.
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        // assets/www 를 file:// 로 열고, 그 안에서 https://script.google.com 으로 직접 통신한다.
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 모바일 웹앱 뷰포트를 그대로 쓰고, 확대·축소는 막아 앱처럼 보이게 한다.
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }
        // index.html 이 라이트 모드로 고정되어 있으므로 시스템 다크모드 강제 반전을 끈다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            s.setForceDark(WebSettings.FORCE_DARK_OFF);
        }

        view.setVerticalScrollBarEnabled(false);
        view.setHorizontalScrollBarEnabled(false);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return handleUrl(Uri.parse(url));
            }
        });

        // alert() · confirm() · prompt() 를 안드로이드 다이얼로그로 띄운다.
        // (일정·학원비 삭제 확인과 저장 오류 안내가 모두 이 창을 쓴다.)
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView v, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView v, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView v, String url, String message, String defaultValue,
                                      final JsPromptResult result) {
                final EditText input = new EditText(MainActivity.this);
                input.setText(defaultValue == null ? "" : defaultValue);
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setView(input)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(input.getText().toString());
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, sourceID + ":" + lineNumber + " " + message);
            }
        });
    }

    /** 앱 안의 페이지는 WebView 에서, 바깥 링크는 기본 브라우저에서 연다. */
    private boolean handleUrl(Uri uri) {
        if (uri == null) {
            return true;
        }
        String scheme = uri.getScheme();
        if (scheme == null || "file".equals(scheme) || "about".equals(scheme)
                || "javascript".equals(scheme)) {
            return false;
        }
        if ("http".equals(scheme) || "https".equals(scheme)
                || "mailto".equals(scheme) || "tel".equals(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "열 수 있는 앱이 없습니다: " + uri);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 열려 있는 모달이 있으면 닫고, 웹 히스토리가 있으면 뒤로, 아니면 앱을 끝낸다.
     * 모달이 열린 채로 앱이 종료되면 입력하던 내용을 잃기 때문이다.
     */
    private void handleBack() {
        if (webView == null) {
            finish();
            return;
        }
        final String js = "(function(){"
                + "var m=document.querySelector('.modal-backdrop.open');"
                + "if(!m){return 'none';}"
                + "var btns=Array.prototype.slice.call(m.querySelectorAll('[onclick]'));"
                + "for(var i=0;i<btns.length;i++){"
                + "  var a=btns[i].getAttribute('onclick')||'';"
                + "  if(/close/i.test(a)){btns[i].click();return 'closed';}"
                + "}"
                + "m.classList.remove('open');return 'closed';"
                + "})()";
        webView.evaluateJavascript(js, value -> {
            boolean closed = value != null && value.contains("closed");
            if (closed) {
                return;
            }
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            // 돌아왔을 때 최신 시간표를 다시 받아 오도록 페이지에 알린다.
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
