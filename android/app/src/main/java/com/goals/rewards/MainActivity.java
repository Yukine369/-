package com.goals.rewards;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 42;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setUserAgentString(s.getUserAgentString() + " GoalsAndroidApp/1.0");

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternal(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternal(Uri.parse(url));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Файл резервной копии"), FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/www/index.html");
    }

    private boolean openExternal(Uri uri) {
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) { }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.onAndroidBack && window.onAndroidBack()) ? 'yes' : 'no'", value -> {
            if (value == null || !value.contains("yes")) MainActivity.super.onBackPressed();
        });
    }

    private class Bridge {
        @JavascriptInterface
        public void shareFile(final String name, final String text) {
            runOnUiThread(() -> {
                try {
                    File dir = new File(getCacheDir(), "backup");
                    dir.mkdirs();
                    String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
                    File f = new File(dir, safe);
                    FileOutputStream out = new FileOutputStream(f);
                    out.write(text.getBytes(Charset.forName("UTF-8")));
                    out.close();
                    Uri uri = Uri.parse("content://com.goals.rewards.backup/" + safe);
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("application/json");
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.putExtra(Intent.EXTRA_SUBJECT, "Резервная копия «Цели и награды»");
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(send, "Сохранить копию"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Не удалось создать файл", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
