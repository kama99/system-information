package common.lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ZoomButtonsController;
import base.lib.WebUtil;
import base.lib.util;

public class MyWebView extends WebView {
	Context mContext;
	SimpleBrowser mActivity;
	
	public String pageSource = "", m_url = "";

	WrapWebSettings webSettings;

	ZoomButtonsController mZoomButtonsController;
	public boolean zoomVisible = false;
	public boolean html5 = false;

	int mProgress = 0;
	public boolean isForeground = true;
	public boolean closeToBefore = true;
	public boolean shouldCloseIfCannotBack = false;

	static Method setScrollbarFadingEnabled = null;
	static Method canScrollVerticallyMethod = null;
	static Method freeMemoryMethod = null;
	static {
		try {
			//API 5
			setScrollbarFadingEnabled = WebView.class.getMethod("setScrollbarFadingEnabled", new Class[] { boolean.class });

			//API 7
			freeMemoryMethod  = WebView.class.getMethod("freeMemory");

			//API 14
			canScrollVerticallyMethod = WebView.class.getMethod("canScrollVertically", new Class[] { int.class });
		} catch (Exception e) {}
	}
	public boolean myCanScrollVertically(int direction) {// API 14
		if (canScrollVerticallyMethod != null)
			try {
				Object o = canScrollVerticallyMethod.invoke(this, direction);
				return "true".equals(o.toString());
			} catch(Exception e) {}
		
		return true;
	}
	
	public void freeMemory(MyWebView webview) {
		if (freeMemoryMethod != null)
			try {freeMemoryMethod.invoke(webview);} catch (Exception e) {}
	}
	
	public void getPageSource() {// to get page source, part 3
		loadUrl("javascript:window.JSinterface.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
	}
	
	public void getPageReadyState() {// title1 will not be empty if ready
		loadUrl("javascript:window.JSinterface.processReady(document.getElementById('title1').innerHTML);");
	}

	public void setZoomControl(int visibility) {
		Class<?> classType;
		Field field;
		try {
			classType = WebView.class;
			field = classType.getDeclaredField("mZoomButtonsController");
			field.setAccessible(true);
			if (visibility == View.GONE) {
				// backup the original zoom controller
				mZoomButtonsController = (ZoomButtonsController) field.get(this);
				ZoomButtonsController myZoomButtonsController = new ZoomButtonsController(this);
				myZoomButtonsController.getZoomControls().setVisibility(visibility);
				field.set(this, myZoomButtonsController);
				zoomVisible = false;
			} else {
				field.set(this, mZoomButtonsController);
				zoomVisible = true;
			}
		} catch (Exception e) {}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {// onTouchListener may not
													// work. so put relate
													// code here
		int eventAction = event.getAction();
		switch (eventAction) {
		case MotionEvent.ACTION_DOWN:
			if (mActivity.webControl.getVisibility() == View.VISIBLE)// close webcontrol page if it is open.
				mActivity.webControl.setVisibility(View.GONE);
			
			if (mActivity.adContainer.getVisibility() == View.VISIBLE)
				mActivity.adContainer.setVisibility(View.GONE);

			if (!mActivity.showUrl) mActivity.setUrlHeight(false);
			if (!mActivity.showControlBar) mActivity.setBarHeight(false);

			if (!this.isFocused()) {
				this.setFocusableInTouchMode(true);
				this.requestFocus();
				mActivity.webAddress.setFocusableInTouchMode(false);
				mActivity.webAddress.clearFocus();
			}
			break;
		case MotionEvent.ACTION_UP:
			break;
		}
		
		//requestDisallowInterceptTouchEvent(true);
		return super.onTouchEvent(event);
	}

	public MyWebView(Context context, SimpleBrowser activity) {
		super(context);

		mContext = context;
		mActivity = activity;

		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);// no white blank on the right of webview
		
		WebSettings localSettings = getSettings();
		localSettings.setSaveFormData(true);
		localSettings.setTextSize(mActivity.textSize);
		localSettings.setSupportZoom(true);
		localSettings.setBuiltInZoomControls(true);

		// otherwise can't scroll horizontal in some webpage, such as qiupu.
		localSettings.setUseWideViewPort(true);

		localSettings.setPluginsEnabled(true);
		// localSettings.setPluginState(WebSettings.PluginState.ON);
		// setInitialScale(1);
		localSettings.setSupportMultipleWindows(true);
		localSettings.setJavaScriptCanOpenWindowsAutomatically(mActivity.blockPopup);
		localSettings.setBlockNetworkImage(mActivity.blockImage);
		if (mActivity.cachePrefer)
			localSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		localSettings.setJavaScriptEnabled(!mActivity.blockJs);

		if (mActivity.ua <= 1)
			localSettings.setUserAgent(mActivity.ua);
		else
			localSettings.setUserAgentString(WebUtil.selectUA(mActivity.ua));

		webSettings = new WrapWebSettings(localSettings);
		if (!webSettings.setDisplayZoomControls(false)) 
			// hide zoom button by default on API 11 and above
			setZoomControl(View.GONE);// default not show zoom control in new page

		mActivity.registerForContextMenu(this);

		// to get page source, part 2
		addJavascriptInterface(new MyJavaScriptInterface(this, mActivity), "JSinterface");

		setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(final String url, String ua,
					final String contentDisposition, String mimetype,
					long contentLength) {
				downloadAction(url, contentDisposition, mimetype);
			}
		});


		setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedSslError(WebView view,
					SslErrorHandler handler, SslError error) {
				// accept ssl certification whenever needed.
				if (handler != null) handler.proceed();
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				m_url = url;
				pageSource = "";
				
				if (mActivity.HOME_PAGE.equals(url)) view.getSettings().setJavaScriptEnabled(true);// not block js on homepage
				else view.getSettings().setJavaScriptEnabled(!mActivity.blockJs);


				if (isForeground) {
					// close soft keyboard
					mActivity.imm.hideSoftInputFromWindow(getWindowToken(), 0);
					mActivity.loadProgress.setVisibility(View.VISIBLE);
					
					if (mActivity.HOME_PAGE.equals(url)) mActivity.webAddress.setText(mActivity.HOME_BLANK);
					else mActivity.webAddress.setText(url);
					
					if ((mActivity.adview != null) && (mActivity.adview.isReady())) mActivity.adContainer.setVisibility(View.VISIBLE);
					if ((mActivity.adview2 != null) && (mActivity.adContainer2.getVisibility() != View.GONE)) mActivity.adview2.loadAd();
					if (mActivity.interstitialAd != null && !mActivity.interstitialAd.isReady()) mActivity.interstitialAd.loadAd();
					
					mActivity.imgRefresh.setImageResource(R.drawable.stop);

					if (!mActivity.showUrl) mActivity.setUrlHeight(true);
					if (!mActivity.showControlBar) mActivity.setBarHeight(true);
				}

				//try {if (baiduEvent != null) baiduEvent.invoke(mContext, mContext, "1", url);
				//} catch (Exception e) {}
				
				if (!mActivity.incognitoMode) mActivity.recordPages();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				pageSource = "";// prevent get incomplete page source during page loading
				m_url = url;// must sync the url for it may change after pagestarted.
				mProgress = 0;
				pageFinishAction(view, url, isForeground);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (mActivity.HOME_BLANK.equals(url)) {// some site such as weibo and
					// mysilkbaby will send
					// BLANK_PAGE when login.
					return true;// we should do nothing but return true,
								// otherwise may not login.
				} else if (!url.startsWith("http") && !url.startsWith("file")) {
					overloadAction(url);
					return true; // not allow webpage to proceed
				} else if ("file:///sdcard/".equals(url)) {
					Intent intent = new Intent("android.intent.action.GET_CONTENT");
					intent.setType("file/*");
					util.startActivity(intent, true, mContext);
					return true;
				} else
					return false;
			}
		});
	}
	
	void downloadAction(final String url, final String contentDisposition, String mimetype) {
		// need to know it is httpget, post or direct connect.
		// for example, I don't know how to handle this
		// http://yunfile.com/file/murongmr/5a0574ad/. firefox think
		// it is post.
		// url:
		// http://dl33.yunfile.com/file/downfile/murongmr/876b15e4/c7c3002a
		// ua: Mozilla/5.0 (Linux; U; Android 2.3.3; en-us; sdk
		// Build/GRI34) AppleWebKit/533.1 (KHTML, like Gecko)
		// Version/4.0 Mobile Safari/533.1
		// contentDisposition: attachment;
		// filename*="utf-8''CHUN%E5%85%89%E8%BC%9D%E8%8D%92%E9%87%8E.rar"
		// mimetype: application/octet-stream
		// contentLength: 463624
		boolean canOpen = false;
		String apkName = WebUtil.getName(url);
		if ((mimetype == null) || ("".equals(mimetype))) {
			MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
			String ext = apkName.substring(apkName.lastIndexOf(".")+1, apkName.length());
			mimetype = mimeTypeMap.getMimeTypeFromExtension(ext);
		}
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		if ((mimetype != null) && (!mimetype.equals("")) && (!mimetype.equals("application/vnd.android.package-archive")) && (!mimetype.equals("audio/mpeg"))) {
			//show chooser if it can open, otherwise download it.
			//download apk and mp3 directly without confirm. 
			intent.setDataAndType(Uri.parse(url), mimetype);
			List<ResolveInfo> list = mActivity.getPackageManager().queryIntentActivities(intent, 0);
			if ((list != null) && !list.isEmpty()) canOpen = true;
		}

		if (canOpen) {
			try {
			new AlertDialog.Builder(mContext)
			.setTitle(mContext.getString(R.string.choose))
			.setMessage(apkName)
			.setPositiveButton(mContext.getString(R.string.open),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							if (!util.startActivity(intent, false, mContext))
								mActivity.appstate.startDownload(url, contentDisposition, "yes");//download if open fail
						}
					})
			.setNeutralButton(mContext.getString(R.string.download),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							mActivity.appstate.startDownload(url, contentDisposition, "yes");
						}
					})
			.setNegativeButton(mContext.getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
						}
					}).show();} catch(Exception e) {}
		} else mActivity.appstate.startDownload(url, contentDisposition, "yes");
	}
	
	void overloadAction(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		String data = intent.getDataString();
		if (!"".equals(data) && (data.startsWith("vnd.youtube"))) {
			if (!util.startActivity(intent, false, mContext)) {
				try {
				new AlertDialog.Builder(mContext)
				.setMessage("You need install plugin or client to play video.")
				.setPositiveButton("Youtube",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri
										.parse("market://details?id=com.google.android.youtube"));
								util.startActivity(intent, false, mContext);
							}
						})
				.setNeutralButton("Adobe flash",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri
										.parse("market://details?id=com.adobe.flashplayer"));
								util.startActivity(intent, false, mContext);
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();} catch(Exception e) {}
			}
		}
		else util.startActivity(intent, true, mContext);
	}
	
	void pageFinishAction(WebView view, String url, boolean isForeground) {
		if (isForeground) {
			// hide progressbar anyway
			mActivity.loadProgress.setVisibility(View.GONE);
			mActivity.imgRefresh.setImageResource(R.drawable.refresh);
			mActivity.webControl.setVisibility(View.GONE);
			if (mActivity.HOME_PAGE.equals(url)) mActivity.webAddress.setText(mActivity.HOME_BLANK);
			else mActivity.webAddress.setText(url);
		}
		// update the page title in webList
		mActivity.webAdapter.notifyDataSetChanged();

		String title = view.getTitle();
		if (title == null) title = url;

		if (mActivity.HOME_PAGE.equals(url)) ;// do nothing
		else {
			if (mActivity.browserName.equals(title)) ;
				// if title and url not sync, then sync it
				//webAddress.setText(HOME_BLANK);
			else if (!mActivity.incognitoMode) {// handle the bookmark/history after load new page
				if ((mActivity.mHistory.size() > 0) && (mActivity.mHistory.get(mActivity.mHistory.size() - 1).m_url.equals(url))) return;// already the latest, no need to update history list

				String site = WebUtil.getSite(url);
				TitleUrl titleUrl = new TitleUrl(title, url, site);
				mActivity.mHistory.add(titleUrl);// always add it to history if visit any page.
				mActivity.historyChanged = true;

				for (int i = mActivity.mHistory.size() - 2; i >= 0; i--) {
					if (mActivity.mHistory.get(i).m_url.equals(url)) {
						if (title.equals(url)) {// use meaningful title to replace title with url content
							String meaningfulTitle = mActivity.mHistory.get(i).m_title;
							if (!meaningfulTitle.equals(url)) 
								mActivity.mHistory.set(mActivity.mHistory.size()-1, mActivity.mHistory.get(i));
						}
						mActivity.mHistory.remove(i);// record one url only once in the history list. clear old duplicate history if any
						mActivity.updateHistory();
						return;
					} 
					else if (title.equals(mActivity.mHistory.get(i).m_title)) {
						mActivity.mHistory.remove(i);// only keep the latest history of the same title. display multi item with same title is not useful to user
						break;
					}
				}

				if (mActivity.siteArray.indexOf(site) < 0) {
					// update the auto-complete edittext without duplicate
					mActivity.urlAdapter.add(site);
					// the adapter will always return 0 when get count or search, so we use an array to store the site.
					mActivity.siteArray.add(site);
				}

				try {// try to open the png, if can't open, then need save
					FileInputStream fis = mContext.openFileInput(site + ".png");
					try {fis.close();} catch (IOException e) {}
				} catch (Exception e1) {
					try {// save the Favicon
						if (view.getFavicon() != null) {
							Bitmap favicon = view.getFavicon();
							int width = favicon.getWidth();
						    int height = favicon.getHeight();
							if ((width > 16) || (height > 16)) {// scale the favicon if it is not 16*16
							    // calculate the scale
							    float scaleWidth = ((float) 16) / width;
							    float scaleHeight = ((float) 16) / height;

							    // create matrix for the manipulation
							    Matrix matrix = new Matrix();
							    // resize the bit map
							    matrix.postScale(scaleWidth, scaleHeight);

							    // recreate the new Bitmap
							    favicon = Bitmap.createBitmap(favicon, 0, 0, 
							                      width, height, matrix, true); 
							}
							FileOutputStream fos = mContext.openFileOutput(site + ".png", 0);
							favicon.compress(Bitmap.CompressFormat.PNG, 90,	fos);
							fos.close();
						}
					} catch (Exception e) {}
				}

				while (mActivity.mHistory.size() > mActivity.historyCount) 
					// delete from the first history until the list is not larger than historyCount;
					 //not delete icon here. it can be clear when clear all 
					mActivity.mHistory.remove(0);
				
				mActivity.updateHistory();
			}
		}
	}	
}