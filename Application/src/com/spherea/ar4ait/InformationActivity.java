package com.spherea.ar4ait;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InformationActivity extends Activity
{

    /**
     * Web view to display information
     */
    WebView mWebView;

    /**
     * Progress view
     */
    View mProgress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

        setContentView(R.layout.webview);
        mProgress = findViewById(R.id.progress);
        mWebView = (WebView) findViewById(R.id.webview);

        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setWebViewClient(new WebViewHandler());
        String url = getIntent().getStringExtra("URL");
        mWebView.loadUrl(url);
        mWebView.setVisibility(View.VISIBLE);
	}

    @Override
    protected void onResume()
    {
        super.onResume();
        mWebView.resumeTimers();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mWebView.pauseTimers();
    }

    @Override
    public void onBackPressed()
    {
        // if web view can go back, go back
        if (mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }

    class WebViewHandler extends WebViewClient
    {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            mProgress.setVisibility(View.GONE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            if (url.contains("metaio.com"))
            {
                // Open external browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(intent);
                return true;
            }

            return false;
        }
    }
}

