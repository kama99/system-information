package easy.lib;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Map.Entry;

//import net.simonvt.menudrawer.MenuDrawer;
//import net.simonvt.menudrawer.Position;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import common.lib.MyApp;

import base.lib.StringComparator;
import base.lib.WrapInterstitialAd;
import base.lib.util;
import base.lib.WrapAdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ZoomButtonsController;

//for get webpage source on cupcake
class wrapValueCallback {
	ValueCallback<Uri> mInstance;

	wrapValueCallback() {}

	static {
		try {
			Class.forName("android.webkit.ValueCallback");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void checkAvailable() {}

	void onReceiveValue(Uri value) {
		mInstance.onReceiveValue(value);
	}
}

public class SimpleBrowser extends Activity {

	String HOME_PAGE = "file:///android_asset/home.html";
	final String HOME_BLANK = "about:blank";
	String m_homepage = null;
	boolean firstRun = false;

	ListView webList;
	int revertCount = 0;
	boolean needRevert = false;
	boolean reverted = false;

	Context mContext;
	String browserName;

	//boolean flashInstalled = false;
	// settings
	boolean showUrl = true;
	boolean showControlBar = true;
	final int urlHeight = 40, barHeight = 40;
	int statusBarHeight;
	boolean showStatusBar = true;
	int rotateMode = 1;
	boolean incognitoMode = false;
	boolean updownButton = true;
	boolean snapFullWeb = false;
	boolean blockImage = false;
	boolean cachePrefer = false;
	boolean blockPopup = false;
	boolean blockJs = false;
	TextSize textSize = TextSize.NORMAL;
	final int historyCount = 30;
	long sizeM = 1024 * 1024;
	long html5cacheMaxSize = sizeM * 8;
	int ua = 0;
	int searchEngine = 3;
	int shareMode = 2;
	private int SETTING_RESULTCODE = 1002;
	boolean enableProxy = false;
	int localPort;
	// boolean hideExit = true;
	boolean overviewPage = false;
	Locale mLocale;
	int bookmarkIndex = -1;

	SharedPreferences sp;
	Editor sEdit;

	// search
	EditText etSearch;
	TextView searchHint;
	RelativeLayout searchBar;
	ImageView imgSearchNext, imgSearchPrev, imgSearchClose;
	String toSearch = "";
	int matchCount = 0, matchIndex = 0;

	// snap dialog
	ImageView snapView;
	Bitmap bmp;
	AlertDialog snapDialog = null;

	// favo dialog
	EditText titleText;

	// source dialog
	AlertDialog m_sourceDialog = null;
	String sourceOrCookie = "";
	String subFolder = "source";
	
	// page up and down button
	LinearLayout upAndDown;
	ImageView upButton, downButton;

	// browser related
	GridView menuGrid = null;
	View bookmarkDownloads;
	LinearLayout bookmarkView;
	ListView downloadsList;
	RelativeLayout browserView;
	LinearLayout webControl, fakeWebControl;
	LinearLayout imageBtnList;
	FrameLayout adContainer;
	LinearLayout adContainer2;
	int minWebControlWidth = 200;
	int bookmarkWidth = LayoutParams.WRAP_CONTENT;
	int menuWidth = LayoutParams.WRAP_CONTENT;
	boolean menuOpened = true;
	boolean bookmarkOpened = true;
	
	AutoCompleteTextView webAddress;
	ArrayAdapter<String> urlAdapter;
	ArrayList<String> siteArray;

	MyListAdapter bookmarkAdapter, historyAdapter, downloadsAdapter;
	ListView historyList;
	
	ArrayList<MyWebview> serverWebs = new ArrayList<MyWebview>();
	int webIndex;
	ImageView imgPrev, imgRefresh, imgNew, imgBookmark, imgNext, imgMenu;
	boolean shouldGo = false;
	LinearLayout webTools, urlLine;
	RelativeLayout webs;
	Button btnNewpage;
	InputMethodManager imm;
	ProgressBar loadProgress;

	ConnectivityManager cm;
	DisplayMetrics dm;

	// upload related
	static boolean mValueCallbackAvailable;
	static {
		try {
			wrapValueCallback.checkAvailable();
			mValueCallbackAvailable = true;
		} catch (Throwable t) {
			mValueCallbackAvailable = false;
		}
	}
	private wrapValueCallback mUploadMessage;
	private int FILECHOOSER_RESULTCODE = 1001;

	// bookmark and history
	ArrayList<TitleUrl> mHistory = new ArrayList<TitleUrl>();
	ArrayList<TitleUrl> mBookMark = new ArrayList<TitleUrl>();
	ArrayList<TitleUrl> mSystemHistory = new ArrayList<TitleUrl>();
	ArrayList<TitleUrl> mHistoryForAdapter = new ArrayList<TitleUrl>();// the revert for mHistory.
	ArrayList<TitleUrl> mSystemBookMark = new ArrayList<TitleUrl>();
	ArrayList<TitleUrl> mDownloads = new ArrayList<TitleUrl>();
	boolean historyChanged = false, bookmarkChanged = false, downloadsChanged = false;
	ImageView imgAddFavo;//, imgGo;
	boolean noSdcard = false, noHistoryOnSdcard = false;

	// baidu tongji
	static Method baiduResume = null;
	static Method baiduPause = null;
	//static Method baiduEvent = null;
	static {
		try {
			Class c = Class.forName("com.baidu.mobstat.StatService");
			baiduResume = c.getMethod("onResume", new Class[] { Context.class });
			baiduPause = c.getMethod("onPause", new Class[] { Context.class });
			//baiduEvent = c.getMethod("onEvent", new Class[] { Context.class, String.class, String.class });
		} catch (Exception e) {}
	}
	
	// ad
	static boolean mAdAvailable;
	static {
		try {
			Class.forName("com.google.ads.AdView");
			mAdAvailable = true;
		} catch (Throwable t) {
			mAdAvailable = false;
		}
	}
	WrapAdView adview = null, adview2 = null;
	WrapInterstitialAd interstitialAd = null;
	AppHandler mAppHandler = new AppHandler();

	// download related
	String downloadPath = "";
	String dataPath = "";
	NotificationManager nManager;
	ArrayList<packageIDpair> downloadAppID;
	MyApp appstate;

	class packageIDpair {
		String packageName;
		File downloadedfile;
		int notificationID;

		packageIDpair(String name, int id, File file) {
			packageName = name;
			notificationID = id;
			downloadedfile = file;
		}
	}

	static Method setDisplayZoomControls = null;
	static Method canScrollVerticallyMethod = null;
	static {
		try {
			setDisplayZoomControls = WebSettings.class.getMethod("setDisplayZoomControls", new Class[] { boolean.class });//API 11
			canScrollVerticallyMethod = WebView.class.getMethod("canScrollVertically", new Class[] { int.class });//API 14
		} catch (Exception e) {}
	}
	
	class wrapWebSettings {
		WebSettings mInstance;

		wrapWebSettings(WebSettings settings) {
			mInstance = settings;
		}

		synchronized boolean setDisplayZoomControls(boolean enabled) {// API 11
			if (setDisplayZoomControls != null)
				try {
					setDisplayZoomControls.invoke(mInstance, enabled);
					return true;
				} catch (Exception e) {}
			return false;
		}
	}

	class MyWebview extends WebView {
		public String pageSource = "", m_url = "";

		wrapWebSettings webSettings;

		ZoomButtonsController mZoomButtonsController;
		boolean zoomVisible = false;
		boolean html5 = false;

		int mProgress = 0;
		boolean isForeground = true;
		boolean closeToBefore = true;
		boolean shouldCloseIfCannotBack = false;

		public boolean myCanScrollVertically(int direction) {
			if (canScrollVerticallyMethod != null)
				try {
					Object o = canScrollVerticallyMethod.invoke(this, direction);
					return "true".equals(o.toString());
				} catch(Exception e) {}
			
			return true;
		}
		
		public void getPageSource() {// to get page source, part 3
			loadUrl("javascript:window.JSinterface.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
		}

		class MyJavaScriptInterface {
			@SuppressWarnings("unused")
			public void processHTML(String html) {
				pageSource = html;// to get page source, part 1
			}

			@SuppressWarnings("unused")
			public void showInterstitialAd() {
				if (interstitialAd.isReady()) interstitialAd.show();
				else {
					Toast.makeText(mContext, "Admob is loading", Toast.LENGTH_LONG).show();
					Message fail = mAppHandler.obtainMessage();
					fail.what = -3;
					mAppHandler.sendMessage(fail);
				}
			}
		}

		public void setZoomControl(int visibility) {
			Class<?> classType;
			Field field;
			try {
				classType = WebView.class;
				field = classType.getDeclaredField("mZoomButtonsController");
				field.setAccessible(true);
				if (visibility == View.GONE) {
					mZoomButtonsController = (ZoomButtonsController) field
							.get(this);// backup the original zoom
										// controller
					ZoomButtonsController myZoomButtonsController = new ZoomButtonsController(
							this);
					myZoomButtonsController.getZoomControls()
							.setVisibility(visibility);
					field.set(this, myZoomButtonsController);
					zoomVisible = false;
				} else {
					field.set(this, mZoomButtonsController);
					zoomVisible = true;
				}
			} catch (Exception e) {}
		}

		
		private float oldX, oldY;
		// indicate if horizontal scrollbar can't go more to the left
		private boolean overScrollLeft = false;
		// indicate if horizontal scrollbar can't go more to the right
	    private boolean overScrollRight = false;
	    // indicate if horizontal scrollbar can't go more to the left OR right
	    private boolean isScrolling = false;
	    private boolean dragEdge = false;
		@Override
		public boolean onTouchEvent(MotionEvent event) {// onTouchListener may not work. so put relate code here
			int scrollBarWidth = getVerticalScrollbarWidth();
			// width of the view depending of you set in the layout
			int viewWidth = computeHorizontalScrollExtent();
			// width of the webpage depending of the zoom
	        int innerWidth = computeHorizontalScrollRange();
	        // position of the left side of the horizontal scrollbar
	        int scrollBarLeftPos = computeHorizontalScrollOffset();
	        // position of the right side of the horizontal scrollbar, the width of scroll is the width of view minus the width of vertical scrollbar
	        int scrollBarRightPos = scrollBarLeftPos + viewWidth - scrollBarWidth;

	        // if left pos of scroll bar is 0 left over scrolling is true
	        if(scrollBarLeftPos == 0) {
	            overScrollLeft = true;
	        } else {
	            overScrollLeft = false;
	        }

	        // if right pos of scroll bar is superior to webpage width: right over scrolling is true
	        //Log.d("=============scrollBarRightPos", scrollBarRightPos+"");
	        //Log.d("=============innerWidth", innerWidth+"");
	        if(scrollBarRightPos >= innerWidth-6) {
	            overScrollRight = true;
	        } else {
	            overScrollRight = false;
	        }

	        switch (event.getAction()) {
	        case MotionEvent.ACTION_DOWN: // when user touch the screen
	        	dragEdge = false;
	        	
	        	// if scrollbar is the most left or right
	            if(overScrollLeft || overScrollRight) {
	                isScrolling = false;
	            } else {
	                isScrolling = true;
	            }
	            oldX = event.getX();
	            oldY = event.getY();
	            
				if (!this.isFocused()) {
					this.setFocusableInTouchMode(true);
					this.requestFocus();
					webAddress.setFocusableInTouchMode(false);
					webAddress.clearFocus();
				}
	            break;
	        case MotionEvent.ACTION_MOVE: // when user stop to touch the screen
	        	// if scrollbar can't go more to the left OR right 
	        	// this allow to force the user to do another gesture when he reach a side
	            if (!isScrolling && (Math.abs(oldY - event.getY()) < 50)) {
	                if ((event.getX() > oldX+100) && overScrollLeft && (oldX < 100)) {
	                    // left action
	                	if (!reverted) {
	                		if (!bookmarkOpened) showBookmark();
	                	}
	                	else {
		                	if (!menuOpened) onMenuOpened(0, null);
	                	}
	                	dragEdge = true;
	                }
	                else if ((event.getX() < oldX-100) && overScrollRight && (oldX > dm.widthPixels-100)) {
	                	// right action
	                	if (!reverted) {
	                		if (!menuOpened) onMenuOpened(0, null);
	                	}
	                	else {
	                		if (!bookmarkOpened) showBookmark();
	                	}
	                	dragEdge = true;
	                }
	            }
	            break;
	        case MotionEvent.ACTION_UP:
	            if (!dragEdge) {
	            	scrollToMain();

	            	if (webControl.getVisibility() == View.VISIBLE)// close webcontrol page if it is open.
						webControl.setVisibility(View.INVISIBLE);
									
					if (!showUrl) setUrlHeight(false);
					if (!showControlBar) setBarHeight(false);
	            }
	            break;
	        default:
	            break;
	        }
			        
			return super.onTouchEvent(event);
		}

		public MyWebview(Context context) {
			super(context);

			setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);// no white blank on the right of webview
			
			setScrollbarFadingEnabled(true);// hide scroll bar when not scroll. from API5, not work on cupcake.

			WebSettings localSettings = getSettings();
			localSettings.setSaveFormData(true);
			localSettings.setTextSize(textSize);
			localSettings.setSupportZoom(true);
			localSettings.setBuiltInZoomControls(true);

			// otherwise can't scroll horizontal in some webpage, such as qiupu.
			//localSettings.setUseWideViewPort(true);// but it will cause font too small?

			// open Geolocation by default
			localSettings.setGeolocationEnabled(true);//API5
			//localSettings.setGeolocationDatabasePath(getDir("databases", MODE_PRIVATE).getPath());//API5. no use for get location in baidu map?

			localSettings.setPluginsEnabled(true);
			// localSettings.setPluginState(WebSettings.PluginState.ON);
			// setInitialScale(1);
			localSettings.setSupportMultipleWindows(true);
			localSettings.setJavaScriptCanOpenWindowsAutomatically(blockPopup);
			localSettings.setBlockNetworkImage(blockImage);
			if (cachePrefer)
				localSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
			localSettings.setJavaScriptEnabled(!blockJs);

			if (ua <= 1)
				localSettings.setUserAgent(ua);
			else
				localSettings.setUserAgentString(selectUA(ua));

			webSettings = new wrapWebSettings(localSettings);
			if (!webSettings.setDisplayZoomControls(false)) // hide zoom button
															// by default on API
															// 11 and above
				setZoomControl(View.GONE);// default not show zoom control in new page

			localSettings.setDefaultZoom(ZoomDensity.MEDIUM);//start from API7

			localSettings.setDomStorageEnabled(true);// API7, key to enable gmail

			// loads the WebView completely zoomed out. fit for hao123, but not
			// fit for homepage. from API7
			//localSettings.setUseWideViewPort(overviewPage);
			localSettings.setLoadWithOverviewMode(overviewPage);

			registerForContextMenu(this);

			// to get page source, part 2
			addJavascriptInterface(new MyJavaScriptInterface(), "JSinterface");

			setDownloadListener(new DownloadListener() {
				@Override
				public void onDownloadStart(final String url, String ua,
						final String contentDisposition, String mimetype,
						long contentLength) {
					downloadAction(url, contentDisposition, mimetype);
				}
			});

			setWebChromeClient(new WebChromeClient() {
				@Override
				public void onProgressChanged(WebView view, int progress) {
					if (progress == 100)
						mProgress = 0;
					else mProgress = progress;

					if (isForeground) {
						loadProgress.setProgress(progress);
						if (progress == 100)
							loadProgress.setVisibility(View.INVISIBLE);
					}
				}

				// For Android 3.0+
				public void openFileChooser(ValueCallback<Uri> uploadMsg,
						String acceptType) {
					if (null == mUploadMessage)
						mUploadMessage = new wrapValueCallback();
					mUploadMessage.mInstance = uploadMsg;
					Intent i = new Intent(Intent.ACTION_GET_CONTENT);
					i.addCategory(Intent.CATEGORY_OPENABLE);
					i.setType("*/*");
					startActivityForResult(Intent.createChooser(i,
							getString(R.string.select_file)),
							FILECHOOSER_RESULTCODE);
				}

				// For Android < 3.0
				public void openFileChooser(ValueCallback<Uri> uploadMsg) {
					openFileChooser(uploadMsg, "");
				}

				
				//I don't know how to reflect a Interface, so it will crash on cupcake
				@Override
				public void onGeolocationPermissionsShowPrompt(final String origin,
						final GeolocationPermissions.Callback callback) {
					callback.invoke(origin, true, false);//use maps.google.com or map.baidu.com to verify
				}

				@Override
				public View getVideoLoadingProgressView() {
					TextView view = new TextView(mContext);
					view.setText(R.string.wait);
					view.setTextSize(20);
					return view;// this if for hint when load video
				}
				
				@Override
				public void onShowCustomView(View view,	final CustomViewCallback callback) {
					super.onShowCustomView(view, callback);

					if (view instanceof FrameLayout) {
						mCustomViewContainer = (FrameLayout) view;
						mCustomViewCallback = callback;
						if (mCustomViewContainer.getFocusedChild() instanceof VideoView) {
							mVideoView = (VideoView) mCustomViewContainer.getFocusedChild();
							mVideoView.setOnCompletionListener(new OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer mp) {
									mp.stop();
									onHideCustomView();
								}
							});
							mVideoView.setOnErrorListener(new OnErrorListener() {
								@Override
								public boolean onError(MediaPlayer mp, int what, int extra) {
									mp.stop();
									onHideCustomView();
									return true;
								}
							});
							mVideoView.requestFocus();
							mVideoView.start();
						}
						else ;//it is android.webkit.HTML5VideoFullScreen$VideoSurfaceView instead of VideoView
						
						browserView.setVisibility(View.GONE);
	                    setContentView(mCustomViewContainer);
					}
				}// API 7. http://www.w3.org/2010/05/video/mediaevents.html for verify

				public void onHideCustomView() {
					hideCustomView();
				}


				@Override
				public boolean onCreateWindow(WebView view, boolean isDialog,
						boolean isUserGesture, android.os.Message resultMsg) {
					if (openNewPage(null, webIndex+1, true, true)) {// open new page success
						((WebView.WebViewTransport) resultMsg.obj)
								.setWebView(serverWebs.get(webIndex));
						resultMsg.sendToTarget();
						return true;
					} else return false;
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

					if (isForeground) {
						// close soft keyboard
						imm.hideSoftInputFromWindow(getWindowToken(), 0);
						loadProgress.setVisibility(View.VISIBLE);
						
						if (HOME_PAGE.equals(url)) webAddress.setText(HOME_BLANK);
						else webAddress.setText(url);
						
						imgRefresh.setImageResource(R.drawable.stop);

						if (!showUrl) setUrlHeight(true);
						if (!showControlBar) setBarHeight(true);
					}

					//try {if (baiduEvent != null) baiduEvent.invoke(mContext, mContext, "1", url);
					//} catch (Exception e) {}
					
					if (!incognitoMode) recordPages();
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
					if (HOME_BLANK.equals(url)) {// some site such as weibo and
						// mysilkbaby will send
						// BLANK_PAGE when login.
						return true;// we should do nothing but return true,
									// otherwise may not login.
					} else if (!url.startsWith("http") && !url.startsWith("file")) {
						overloadAction(url);
						return true; // not allow webpage to proceed
					} else
						return false;
				}
			});
		}
	}

	private VideoView mVideoView = null;
	private WebChromeClient.CustomViewCallback mCustomViewCallback = null;
	private FrameLayout mCustomViewContainer = null;
	void hideCustomView() {
		browserView.setVisibility(View.VISIBLE);
		if (mCustomViewContainer == null) return;
		
		if (mVideoView != null) {
			// Remove the custom view from its container.
			mCustomViewContainer.removeView(mVideoView);
			mVideoView = null;
		}
		mCustomViewCallback.onCustomViewHidden();
		// Show the browser view.
		setContentView(browserView);
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
		String apkName = getName(url);
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
			List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
			if ((list != null) && !list.isEmpty()) canOpen = true;
		}

		if (canOpen) {
			try {
			new AlertDialog.Builder(mContext)
			.setTitle(getString(R.string.choose))
			.setMessage(apkName)
			.setPositiveButton(getString(R.string.open),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							if (!util.startActivity(intent, false, mContext))
								startDownload(url, contentDisposition, "yes");//download if open fail
						}
					})
			.setNeutralButton(getString(R.string.download),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							startDownload(url, contentDisposition, "yes");
						}
					})
			.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
						}
					}).show();} catch(Exception e) {}
		} else startDownload(url, contentDisposition, "yes");
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
			loadProgress.setVisibility(View.INVISIBLE);
			imgRefresh.setImageResource(R.drawable.refresh);
			webControl.setVisibility(View.INVISIBLE);
			if (HOME_PAGE.equals(url)) webAddress.setText(HOME_BLANK);
			else webAddress.setText(url);						
		}
		// update the page title in webList
		webAdapter.notifyDataSetChanged();

		String title = view.getTitle();
		if (title == null) title = url;

		if (HOME_PAGE.equals(url)) ;// do nothing
		else {
			if (browserName.equals(title)) ;
				// if title and url not sync, then sync it
				//webAddress.setText(HOME_BLANK);
			else if (!incognitoMode) {// handle the bookmark/history after load new page
				if ((mHistory.size() > 0) && (mHistory.get(mHistory.size() - 1).m_url.equals(url))) return;// already the latest, no need to update history list

				String site = getSite(url);
				TitleUrl titleUrl = new TitleUrl(title, url, site);
				mHistory.add(titleUrl);// always add it to history if visit any page.

				for (int i = mHistory.size() - 2; i >= 0; i--) {
					if (mHistory.get(i).m_url.equals(url)) {
						if (title.equals(url)) {// use meaningful title to replace title with url content
							String meaningfulTitle = mHistory.get(i).m_title;
							if (!meaningfulTitle.equals(url)) 
								mHistory.set(mHistory.size()-1, mHistory.get(i));
						}
						mHistory.remove(i);// record one url only once in the history list. clear old duplicate history if any
						updateHistory();
						return;
					} 
					else if (title.equals(mHistory.get(i).m_title)) {
						mHistory.remove(i);// only keep the latest history of the same title. display multi item with same title is not useful to user
						break;
					}
				}

				if (siteArray.indexOf(site) < 0) {
					// update the auto-complete edittext without duplicate
					urlAdapter.add(site);
					// the adapter will always
					// return 0 when get count
					// or search, so we use an
					// array to store the site.
					siteArray.add(site);
				}

				try {// try to open the png, if can't open, then need save
					FileInputStream fis = openFileInput(site + ".png");
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
							FileOutputStream fos = openFileOutput(site + ".png", 0);
							favicon.compress(Bitmap.CompressFormat.PNG, 90,	fos);
							fos.close();
						}
					} catch (Exception e) {}
				}

				while (mHistory.size() > historyCount) 
					// delete from the first history until the list is not larger than historyCount;
					 //not delete icon here. it can be clear when clear all 
					mHistory.remove(0);
				
				updateHistory();
			}
		}
	}
	
	private class MyListAdapter extends ArrayAdapter<TitleUrl> {
		ArrayList localList;
		int type = 0;// 0:bookmark, 1:history, 2:downloads

		public MyListAdapter(Context context, List<TitleUrl> titles) {
			super(context, 0, titles);
			localList = (ArrayList) titles;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			final TitleUrl tu = (TitleUrl) localList.get(position);

			if (convertView == null) {
				final LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.web_list, parent, false);
				
				convertView.setBackgroundResource(R.drawable.webname_layout);
			}

			final ImageView btnIcon = (ImageView) convertView.findViewById(R.id.webicon);
			String filename;
			if (type != 2) filename = getFilesDir().getAbsolutePath() + "/" + tu.m_site + ".png";
			else filename = getFilesDir().getAbsolutePath() + "/" + getSite(tu.m_site) + ".png";
			
			File f = new File(filename);
			if (f.exists())
				try {
					btnIcon.setImageURI(Uri.parse(filename));
					btnIcon.setVisibility(View.VISIBLE);
				} catch (Exception e) {}// catch an null pointer exception on 1.6
			else btnIcon.setVisibility(View.INVISIBLE);

			final ImageView btnDelete = (ImageView) convertView.findViewById(R.id.webclose);
			btnDelete.setVisibility(View.INVISIBLE);
			btnDelete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (type == 0) {// bookmark
						File favicon = new File(dataPath + "files/" + mBookMark.get(position).m_site + ".png");
						favicon.delete();//delete favicon of the site
						mBookMark.remove(position);
						updateBookmark();
					}
					else if (type == 1) {// history
						mHistory.remove(mHistory.size() - 1 - position);
						updateHistory();
					}
					else {// downloads
						mDownloads.remove(position);
						updateDownloads();
					}
					btnDelete.setVisibility(View.INVISIBLE);
				}
			});

			TextView webname = (TextView) convertView.findViewById(R.id.webname);
			webname.setText(tu.m_title);
			if (type == 1) webname.setTextColor(0xffddddff);
			else if (type == 2) webname.setTextColor(0xffffdd8b);

			webname.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (type != 2) {// bookmark and history
						serverWebs.get(webIndex).loadUrl(tu.m_url);
						imgBookmark.performClick();
					}
					else // open downloads file
						openDownload(tu);
				}
			});
			webname.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (btnDelete.getVisibility() == View.INVISIBLE) 
						btnDelete.setVisibility(View.VISIBLE);
					else btnDelete.setVisibility(View.INVISIBLE);
					return true;
				}
			});

			return convertView;
		}
	}
	
	public void ClearCache() {
		serverWebs.get(webIndex).clearCache(true);
		// mContext.deleteDatabase("webviewCache.db");//this may get
		// disk IO crash
		ClearFolderTask cltask = new ClearFolderTask();
		// clear cache on sdcard and in data folder
		cltask.execute(downloadPath + "cache/webviewCache/", dataPath + "cache/webviewCache/");		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == FILECHOOSER_RESULTCODE) {
			if ((null == mUploadMessage) || (null == mUploadMessage.mInstance))
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			if (mValueCallbackAvailable)
				mUploadMessage.onReceiveValue(result);
			mUploadMessage.mInstance = null;
		} else if (requestCode == SETTING_RESULTCODE) {
			boolean clearData = sp.getBoolean("clear_data", false);
			if (clearData) {
				sEdit.putBoolean("clear_data", false);

				for (int i = 0; i < webAdapter.getCount(); i++)
					serverWebs.get(i).stopLoading();// stop loading while clear

				boolean clearCache = sp.getBoolean("clear_cache", false);
				if (clearCache) ClearCache();

				boolean clearCookie = sp.getBoolean("clear_cookie", false);
				if (clearCookie) {
					CookieSyncManager.createInstance(mContext);
					CookieManager.getInstance().removeAllCookie();
				}

				boolean clearFormdata = sp.getBoolean("clear_formdata", false);
				if (clearFormdata) {
					WebViewDatabase.getInstance(mContext).clearFormData();
				}

				boolean clearPassword = sp.getBoolean("clear_password", false);
				if (clearPassword) {
					WebViewDatabase.getInstance(mContext)
							.clearHttpAuthUsernamePassword();
					WebViewDatabase.getInstance(mContext)
							.clearUsernamePassword();
				}

				boolean clearIcon = sp.getBoolean("clear_icon", false);
				if (clearIcon) {
					ClearFolderTask cltask = new ClearFolderTask();
					// clear cache on sdcard and in data folder
					cltask.execute(dataPath + "files/", "png");
				}
				
				boolean clearHome = sp.getBoolean("clear_home", false);
				if (clearHome) {
					m_homepage = null;
					sEdit.putString("homepage", null);
				}
				
				boolean clearHistory = sp.getBoolean("clear_history", false);
				boolean clearBookmark = sp.getBoolean("clear_bookmark", false);

				if (clearHistory && clearBookmark && clearCache && clearCookie
						&& clearFormdata && clearPassword && clearIcon) {// clear all
					// mContext.deleteDatabase("webview.db");//this may get disk IO crash
					ClearFolderTask cltask = new ClearFolderTask();
					// clear files folder and app_databases folder
					cltask.execute(getFilesDir().getAbsolutePath(),
							getDir("databases", MODE_PRIVATE).getAbsolutePath());
				}

				if (clearHistory) {
					mHistory.clear();
					updateHistory();
					WriteTask wtask = new WriteTask();
					wtask.execute("history");
					clearFile("searchwords");
					siteArray.clear();
					urlAdapter.clear();
				}

				if (clearBookmark) {
					mBookMark.clear();
					updateBookmark();
					WriteTask wtask = new WriteTask();
					wtask.execute("bookmark");
				}

				boolean clearDownloads = sp.getBoolean("clear_downloads", false);
				if (clearDownloads) {
					mDownloads.clear();
					updateDownloads();
					WriteTask wtask = new WriteTask();
					wtask.execute("downloads");
				}

				String message = "";
				if (clearCache)
					message += "Cache, ";
				if (clearHistory)
					message += getString(R.string.history) + ", ";
				if (clearBookmark)
					message += getString(R.string.bookmark) + ", ";
				if (clearCookie)
					message += "Cookie, ";
				if (clearFormdata)
					message += getString(R.string.formdata) + ", ";
				if (clearPassword)
					message += getString(R.string.password) + ", ";
				if (clearIcon)
					message += getString(R.string.icon) + ", ";
				if (clearHome)
					message += getString(R.string.home) + ", ";
				message = message.trim();
				if (!"".equals(message)) {
					if (message.endsWith(","))
						message = message.substring(0, message.length() - 1);
					Toast.makeText(mContext,
							message + " " + getString(R.string.data_cleared),
							Toast.LENGTH_LONG).show();
				}
			}

			boolean tmpShow = sp.getBoolean("show_statusBar", true);
			if (tmpShow != showStatusBar) {
				showStatusBar = tmpShow;
				if (!showStatusBar) {// full screen
					getWindow().setFlags(
							WindowManager.LayoutParams.FLAG_FULLSCREEN,
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
				} else {
					getWindow().clearFlags(
							WindowManager.LayoutParams.FLAG_FULLSCREEN);
				}
			}
			
			showUrl = sp.getBoolean("show_url", true);
			showControlBar = sp.getBoolean("show_controlBar", true);
			setWebpagesLayout();
			
			int tmpMode = sp.getInt("rotate_mode", 1);
			if (rotateMode != tmpMode) {
				rotateMode = tmpMode;
				if (rotateMode == 1) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				else if (rotateMode == 2) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}

			// default to full screen now
			snapFullWeb = sp.getBoolean("full_web", false);

			incognitoMode = sp.getBoolean("incognito", false);
			
			updownButton = sp.getBoolean("up_down", false);
			if (updownButton) upAndDown.setVisibility(View.VISIBLE);
			else upAndDown.setVisibility(View.INVISIBLE);
			
			shareMode = sp.getInt("share_mode", 2);
			
			searchEngine = sp.getInt("search_engine", 3);

			boolean tmpEnableProxy = sp.getBoolean("enable_proxy", false);
			int tmpLocalPort = sp.getInt("local_port", 1984);
			if ((enableProxy != tmpEnableProxy) || (localPort != tmpLocalPort)) {
				enableProxy = tmpEnableProxy;
				localPort = tmpLocalPort;
				if (enableProxy)
					ProxySettings.setProxy(mContext, "127.0.0.1", localPort);
				else
					try {
						ProxySettings.resetProxy(mContext);
					} catch (Exception e) {}
			}

			WebSettings localSettings = serverWebs.get(webIndex).getSettings();

			ua = sp.getInt("ua", 0);
			if (ua <= 1)
				localSettings.setUserAgent(ua);
			else
				localSettings.setUserAgentString(selectUA(ua));

			// hideExit = sp.getBoolean("hide_exit", true);

			readTextSize(sp); // no need to reload page if fontSize changed
			localSettings.setTextSize(textSize);

			blockImage = sp.getBoolean("block_image", false);
			localSettings.setBlockNetworkImage(blockImage);

			boolean tmpCachePrefer = sp.getBoolean("cache_prefer", false);
			if (tmpCachePrefer != cachePrefer) {
				cachePrefer = tmpCachePrefer;
				if (cachePrefer)
					localSettings
							.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
				else
					localSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
			}

			blockPopup = sp.getBoolean("block_popup", false);
			localSettings.setJavaScriptCanOpenWindowsAutomatically(blockPopup);
			// localSettings.setSupportMultipleWindows(!blockPopup);

			blockJs = sp.getBoolean("block_js", false);
			localSettings.setJavaScriptEnabled(!blockJs);

			wrapWebSettings webSettings = new wrapWebSettings(localSettings);
			overviewPage = sp.getBoolean("overview_page", false);
			//localSettings.setUseWideViewPort(overviewPage);
			localSettings.setLoadWithOverviewMode(overviewPage);

			boolean showZoom = sp.getBoolean("show_zoom", false);
			if (webSettings.setDisplayZoomControls(showZoom)) {// hide zoom
																// button by
																// default on
																// API 11 and
																// above
				localSettings.setBuiltInZoomControls(showZoom);// setDisplayZoomControls(false)
																// not work, so
																// we need
																// disable zoom
																// control
				serverWebs.get(webIndex).zoomVisible = showZoom;
			} else {
				if (showZoom)
					serverWebs.get(webIndex).setZoomControl(View.VISIBLE);
				else
					serverWebs.get(webIndex).setZoomControl(View.GONE);
			}

			boolean html5 = sp.getBoolean("html5", false);
			serverWebs.get(webIndex).html5 = html5;
			localSettings.setAppCacheEnabled(html5);// API7
			localSettings.setDatabaseEnabled(html5);// API5
			if (html5) {
				localSettings.setAppCachePath(getDir("databases", MODE_PRIVATE).getPath());// API7
				// it will cause crash on OPhone if not set the max size
				localSettings.setAppCacheMaxSize(html5cacheMaxSize);
				localSettings.setDatabasePath(getDir("databases", MODE_PRIVATE)
						.getPath());// API5. how slow will it be if set path to sdcard?
			}

			String tmpEncoding = getEncoding(sp.getInt("encoding", 0));
			if (!tmpEncoding.equals(localSettings.getDefaultTextEncodingName())) {
				if ("AUTOSELECT".equals(tmpEncoding) && "Latin-1".equals(localSettings.getDefaultTextEncodingName())) ;//not reload in this case
				else {
					localSettings.setDefaultTextEncodingName(tmpEncoding);
					// set default encoding to autoselect
					sEdit.putInt("encoding", 0);
					reloadPage();
				}
			}

			sEdit.commit();
		}
	}

	void clearFile(String filename) {
		try {// clear the pages file
			FileOutputStream fo = openFileOutput(filename, 0);
			ObjectOutputStream oos = new ObjectOutputStream(fo);
			oos.writeObject("");
			oos.flush();
			oos.close();
			fo.close();
		} catch (Exception e) {}
	}
	
	public void setWebpagesLayout() {
		setUrlHeight(showUrl);
		setBarHeight(showControlBar);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		if (showUrl) 
			lp.addRule(RelativeLayout.BELOW, R.id.urlline);
		else 
			lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		if (showControlBar) 
			lp.addRule(RelativeLayout.ABOVE, R.id.webtools);
		else 
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		webs.setLayoutParams(lp);
		webs.requestLayout();
	}
	
	void setUrlHeight(boolean showUrlNow) {
		LayoutParams lpUrl = urlLine.getLayoutParams();
		if (showUrlNow) 
			lpUrl.height = LayoutParams.WRAP_CONTENT;
		else lpUrl.height = 0;
		urlLine.requestLayout();		
		updateHistoryViewHeight();
	}
	
	void setBarHeight(boolean showBarNow) {
		LayoutParams lpBar = webTools.getLayoutParams();
		if (showBarNow) 
			lpBar.height = LayoutParams.WRAP_CONTENT;
		else lpBar.height = 0;
		webTools.requestLayout();
		updateHistoryViewHeight();
	}
	
	void hideMenu() {
		menuGrid.getLayoutParams().width = 0;
		menuGrid.requestLayout();
		menuOpened = false;
	}
	
	void hideBookmark() {
		bookmarkDownloads.getLayoutParams().width = 0;
		bookmarkDownloads.requestLayout();
		bookmarkView.setVisibility(View.VISIBLE);
		if (downloadsList != null) downloadsList.setVisibility(View.GONE);
		bookmarkOpened = false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		final HitTestResult result = ((WebView) v).getHitTestResult();
		final String url = result.getExtra();

		MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {// do the menu action
				switch (item.getItemId()) {
				case 0:// download
					String ext = null;
					if (result.getType() == HitTestResult.IMAGE_TYPE
							|| result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
						ext = ".jpg";
					startDownload(url, ext, "yes");
					break;
				case 3:// open in foreground
					openNewPage(url, webIndex+1, true, true); 
					break;
				case 4:// copy url
					ClipboardManager ClipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipMan.setText(url);
					break;
				case 5:// share url
					shareUrl("", url);
					break;
				case 6:// open in background
					openNewPage(url, webAdapter.getCount(), false, true);// use openNewPage(url, webIndex+1, true, true) for open in new tab 
					break;
				case 9: // remove bookmark
					removeFavo(bookmarkIndex);
					break;
				case 10:// add bookmark
					int historyIndex = -1;
					for (int i = 0; i < mHistory.size(); i++) {
						if (mHistory.get(i).m_url.equals(url)) {
							historyIndex = i;
							break;
						}
					}
					if (historyIndex > -1)
						addFavo(url, mHistory.get(historyIndex).m_title);
					else addFavo(url, url);
					break;
				}
				return true;
			}
		};

		// set the title to the url
		menu.setHeaderTitle(result.getExtra());
		if (url != null) {
			if (dm.heightPixels > dm.density*480) // only show this menu item on large screen
				menu.add(0, 3, 0, R.string.open_new).setOnMenuItemClickListener(handler);
			menu.add(0, 6, 0, R.string.open_background).setOnMenuItemClickListener(handler);
			menu.add(0, 5, 0, R.string.shareurl).setOnMenuItemClickListener(handler);

			if (dm.heightPixels > dm.density*480) {// only show this menu item on large screen
				boolean foundBookmark = false;
				for (int i = mBookMark.size() - 1; i >= 0; i--)
					if (mBookMark.get(i).m_url.equals(url)) {
						foundBookmark = true;
						bookmarkIndex = i;
						menu.add(0, 9, 0, R.string.remove_bookmark).setOnMenuItemClickListener(handler);
						break;
					}
				if (!foundBookmark)
					menu.add(0, 10, 0, R.string.add_bookmark).setOnMenuItemClickListener(handler);
			}
			
			menu.add(0, 0, 0, R.string.save).setOnMenuItemClickListener(handler);
			menu.add(0, 4, 0, R.string.copy_url).setOnMenuItemClickListener(handler);
		}
	}

	void createShortcut(String url, String title) {
		Intent i = new Intent(this, SimpleBrowser.class);
		i.setData(Uri.parse(url));
	    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		Intent shortcutIntent = new Intent();
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(mContext, R.drawable.explorer));
	    shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	    shortcutIntent.putExtra("duplicate", false); // Just create once
	    sendBroadcast(shortcutIntent);
	}
	

	void scrollToMain() {
		if (bookmarkOpened) hideBookmark();		
		if (menuOpened) hideMenu(); 
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (menuOpened) hideMenu();
		else {
			if ((urlLine.getLayoutParams().height == 0) || (webTools.getLayoutParams().height == 0)) {// show bars if hided 
				if (!showUrl) setUrlHeight(true);
				if (!showControlBar) setBarHeight(true);
			}
				
			menuOpened = true;
			if (menuGrid.getChildCount() == 0) initMenuDialog();
			menuGrid.getLayoutParams().width = menuWidth;
			menuGrid.requestLayout();
			if (dm.widthPixels-menuWidth-bookmarkWidth < minWebControlWidth) hideBookmark();
		}
		return false;// show system menu if return true.
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("menu");// must create one menu?
		return super.onCreateOptionsMenu(menu);
	}

	private SimpleAdapter getMenuAdapter(String[] menuNameArray,
			int[] imageResourceArray) {
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < menuNameArray.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("itemImage", imageResourceArray[i]);
			map.put("itemText", menuNameArray[i]);
			data.add(map);
		}
		SimpleAdapter simperAdapter = new SimpleAdapter(this, data,
				R.layout.icon_list, new String[] { "itemImage", "itemText" },
				new int[] { R.id.appicon, R.id.appname });
		return simperAdapter;
	}

	private void shareUrl(String title, String url) {
		if (title == null) title = "";
		if (url == null) url = "";
		
		Intent shareIntent = new Intent(Intent.ACTION_VIEW);
		shareIntent.setClassName(getPackageName(), SimpleBrowser.class.getName());
		Uri data = null;
		String from = "\n(from ";
		boolean chineseLocale = Locale.CHINA.equals(mLocale) || "easy.browser".equals(getPackageName());//easy.browser only release in China
				
		if (shareMode != 1) scrollToMain();
		switch (shareMode) {
		case 2:// facebook or weibo
			if (chineseLocale)// weibo for chinese locale
				data = Uri.parse("http://v.t.sina.com.cn/share/share.php?url=" + url + "&title=" + title + "&appkey=3792856654&ralateUid=1877224203&source=bookmark");
			else // facebook for none chinese locale
				data = Uri.parse("http://www.facebook.com/sharer.php?u=" + url + "&t=" + title + from + browserName + ")");
			break;
		case 3:
			if (chineseLocale) {// qzone for chinese locale 
				if ("".equals(title)) title = url;
				data = Uri.parse("http://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=" + url + "&desc=" + title + "&title" + title + "&site=" + browserName);
			}
			else // twitter for none chinese locale
				data = Uri.parse("http://twitter.com/intent/tweet?url=" + url + "&text=" + title + from + browserName + ")");
			break;
		case 4:
			if (chineseLocale) // tencent weibo for chinese localse
				data = Uri.parse("http://share.v.t.qq.com/index.php?c=share&a=index&url=" + url + "&title=" + title + url + from + browserName + ")");
			else // google+ for none chinese locale
				data = Uri.parse("https://plusone.google.com/_/+1/confirm?hl=en&url=" + url);
			break;
		case 1:
		default:
			if (!"".equals(title)) title = title + "\n";
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, R.string.share);
			intent.putExtra(Intent.EXTRA_TEXT, title + url + from + browserName + ")");
			util.startActivity(
					Intent.createChooser(intent, getString(R.string.sharemode)),
					true, mContext);
			return;
		}
		
		shareIntent.setData(data);
		util.startActivity(shareIntent, false, mContext);
	}

	public void readPreference() {
		blockImage = sp.getBoolean("block_image", false);
		cachePrefer = sp.getBoolean("cache_prefer", false);
		blockPopup = sp.getBoolean("block_popup", false);
		blockJs = sp.getBoolean("block_js", false);
		// hideExit = sp.getBoolean("hide_exit", true);
		overviewPage = sp.getBoolean("overview_page", false);
		ua = sp.getInt("ua", 0);
		//showZoom = sp.getBoolean("show_zoom", false);
		mLocale = getBaseContext().getResources().getConfiguration().locale;
		if ("ru_RU".equals(mLocale.toString()))
			searchEngine = sp.getInt("search_engine", 4); // yandex
		else if (Locale.CHINA.equals(mLocale)) {
			searchEngine = sp.getInt("search_engine", 2); // easou
			HOME_PAGE = "file:///android_asset/home-ch.html";
		}
		else
			searchEngine = sp.getInt("search_engine", 5); // duckduckgo
		shareMode = sp.getInt("share_mode", 2); // share by facebook/weibo by default
		snapFullWeb = sp.getBoolean("full_web", false);
		readTextSize(sp);// init the text size
		enableProxy = sp.getBoolean("enable_proxy", false);
		if (enableProxy) {
			localPort = sp.getInt("local_port", 1984);
			ProxySettings.setProxy(mContext, "127.0.0.1", localPort);
		}
		m_homepage = sp.getString("homepage", null);

		incognitoMode = sp.getBoolean("incognito", false);
		updownButton = sp.getBoolean("up_down", false);
		
		showStatusBar = sp.getBoolean("show_statusBar", true);
		showUrl = sp.getBoolean("show_url", true);
		showControlBar = sp.getBoolean("show_controlBar", true);
		
		
		rotateMode = sp.getInt("rotate_mode", 1);
		if (rotateMode == 1) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		else if (rotateMode == 2) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	String getSite(String url) {
		String site = "";
		String[] tmp = url.split("/");
		// if url is http://m.baidu.com, then url.split("/")[2] is m.baidu.com
		if (tmp.length > 2)	site = tmp[2];
		else site = tmp[0];
		
		return site;
	}
	
	public void initSnapDialog() {		
		snapView = (ImageView) getLayoutInflater().inflate(
				R.layout.snap_browser, null);
		snapDialog = new AlertDialog.Builder(this)
				.setView(snapView)
				.setTitle(R.string.browser_name)
				.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {// share
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							String snap = downloadPath + "snap/" + serverWebs.get(webIndex).getTitle() + ".png";
							FileOutputStream fos = new FileOutputStream(snap);

							bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
							fos.close();

							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setType("image/*");
							intent.putExtra(Intent.EXTRA_SUBJECT, R.string.share);
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(snap)));
							util.startActivity(Intent.createChooser(
									intent,
									getString(R.string.sharemode)),
									true, mContext);
						} catch (Exception e) {
							Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {// save
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							String title = serverWebs.get(webIndex).getTitle();
							if (title == null) title = getSite(serverWebs.get(webIndex).m_url);
							title += "(snap).png";
							String site = downloadPath + "snap/";
							String snap = site + title;
							FileOutputStream fos = new FileOutputStream(snap);
							bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
							fos.close();
							Toast.makeText(getBaseContext(), getString(R.string.save) + " " + snap, Toast.LENGTH_LONG).show();
							addDownloads(new TitleUrl(title, "file://" + snap, serverWebs.get(webIndex).m_url));
						} catch (Exception e) {
							Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {// cancel
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				}).create();
	}

	public void initSourceDialog() {		
		m_sourceDialog = new AlertDialog.Builder(this)
		.setTitle(R.string.browser_name)
		.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {// share
			@Override
			public void onClick(DialogInterface dialog,	int which) {
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.fromParts("mailto", "", null));
				intent.putExtra(Intent.EXTRA_TEXT, sourceOrCookie);
				intent.putExtra(Intent.EXTRA_SUBJECT, serverWebs.get(webIndex).getTitle());
				if (!util.startActivity(intent, false, getBaseContext())) 
					shareUrl("", sourceOrCookie);
			}
		})
		.setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {// save
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					String title = serverWebs.get(webIndex).getTitle();
					if (title == null) title = getSite(serverWebs.get(webIndex).m_url);
					title += "_" + subFolder + ".txt";
					String site = downloadPath + subFolder + "/";
					String snap = site + title;
					FileOutputStream fos = new FileOutputStream(snap);
					fos.write(sourceOrCookie.getBytes());
					fos.close();
					Toast.makeText(getBaseContext(), getString(R.string.save) + " " + snap, Toast.LENGTH_LONG).show();
					addDownloads(new TitleUrl(title, "file://" + snap, serverWebs.get(webIndex).m_url));
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
				}
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {// cancel
			@Override
			public void onClick(DialogInterface dialog,	int which) {}
		}).create();
	}
	
	public void initMenuDialog() {
		// menu icon
		int[] menu_image_array = {
				R.drawable.exit,
				R.drawable.recycle,
				R.drawable.set_home,
				R.drawable.pin,
				R.drawable.search, 
				R.drawable.copy,
				R.drawable.downloads,
				R.drawable.save,
				R.drawable.capture,
				R.drawable.html_w,
				R.drawable.favorite,
				R.drawable.link,
				R.drawable.share,
				R.drawable.about
			};
		// menu text
		String[] menu_name_array = {
				getString(R.string.exit),
				"PDF",
				getString(R.string.set_homepage),
				getString(R.string.add_shortcut),
				getString(R.string.search),
				getString(R.string.copy),
				getString(R.string.downloads),
				getString(R.string.save),
				getString(R.string.snap),
				getString(R.string.source),
				getString(R.string.bookmark),
				"cookie",
				getString(R.string.shareurl),
				getString(R.string.settings)
			};

		final Context localContext = this;
		menuGrid.setFadingEdgeLength(0);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		menuGrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:// exit
					clearFile("pages");
					ClearCache(); // clear cache when exit
					finish();
					break;
				case 1:// pdf
					scrollToMain();
					serverWebs.get(webIndex).loadUrl("http://www.web2pdfconvert.com/engine?curl=" + serverWebs.get(webIndex).m_url);
					break;
				case 2:// set homepage
					m_homepage = serverWebs.get(webIndex).getUrl();
					if (!HOME_PAGE.equals(m_homepage)) {// not set asset/home.html as home page
						sEdit.putString("homepage", m_homepage);
						sEdit.commit();
					}
					Toast.makeText(mContext, serverWebs.get(webIndex).getTitle() + " " + getString(R.string.set_homepage), Toast.LENGTH_LONG).show();
					break;
				case 3:// add short cut
					createShortcut(serverWebs.get(webIndex).getUrl(), serverWebs.get(webIndex).getTitle());
					Toast.makeText(mContext, getString(R.string.add_shortcut) + " " + serverWebs.get(webIndex).getTitle(), Toast.LENGTH_LONG).show();
					break;
				case 4:// search
					scrollToMain();
					webControl.setVisibility(View.INVISIBLE);// hide webControl when search
						// serverWebs.get(webIndex).showFindDialog("e", false);
					if (searchBar == null) initSearchBar();
					searchBar.bringToFront();
					searchBar.setVisibility(View.VISIBLE);
					etSearch.requestFocus();
					toSearch = "";
					imm.toggleSoftInput(0, 0);
					break;
				case 5:// copy
					scrollToMain();
					webControl.setVisibility(View.INVISIBLE);// hide webControl when copy
					try {
						if (Integer.decode(android.os.Build.VERSION.SDK) > 10)
							Toast.makeText(mContext,
									getString(R.string.copy_hint),
									Toast.LENGTH_LONG).show();
					} catch (Exception e) {}

					try {
						KeyEvent shiftPressEvent = new KeyEvent(0, 0,
								KeyEvent.ACTION_DOWN,
								KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
						shiftPressEvent.dispatch(serverWebs.get(webIndex));
					} catch (Exception e) {}
					break;
				case 6:// downloads
					if (mDownloads.size() == 0) {
						Toast.makeText(mContext, "no downloads recorded", Toast.LENGTH_LONG).show();
						break;
					}
					
					if (downloadsList == null) initDownloads();
					
					bookmarkView.setVisibility(View.GONE);
					downloadsList.setVisibility(View.VISIBLE);
					showBookmark();
					break;
				case 7:// save
		    		startDownload(serverWebs.get(webIndex).m_url, "", "no");
					break;
				case 8:// view snap
					try {// still got java.lang.RuntimeException: Canvas: trying
							// to use a recycled bitmap android.graphics.Bitmap
							// from one user. so catch it.
						if (!snapFullWeb) {
							// the snap will not refresh if not destroy cache
							webpages.destroyDrawingCache();
							webpages.setDrawingCacheEnabled(true);
							bmp = webpages.getDrawingCache();
						} else {
							Picture pic = serverWebs.get(webIndex)
									.capturePicture();

							// bmp = Bitmap.createScaledBitmap(???,
							// pic.getWidth(), pic.getHeight(), false);//check
							// here http://stackoverflow.com/questions/477572
							bmp = Bitmap.createBitmap(pic.getWidth(),
									pic.getHeight(), Bitmap.Config.ARGB_4444);
							// the size of the web page may be very large.

							Canvas canvas = new Canvas(bmp);
							pic.draw(canvas);
						}
						
						if (snapDialog == null) initSnapDialog();
						snapView.setImageBitmap(bmp);
						snapDialog.setTitle(serverWebs.get(webIndex).getTitle());
						if (HOME_PAGE.equals(serverWebs.get(webIndex).getUrl()))
							snapDialog.setIcon(R.drawable.explorer);
						else
							snapDialog.setIcon(new BitmapDrawable(serverWebs.get(webIndex).getFavicon()));
						snapDialog.show();
					} catch (Exception e) {
						Toast.makeText(mContext, e.toString(),
								Toast.LENGTH_LONG).show();
					}
					break;
				case 9:// view page source
					try {
						if ("".equals(serverWebs.get(webIndex).pageSource)) {
							serverWebs.get(webIndex).pageSource = "Loading... Please try again later.";
							serverWebs.get(webIndex).getPageSource();
						}

						sourceOrCookie = serverWebs.get(webIndex).pageSource;
						subFolder = "source";
						showSourceDialog();
					} catch (Exception e) {
						Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
					}
					break;
				case 10:// bookmark
					String url = serverWebs.get(webIndex).m_url;
					if (HOME_PAGE.equals(url)) return;// not add home page
					addRemoveFavo(url, serverWebs.get(webIndex).getTitle());
					break;
				case 11:// cookie
					CookieManager cookieManager = CookieManager.getInstance(); 
					String cookie = cookieManager.getCookie(serverWebs.get(webIndex).m_url);
					if (cookie != null)
						sourceOrCookie = cookie.replaceAll("; ", "\n\n");
					else sourceOrCookie = "No cookie on this page.";
					
					subFolder = "cookie";
					showSourceDialog();
					break;
				case 12:// share url
					shareUrl(serverWebs.get(webIndex).getTitle(), serverWebs.get(webIndex).m_url);
					break;
				case 13:// settings
					Intent intent = new Intent(getPackageName() + "about");
					intent.setClassName(getPackageName(), AboutBrowser.class.getName());
					startActivityForResult(intent, SETTING_RESULTCODE);
					break;
				}
			}
		});
	}
	
	void showSourceDialog() {
		if (m_sourceDialog == null) initSourceDialog();
		m_sourceDialog.setTitle(serverWebs.get(webIndex).getTitle());
		if (HOME_PAGE.equals(serverWebs.get(webIndex).getUrl()))
			m_sourceDialog.setIcon(R.drawable.explorer);
		else
			m_sourceDialog.setIcon(new BitmapDrawable(serverWebs.get(webIndex).getFavicon()));
		m_sourceDialog.setMessage(sourceOrCookie);
		m_sourceDialog.show();
	}
	
	public void initUpDown() {
		upAndDown = (LinearLayout) findViewById(R.id.up_down);
		if (updownButton) upAndDown.setVisibility(View.VISIBLE);
		else upAndDown.setVisibility(View.INVISIBLE);
		
		ImageView dragButton = (ImageView) findViewById(R.id.page_drag);
		dragButton.setAlpha(40);
		dragButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int[] temp = new int[] { 0, 0 };
				int eventAction = event.getAction();
				int x = (int) event.getRawX();
				int y = (int) event.getRawY();
				int offset = - urlLine.getHeight();
				switch (eventAction) {
				case MotionEvent.ACTION_DOWN:// prepare for drag
					if (!showUrl) setUrlHeight(showUrl);
					if (!showControlBar) setBarHeight(showControlBar);
					temp[0] = (int) event.getX();
					temp[1] = y;
					break;

				case MotionEvent.ACTION_MOVE:// drag the button
					upAndDown.layout(
							x - temp[0], 
							y - temp[1] - upAndDown.getHeight() + offset, 
							x - temp[0] + upAndDown.getWidth(), 
							y - temp[1] + offset
						);
					upAndDown.postInvalidate();
					break;

				case MotionEvent.ACTION_UP:// reset the margin when stop drag
					MarginLayoutParams lp = (MarginLayoutParams) upAndDown.getLayoutParams();
					lp.setMargins(
							0, 
							Math.min(upAndDown.getTop(), 0), 
							Math.min(Math.max(webs.getWidth()-upAndDown.getRight(), 0), webs.getWidth()-upAndDown.getWidth()), 
							Math.min(Math.max(webs.getHeight()-upAndDown.getBottom(), 0), webs.getHeight()-20)
						);
					upAndDown.setLayoutParams(lp);
					break;
				}
				return true;
			}
		});
		
		upButton = (ImageView) findViewById(R.id.page_up);
		upButton.setAlpha(40);
		Matrix matrix = new Matrix();
		matrix.postRotate(180f, 24*dm.density, 24*dm.density);
		upButton.setImageMatrix(matrix);// rotate 180 degree
		upButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int eventAction = event.getAction();
				switch (eventAction) {
				case MotionEvent.ACTION_DOWN:
					actioned = false;
					lastTime = event.getEventTime();
					timeInterval = 100;
					break;
				case MotionEvent.ACTION_MOVE:
					if (event.getEventTime() - lastTime > timeInterval) {
						timeInterval = 70;
						lastTime = event.getEventTime();
						scrollUp();
					}
					break;
				case MotionEvent.ACTION_UP:
					if (!actioned) scrollUp();
					break;
				}
				return true;
			}
		});
		
		downButton = (ImageView) findViewById(R.id.page_down);
		downButton.setAlpha(40);
		downButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int eventAction = event.getAction();
				switch (eventAction) {
				case MotionEvent.ACTION_DOWN:
					actioned = false;
					lastTime = event.getEventTime();
					timeInterval = 100;// the interval for first scroll is longer than the after, to avoid scroll twice
					break;
				case MotionEvent.ACTION_MOVE:
					if (event.getEventTime() - lastTime > timeInterval) {
						timeInterval = 70;
						lastTime = event.getEventTime();
						scrollDown();
					}
					break;
				case MotionEvent.ACTION_UP:
					if (!actioned) scrollDown();
					break;
				}
				return true;
			}
		});
	}
	
	long lastTime = 0;
	long timeInterval = 100;
	boolean actioned = false;
	void scrollUp() {
		actioned = true;
		if (!showUrl) setUrlHeight(showUrl);
		if (!showControlBar) setBarHeight(showControlBar);
		if (serverWebs.get(webIndex).myCanScrollVertically(-1))// pageUp/pageDown have animation which is slow
			serverWebs.get(webIndex).scrollBy(0, -serverWebs.get(webIndex).getHeight()+10);		
	}
	void scrollDown() {
		actioned = true;
		if (!showUrl) setUrlHeight(showUrl);
		if (!showControlBar) setBarHeight(showControlBar);
		if (serverWebs.get(webIndex).myCanScrollVertically(1))
			serverWebs.get(webIndex).scrollBy(0, serverWebs.get(webIndex).getHeight()-10);		
	}
	
	public void initSearchBar() {		
		imgSearchPrev = (ImageView) findViewById(R.id.search_prev);
		imgSearchPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!toSearch.equals(etSearch.getText().toString()))
					findMatchCount();
				else if (matchCount > 0) {
					serverWebs.get(webIndex).findNext(false);
					matchIndex -= 1;
					if (matchIndex < 0) {
						while (matchIndex < matchCount - 1) {
							serverWebs.get(webIndex).findNext(true);
							matchIndex += 1;
						}
					}
				}

				if (matchCount > 0)
					searchHint.setText((matchIndex + 1) + " of " + matchCount);
				else
					searchHint.setText("0 of 0");
			}
		});
		imgSearchNext = (ImageView) findViewById(R.id.search_next);
		imgSearchNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!toSearch.equals(etSearch.getText().toString()))
					findMatchCount();
				else if (matchCount > 0) {
					serverWebs.get(webIndex).findNext(true);
					matchIndex += 1;
					if (matchIndex >= matchCount)
						while (matchIndex > 0) {
							serverWebs.get(webIndex).findNext(false);
							matchIndex -= 1;
						}
				}

				if (matchCount > 0)
					searchHint.setText((matchIndex + 1) + " of " + matchCount);
				else
					searchHint.setText("0 of 0");
			}
		});

		imgSearchClose = (ImageView) findViewById(R.id.close_search);
		imgSearchClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				hideSearchBox();
			}
		});

		searchBar = (RelativeLayout) findViewById(R.id.search_bar);
		searchHint = (TextView) findViewById(R.id.search_hint);
		etSearch = (EditText) findViewById(R.id.search);
		etSearch.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP)
					switch (keyCode) {
					case KeyEvent.KEYCODE_SEARCH:
					case KeyEvent.KEYCODE_ENTER:
					case KeyEvent.KEYCODE_DPAD_CENTER:
						imgSearchNext.performClick();
						break;
					}
				return false;
			}
		});		
	}
	
	public void initWebControl() {
		// web control
		webControl = (LinearLayout) browserView.findViewById(R.id.webcontrol);
		fakeWebControl = (LinearLayout) browserView.findViewById(R.id.fakeWebcontrol);
		
		btnNewpage = (Button) browserView.findViewById(R.id.opennewpage);
		btnNewpage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {// add a new page
				if (m_homepage != null) openNewPage(m_homepage, webIndex+1, true, false);
				else openNewPage("", webIndex+1, true, false);
			}
		});
		// web list
		webAdapter = new WebAdapter(mContext, serverWebs);
		webList = (ListView) browserView.findViewById(R.id.weblist);
		//webList.inflate(mContext, R.layout.web_list, null);
		webList.setFadingEdgeLength(0);// no shadow when scroll
		webList.setScrollingCacheEnabled(false);
		webList.setAdapter(webAdapter);
	}
	
	void initDownloads() {
		downloadsAdapter = new MyListAdapter(mContext, mDownloads);
		downloadsAdapter.type = 2;
		downloadsList = (ListView) findViewById(R.id.downloads);
		downloadsList.inflate(mContext, R.layout.web_list, null);
		downloadsList.setAdapter(downloadsAdapter);
		downloadsList.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
					openDownload(mDownloads.get(((ListView) v).getSelectedItemPosition()));
				return false;
			}
		});		
	}
	
	void openDownload(TitleUrl tu) {
		Intent intent = new Intent("android.intent.action.VIEW");
		
		String ext = tu.m_title.substring(tu.m_title.lastIndexOf(".")+1, tu.m_title.length());
		MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
		String mimeType = mimeTypeMap.getMimeTypeFromExtension(ext);
		if (mimeType != null) intent.setDataAndType(Uri.parse(tu.m_url), mimeType);
		else intent.setData(Uri.parse(tu.m_url));// we can open it now
		
		util.startActivity(intent, true, mContext);
	}
	
	public void initBookmarks() {
		// set background tile of bookmark view. not use now
		/*Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.noise);  
		BitmapDrawable drawable = new BitmapDrawable(bitmap);  
		drawable.setTileModeXY(TileMode.REPEAT , TileMode.REPEAT );
		drawable.setDither(true);  
		bookmarkView.setBackgroundDrawable(drawable);*/ 
		
		bookmarkAdapter = new MyListAdapter(mContext, mBookMark);
		bookmarkAdapter.type = 0;
		ListView bookmarkList = (ListView) findViewById(R.id.bookmark);
		bookmarkList.inflate(mContext, R.layout.web_list, null);
		bookmarkList.setAdapter(bookmarkAdapter);
		bookmarkList.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					ListView lv = (ListView) v;
					serverWebs.get(webIndex).loadUrl(mBookMark.get(lv.getSelectedItemPosition()).m_url);
					imgBookmark.performClick();
				}
				return false;
			}
		});

		historyAdapter = new MyListAdapter(mContext, mHistoryForAdapter);
		historyAdapter.type = 1;
		historyList = (ListView) findViewById(R.id.history);
		historyList.inflate(mContext, R.layout.web_list, null);
		historyList.setAdapter(historyAdapter);
		historyList.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					ListView lv = (ListView) v;
					serverWebs.get(webIndex).loadUrl(mHistory.get(mHistory.size() - 1 - lv.getSelectedItemPosition()).m_url);
					imgBookmark.performClick();
				}
				return false;
			}
		});
		
		updateHistory();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// mContext = getApplicationContext();//this will cause force close when
		// select locale in google translate
		mContext = this;

		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		browserName = getString(R.string.browser_name);
		
		// init settings
		sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		sEdit = sp.edit();
		readPreference();

		nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		downloadAppID = new ArrayList();
		appstate = ((MyApp) getApplicationContext());

		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

		// hide titlebar of application, must be before setting the layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
        LayoutInflater inflater = LayoutInflater.from(this);
        
        browserView = (RelativeLayout) inflater.inflate(R.layout.browser, null);
        setContentView(browserView);
        bookmarkDownloads = browserView.findViewById(R.id.bookmarkDownloads);
        bookmarkView = (LinearLayout) browserView.findViewById(R.id.bookmarkView);
		menuGrid = (GridView) browserView.findViewById(R.id.grid_menu);

		initWebControl();

		loadProgress = (ProgressBar) findViewById(R.id.loadprogress);

		imgAddFavo = (ImageView) findViewById(R.id.addfavorite);
		imgAddFavo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String url = serverWebs.get(webIndex).m_url;
				if (HOME_PAGE.equals(url)) return;// not add home page

				addRemoveFavo(url, serverWebs.get(webIndex).getTitle());
			}
		});
		imgAddFavo.setOnLongClickListener(new OnLongClickListener() {// long click to show bookmark
			@Override
			public boolean onLongClick(View arg0) {
				CharSequence bookmarks[] = new CharSequence[mBookMark.size()];
				for (int i = 0; i < mBookMark.size(); i++)
				{
					bookmarks[i] = mBookMark.get(i).m_title;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(getString(R.string.bookmark));
				builder.setItems(bookmarks, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        // the user clicked on engine[which]
						serverWebs.get(webIndex).loadUrl(mBookMark.get(which).m_url);
				    }
				});
				builder.show();
				return true;
			}
		});

		webAddress = (AutoCompleteTextView) findViewById(R.id.url);
		webAddress.bringToFront();
		webAddress.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				gotoUrl(urlAdapter.getItem(position));
			}

		});
		webAddress.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				gotoUrl(webAddress.getText().toString().toLowerCase());
				return false;
			}
		});
		webAddress.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				view.setFocusableInTouchMode(true);
				serverWebs.get(webIndex).setFocusable(false);
				return false;
			}
		});
		webAddress.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				imgRefresh.setImageResource(R.drawable.go);
				shouldGo = true;
				return false;
			}
		});

		dataPath = "/data/data/" + getPackageName() + "/";
		downloadPath = util.preparePath(mContext);
		if (downloadPath == null) downloadPath = dataPath;
		if (downloadPath.startsWith(dataPath)) noSdcard = true;

		// should read in below sequence: 1, sdcard. 2, data/data. 3, native browser
		try {
			FileInputStream fi = null;
			if (noSdcard) fi = openFileInput("history");
			else {
				try {// try to open history on sdcard at first
					File file = new File(downloadPath + "bookmark/history");
					fi = new FileInputStream(file);
				} catch (FileNotFoundException e) {// read from /data/data if fail
					noHistoryOnSdcard = true;
					fi = openFileInput("history");
				}
			}
			
			try {// close anyway
				fi.close();
			} catch (Exception e) {}
		} catch (FileNotFoundException e1) {
			firstRun = true;
		}
		
		siteArray = new ArrayList<String>();
		urlAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_dropdown_item_1line,
				new ArrayList<String>());
		getHistoryList();// read history and bookmark from native browser
		String site;
		for (int i = 0; i < mSystemHistory.size(); i++) {
			site = mSystemHistory.get(i).m_site;
			if (siteArray.indexOf(site) < 0) {
				siteArray.add(site);
				urlAdapter.add(site);
			}
		}
		for (int i = 0; i < mSystemBookMark.size(); i++) {
			site = mSystemBookMark.get(i).m_site;
			if (siteArray.indexOf(site) < 0) {
				siteArray.add(site);
				urlAdapter.add(site);
			}
		}
		
		if (!firstRun) {
			mHistory = readBookmark("history");
			mBookMark = readBookmark("bookmark");
			mDownloads = readBookmark("downloads");
			
			Collections.sort(mBookMark, new myComparator());

			for (int i = 0; i < mHistory.size(); i++) {
				site = mHistory.get(i).m_site;
				if (siteArray.indexOf(site) < 0) {
					siteArray.add(site);
					urlAdapter.add(site);
				}
			}
			for (int i = 0; i < mBookMark.size(); i++) {
				site = mBookMark.get(i).m_site;
				if (siteArray.indexOf(site) < 0) {
					siteArray.add(site);
					urlAdapter.add(site);
				}
			}

			// read search words
			ObjectInputStream ois = null;
			FileInputStream fi = null;
			String word = null;
			try {
				fi = openFileInput("searchwords");
				ois = new ObjectInputStream(fi);
				while ((word = (String) ois.readObject()) != null) {
					if (siteArray.indexOf(word) < 0) {
						siteArray.add(word);
						urlAdapter.add(word);
					}
				}
			} catch (EOFException e) {// only when read eof need send out msg.
				try {
					ois.close();
					fi.close();
				} catch (Exception e1) {}
			} catch (Exception e) {}
		}
		else {// copy from system bookmark if first run
			for (int i = 0; i < mSystemHistory.size(); i++) {
				if (i > historyCount) break;
				mHistory.add(mSystemHistory.get(i));
			}

			for (int i = 0; i < mSystemBookMark.size(); i++)
				mBookMark.add(mSystemBookMark.get(i));
			Collections.sort(mBookMark, new myComparator());

			historyChanged = true;
			bookmarkChanged = true;
		}

		urlAdapter.sort(new StringComparator());
		webAddress.setAdapter(urlAdapter);

		cm = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
		WebIconDatabase.getInstance().open(
				getDir("databases", MODE_PRIVATE).getPath());
		webIndex = 0;
		serverWebs.add(new MyWebview(mContext));
		webpages = (MyViewFlipper) findViewById(R.id.webpages);
		webpages.addView(serverWebs.get(webIndex));

		webTools = (LinearLayout) findViewById(R.id.webtools);
		urlLine = (LinearLayout) findViewById(R.id.urlline);
		webs = (RelativeLayout) findViewById(R.id.webs);
		
		adContainer = (FrameLayout) findViewById(R.id.adContainer);
		adContainer2 = (LinearLayout) findViewById(R.id.adContainer2);
		imageBtnList = (LinearLayout) findViewById(R.id.imagebtn_list);
		imageBtnList.bringToFront();
		
		if (!showStatusBar) 
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

		imgNext = (ImageView) findViewById(R.id.next);
		imgNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (serverWebs.get(webIndex).canGoForward())
					serverWebs.get(webIndex).goForward();
			}
		});
		imgNext.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				CharSequence operations[] = new CharSequence[] {getString(R.string.scroll_top), getString(R.string.page_up), getString(R.string.page_down), getString(R.string.scroll_bottom)};
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setSingleChoiceItems(operations, -1, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
			    		ListView lw = ((AlertDialog)dialog).getListView();
				    	switch(which) {
				    	case 0:
				    		serverWebs.get(webIndex).pageUp(true);
				    		dialog.dismiss();
				    		break;
				    	case 1:
				    		serverWebs.get(webIndex).pageUp(false);
				    		lw.clearChoices();
				    		break;
				    	case 2:
				    		serverWebs.get(webIndex).pageDown(false);
				    		lw.clearChoices();
				    		break;
				    	case 3:
				    		serverWebs.get(webIndex).pageDown(true);
				    		dialog.dismiss();
				    		break;
				    	}
				    }
				}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
				    	dialog.dismiss();
					}
				});
				builder.show();
				return true;
			}
		});
		
		imgPrev = (ImageView) findViewById(R.id.prev);
		imgPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (serverWebs.get(webIndex).canGoBack())
					serverWebs.get(webIndex).goBack();
				else if (serverWebs.get(webIndex).shouldCloseIfCannotBack)
					closePage(webIndex, false);
			}
		});
		imgPrev.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				final WebBackForwardList wbfl = serverWebs.get(webIndex).copyBackForwardList();
				if ((wbfl != null) && !incognitoMode) {
					int size = wbfl.getSize();
					final int current = wbfl.getCurrentIndex();
					if (size > 0) {
						CharSequence historys[] = new CharSequence[size];
						for (int i = 0; i < size; i++)
							historys[i] = wbfl.getItemAtIndex(i).getTitle();

						AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
						builder.setSingleChoiceItems(historys, current, new DialogInterface.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						    	serverWebs.get(webIndex).goBackOrForward(which - current);
						    	dialog.dismiss();
						    }
						});
						builder.show();
					}
				}
				return true;
			}
		});
		
		imgRefresh = (ImageView) findViewById(R.id.refresh);
		imgRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (shouldGo) {
					shouldGo = false;
					gotoUrl(webAddress.getText().toString().toLowerCase());
				}
				else reloadPage();
			}
		});
		imgRefresh.setOnLongClickListener(new OnLongClickListener() {// long click to select search engine
			@Override
			public boolean onLongClick(View arg0) {
				CharSequence engine[] = new CharSequence[] {getString(R.string.bing), getString(R.string.baidu), getString(R.string.google), getString(R.string.yandex), getString(R.string.duckduckgo)};
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(getString(R.string.search_engine));
				builder.setSingleChoiceItems(engine, searchEngine-1, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        // the user clicked on engine[which]
				    	searchEngine = which + 1;
				    	sEdit.putInt("search_engine", searchEngine);
				    	sEdit.commit();
				    	dialog.dismiss();
						gotoUrl(webAddress.getText().toString().toLowerCase());
				    }
				});
				builder.show();
				return true;
			}
		});
		
		imgBookmark = (ImageView) findViewById(R.id.bookmark_icon);
		imgBookmark.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (bookmarkOpened) {
					if ((downloadsList != null) && (downloadsList.getVisibility() == View.VISIBLE)) {
						bookmarkView.setVisibility(View.VISIBLE);
						downloadsList.setVisibility(View.GONE);
					}
					else hideBookmark();
				}
				else showBookmark(); 
			}
		});
		imgBookmark.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				if (!bookmarkOpened) showBookmark();
				exchangePosition();
				return true;
			}
		});
		
		imgMenu = (ImageView) findViewById(R.id.menu);
		imgMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				onMenuOpened(0, null);
			}
		});
		imgMenu.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				CharSequence operations[] = new CharSequence[] {
						getString(R.string.full_screen),
						getString(R.string.incognito),
						getString(R.string.page_updown),
						getString(R.string.block_image),
						getString(R.string.show_zoom),
						getString(R.string.overview_page),
						getString(R.string.hide)
				};
				boolean checkeditems[] = new boolean[] {
						!(showUrl || showControlBar || showStatusBar),
						incognitoMode, 
						updownButton, 
						blockImage, 
						serverWebs.get(webIndex).zoomVisible, 
						overviewPage, 
						false
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMultiChoiceItems(operations, checkeditems, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean selected) {
						WebSettings localSettings = serverWebs.get(webIndex).getSettings();
						wrapWebSettings webSettings = new wrapWebSettings(localSettings);
						switch(which) {
						case 0:
							boolean fullScreen = selected;
							showUrl = !fullScreen;
							showControlBar = !fullScreen;
							showStatusBar = !fullScreen;
							if (fullScreen) {
								getWindow().setFlags(
										WindowManager.LayoutParams.FLAG_FULLSCREEN,
										WindowManager.LayoutParams.FLAG_FULLSCREEN);								
							}
							else {
								getWindow().clearFlags(
										WindowManager.LayoutParams.FLAG_FULLSCREEN);
							}
							setWebpagesLayout();
							sEdit.putBoolean("show_url", showUrl);
							sEdit.putBoolean("show_controlBar", showControlBar);
							sEdit.putBoolean("show_statusBar", showStatusBar);
							break;
						case 1:
							incognitoMode = selected;
							sEdit.putBoolean("incognito", incognitoMode);
							break;
						case 2:
							updownButton = selected;
							if (updownButton) upAndDown.setVisibility(View.VISIBLE);
							else upAndDown.setVisibility(View.INVISIBLE);
							sEdit.putBoolean("up_down", updownButton);
							break;
						case 3:
							blockImage = selected;
							localSettings.setBlockNetworkImage(blockImage);
							sEdit.putBoolean("block_image", blockImage);
							break;
						case 4:
							boolean showZoom = selected;
							if (webSettings.setDisplayZoomControls(showZoom)) {
								localSettings.setBuiltInZoomControls(showZoom);
								serverWebs.get(webIndex).zoomVisible = showZoom;
							} else {
								if (showZoom)
									serverWebs.get(webIndex).setZoomControl(View.VISIBLE);
								else
									serverWebs.get(webIndex).setZoomControl(View.GONE);
							}
							sEdit.putBoolean("show_zoom", showZoom);
							break;
						case 5:
							overviewPage = selected;
							//localSettings.setUseWideViewPort(overviewPage);
							localSettings.setLoadWithOverviewMode(overviewPage);
							sEdit.putBoolean("overview_page", overviewPage);
							break;
						case 6:
							moveTaskToBack(true);
							break;
						}
						sEdit.commit();
						dialog.dismiss();
					}
				});
				builder.show();
				return true;
			}
		});
		
		imgNew = (ImageView) findViewById(R.id.newpage);
		imgNew.setImageBitmap(util.generatorCountIcon(util.getResIcon(getResources(), R.drawable.newpage), 1, 2, dm.density, mContext));
		imgNew.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (webControl.getVisibility() == View.INVISIBLE) {
					if (urlLine.getLayoutParams().height == 0) setUrlHeight(true);// show url if hided
				
					if (webControl.getWidth() < minWebControlWidth) scrollToMain();// otherwise may not display weblist correctly
					webAdapter.notifyDataSetInvalidated();
					webControl.setVisibility(View.VISIBLE);
				} else webControl.setVisibility(View.INVISIBLE);
			}
		});
		imgNew.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				CharSequence historys[] = new CharSequence[mHistory.size()];
				for (int i = 0; i < mHistory.size(); i++)
				{
					historys[i] = mHistory.get(i).m_title;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle(getString(R.string.history));
				builder.setItems(historys, new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        // the user clicked on engine[which]
						serverWebs.get(webIndex).loadUrl(mHistory.get(which).m_url);
				    }
				});
				builder.show();
				return true;
			}
		});

		createAd();
		setLayout();
		hideMenu();
		hideBookmark();
		setWebpagesLayout();
		//initMenuDialog();// if not init here, it will show blank on some device with scroll ball?
		//initBookmarks();
		initUpDown();

		urlLine.bringToFront();// set the z-order
		webTools.bringToFront();
		
		final FrameLayout toolAndAd = (FrameLayout) findViewById(R.id.webtoolnad);
		toolAndAd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				exchangePosition();
			}
		});

		try {// there are a null pointer error reported for the if line below,
				// hard to reproduce, maybe someone use instrument tool to test
				// it. so just catch it.
			if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
				if (!incognitoMode && readPages("pages")) {
					closePage(0, false);// the first page is no use if open saved url or homepage
				}
				else if ((m_homepage != null) && !"".equals(m_homepage)) serverWebs.get(webIndex).loadUrl(m_homepage);
				else loadPage();// load about:blank if no url saved or homepage specified
			}
			else serverWebs.get(webIndex).loadUrl(getIntent().getDataString());
		} catch (Exception e) {}

		// for package added
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addDataScheme("package");
		registerReceiver(packageReceiver, filter);

		filter = new IntentFilter("simpleHome.action.START_DOWNLOAD");
		registerReceiver(downloadReceiver, filter);

		//MenuDrawer mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT, Position.LEFT);
        //mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
        //mMenuDrawer.setDropShadowEnabled(false);
		/*MenuDrawer mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.BEHIND, Position.LEFT, MenuDrawer.MENU_DRAG_WINDOW);
        mMenuDrawer.setContentView(browserView);
        browserView.removeView(bookmarkDownloads);
        bookmarkDownloads.setVisibility(View.VISIBLE);
        mMenuDrawer.setMenuView(bookmarkDownloads);*/
}
	
	void exchangePosition() {
		//reverse the position of buttons and ads
		LayoutParams lp1 = imageBtnList.getLayoutParams();
		LayoutParams lp2 = adContainer2.getLayoutParams();
		
		lp1.height = (int)(50 * dm.density);
		lp2.height = (int)(40 * dm.density);
		
		imageBtnList.setLayoutParams(lp2);
		adContainer2.setLayoutParams(lp1);
		
		//if (adContainer2.getVisibility() == View.GONE) // but maybe should open for change priorty of left/right hand? 
			//return; // no need to change position of bookmark if not width enough
		
		//change position of bookmark and menu
		lp1 = bookmarkDownloads.getLayoutParams();
		lp2 = menuGrid.getLayoutParams();
		int tmp = lp1.width;
		lp1.width = lp2.width;
		lp2.width = tmp;
		bookmarkDownloads.setLayoutParams(lp2);
		menuGrid.setLayoutParams(lp1);
		
		//revert toLeft and toRight of webControl
		lp1 = webControl.getLayoutParams();
		lp2 = fakeWebControl.getLayoutParams();
		webControl.setLayoutParams(lp2);
		fakeWebControl.setLayoutParams(lp1);
		
		// revert webList
		revertCount++;
		if ((revertCount > 1) && (revertCount % 2 == 0))
			needRevert = true;
		else needRevert = false;
		webList.invalidateViews();
		
		if (revertCount % 2 == 0) reverted = false;
		else reverted = true;
		
		// revert add bookmark and refresh button
		lp1 = imgAddFavo.getLayoutParams();
		lp2 = imgRefresh.getLayoutParams();
		imgAddFavo.setLayoutParams(lp2);
		imgRefresh.setLayoutParams(lp1);
	}

	void getTitleHeight() {
		Rect rectgle= new Rect();
		Window window= getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		statusBarHeight = rectgle.top;
	}
	
	void showBookmark() {
		bookmarkDownloads.getLayoutParams().width = bookmarkWidth;
		bookmarkDownloads.requestLayout();
		bookmarkOpened = true;
		if (dm.widthPixels-menuWidth-bookmarkWidth < minWebControlWidth) {
			webControl.setVisibility(View.INVISIBLE);
			hideMenu();
		}
		if (bookmarkAdapter == null) initBookmarks();
	}
	
	void reloadPage() {
		if (loadProgress.getVisibility() == View.VISIBLE) {
			imgRefresh.setImageResource(R.drawable.refresh);
			// webpage is loading then stop it
			serverWebs.get(webIndex).stopLoading();
			loadProgress.setVisibility(View.INVISIBLE);
		} else {// reload the webpage
			imgRefresh.setImageResource(R.drawable.stop);
			loadProgress.setProgress(1);// to make it seems feedback more fast
			String url = serverWebs.get(webIndex).getUrl();
			String m_url = serverWebs.get(webIndex).m_url;
			if (m_url.equals(url))
                serverWebs.get(webIndex).reload();
			else 
				serverWebs.get(webIndex).loadUrl(m_url);
		}		
	}
	
	boolean readPages(String filename) {
		ObjectInputStream ois = null;
		FileInputStream fi = null;
		String url = null;
		try {
			fi = openFileInput(filename);
			ois = new ObjectInputStream(fi);
			while ((url = (String) ois.readObject()) != null) {
				if (!"".equals(url)) openNewPage(url, webAdapter.getCount(), false, false);
			}
		} catch (EOFException e) {// only when read eof need send out msg.
			try {
				ois.close();
				fi.close();
			} catch (Exception e1) {}
		} catch (Exception e) {}

		return ((url != null) && !"".equals(url));
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(downloadReceiver);
		unregisterReceiver(packageReceiver);

		if (adview != null) adview.destroy();
		if (adview2 != null) adview2.destroy();

		super.onDestroy();
	}

	BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			startDownload(intent.getStringExtra("url"), null, "yes");
		}
	};

	BroadcastReceiver packageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
				// it always in the format of package:x.y.z
				String packageName = intent.getDataString().split(":")[1];
				for (int i = 0; i < downloadAppID.size(); i++) {
					// cancel download notification if install succeed
					if (downloadAppID.get(i).packageName.equals(packageName)) {
						// only remove the notification internal id, not delete
						// file.
						// otherwise when user click the ad again, it will
						// download again.
						// traffic is important than storage.
						// user can download it manually when click downloads
						nManager.cancel(downloadAppID.get(i).notificationID);
						downloadAppID.remove(i);
						break;
					}
				}

			}
		}
	};

	void gotoUrl(String url) {
		if (HOME_BLANK.equals(url)) url = HOME_PAGE;
		else if (!url.contains("://")) {
			if (!url.contains(".")) {
				if ((!incognitoMode) && (siteArray.indexOf(url) < 0)) {
					siteArray.add(url);
					urlAdapter.add(url);
					try {// write to /data/data/easy.browser/files/
						FileOutputStream fo = openFileOutput("searchwords", MODE_APPEND);
						ObjectOutputStream oos = new ObjectOutputStream(fo);
						oos.writeObject(url);// record new search word
						oos.flush();
						oos.close();
						fo.close();
					} catch (Exception e) {}
				}
				
				switch (searchEngine) {
				case 1:// bing
					url = "http://www.bing.com/search?q=" + url;
					break;
				case 2:// baidu
					url = "http://www.baidu.com/s?word=" + url;
					break;
				case 3:// google
					url = "http://www.google.com/search?q=" + url;
					break;
				case 4:// yandex
					url = "http://yandex.ru/touchsearch?clid=1911434&text=" + url;
					break;
				case 5:// DuckDuckGo
				default:
					url = "https://duckduckgo.com/?t=easybrowser&q=" + url;
					break;
				}
			}
			else url = "http://" + url;
		}
		
		if (!url.equals(serverWebs.get(webIndex).getUrl())) serverWebs.get(webIndex).loadUrl(url);//only load page if input different url
	}

	void changePage(int position) {
		while (webpages.getDisplayedChild() != position)
			webpages.showNext();
		for (int i = 0; i < serverWebs.size(); i++) {
			serverWebs.get(i).isForeground = false;
			serverWebs.get(i).freeMemory();
		}
		serverWebs.get(position).isForeground = true;
		webIndex = position;
		String url = serverWebs.get(webIndex).m_url;
		if (url == null) url = "";
		else if (HOME_PAGE.equals(url)) url = HOME_BLANK;
		webAddress.setText(url);// refresh the display url

		// global settings
		WebSettings localSettings = serverWebs.get(webIndex).getSettings();
		// localSettings.setBuiltInZoomControls(showZoom);
		if (ua <= 1)
			localSettings.setUserAgent(ua);
		else
			localSettings.setUserAgentString(selectUA(ua));
		localSettings.setTextSize(textSize);
		localSettings.setBlockNetworkImage(blockImage);
		if (cachePrefer)
			localSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		else
			localSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
		localSettings.setJavaScriptCanOpenWindowsAutomatically(blockPopup);
		// localSettings.setSupportMultipleWindows(true);
		localSettings.setJavaScriptEnabled(!blockJs);
		//localSettings.setUseWideViewPort(overviewPage);
		localSettings.setLoadWithOverviewMode(overviewPage);

		if (serverWebs.get(position).mProgress > 0) {
			imgRefresh.setImageResource(R.drawable.stop);
			loadProgress.setVisibility(View.VISIBLE);
			loadProgress.setProgress(serverWebs.get(position).mProgress);
		} else {
			imgRefresh.setImageResource(R.drawable.refresh);
			loadProgress.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {// open file from sdcard
		if (!Intent.ACTION_MAIN.equals(intent.getAction())) {
			String uri = intent.getDataString();
			if (uri == null)
				return;

			boolean found = false;
			int blankIndex = -1;
			for (int i = 0; i < serverWebs.size(); i++) {
				String url = serverWebs.get(i).m_url;
				if ((uri + "/").equals(url) || uri.equals(url)) {
					changePage(i); // show correct page
					found = true;
					break;
				} else if (HOME_PAGE.equals(url))
					blankIndex = i;
			}

			if (!found) {
				if (blankIndex < 0)
					openNewPage(uri, webIndex + 1, true, true);
				else {
					serverWebs.get(blankIndex).loadUrl(uri);
					changePage(blankIndex);
				}
			}
		}

		super.onNewIntent(intent);
	}

	void addRemoveFavo(String url, String title) {
		for (int i = mBookMark.size() - 1; i >= 0; i--)
			if (mBookMark.get(i).m_url.equals(url)) {
				removeFavo(i);
				return;
			}

		addFavo(url, title);// add favo if not found it
	}
	
	void removeFavo(final int order) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.remove_bookmark)
				.setMessage(mBookMark.get(order).m_title)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {//index out of bound error reported by a few user
									mBookMark.remove(order);
									updateBookmark();
								} catch (Exception e) {}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();
	}

	void removeHistory(final int order) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.remove_history)
				.setMessage(mHistory.get(order).m_title)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {//index out of bound error reported by a few user
									mHistory.remove(order);
									updateHistory();
								} catch (Exception e) {}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();
	}

	private void addFavo(final String url, final String title) {
		if (url == null) {
			Toast.makeText(mContext, "null url", Toast.LENGTH_LONG).show();
			return;
		}

		LinearLayout favoView = (LinearLayout) getLayoutInflater().inflate(
				R.layout.addfavo_browser, null);
		titleText = (EditText) favoView.findViewById(R.id.edit_favo);
		titleText.setText(title);
		titleText.setSelection(titleText.getText().length());

		// need user's confirm to add to bookmark
		new AlertDialog.Builder(this)
				.setView(favoView)
				.setMessage(url)
				.setTitle(R.string.add_bookmark)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String site = getSite(url);
								String title = titleText.getText().toString();
								// add a blank character to occupy the space
								if ("".equals(title)) title += (char) 0xa0;
								TitleUrl titleUrl = new TitleUrl(title, url, site);
								mBookMark.add(titleUrl);
								// sort by name
								Collections.sort(mBookMark, new myComparator());
								updateBookmark();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();
	}

	void addDownloads(TitleUrl tu) {
		for (int i = 0; i < mDownloads.size(); i++)
			if (mDownloads.get(i).m_url.equals(tu.m_url))
				return;
		
		mDownloads.add(tu);
		Collections.sort(mDownloads, new myComparator());// sort by name
		updateDownloads();
	}
	
	private boolean openNewPage(String url, int newIndex, boolean changeToNewPage, boolean closeIfCannotBack) {
		boolean result = true;

		if (webAdapter.getCount() == 9) {// max pages is 9
			Toast.makeText(mContext, R.string.nomore_pages, Toast.LENGTH_LONG).show();
			return false; // not open new page if got max pages
		} else {
			webAdapter.insert(new MyWebview(mContext), newIndex);
			webAdapter.notifyDataSetInvalidated();
			webpages.addView(webAdapter.getItem(newIndex), newIndex);
			imgNew.setImageBitmap(util.generatorCountIcon(
					util.getResIcon(getResources(), R.drawable.newpage),
					webAdapter.getCount(), 
					2,
					dm.density,
					mContext));
			if (changeToNewPage) changePage(newIndex);
			else serverWebs.get(newIndex).isForeground = false;
			serverWebs.get(newIndex).closeToBefore = changeToNewPage;
			serverWebs.get(newIndex).shouldCloseIfCannotBack = closeIfCannotBack;
		}

		if (url != null) {
			if ("".equals(url)) loadPage();
			// else if (url.endsWith(".pdf"))//can't open local pdf by google
			// doc
			// serverWebs.get(webIndex).loadUrl("http://docs.google.com/gview?embedded=true&url="
			// + url);
			else {
				try {url = URLDecoder.decode(url);} catch (Exception e) {}
				serverWebs.get(newIndex).loadUrl(url);
			}
		}

		return result;
	}

	void hideSearchBox() {
		imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
		searchBar.setVisibility(View.INVISIBLE);
		matchCount = 0;
		// remove the match by an impossible search
		serverWebs.get(webIndex).findAll("jingtao10175jtbuaa@gmail.com");
		searchHint.setText("");
	}

	void findMatchCount() {
		toSearch = etSearch.getText().toString();
		matchCount = serverWebs.get(webIndex).findAll(toSearch);
		if (matchCount > 0) {
			try {
				Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
				m.invoke(serverWebs.get(webIndex), true);
			} catch (Throwable ignored) {
			}

			matchIndex = matchCount;
			while (matchIndex > 0) {
				serverWebs.get(webIndex).findNext(false);
				// move to select the first match
				matchIndex -= 1;
			}
		}
	}

	void actionBack() {
		if (browserView.getVisibility() == View.GONE) hideCustomView();// playing video. need wait it over?
		else if (menuOpened) hideMenu();
		else if (bookmarkOpened) hideBookmark();
		else if (webControl.getVisibility() == View.VISIBLE)
			webControl.setVisibility(View.INVISIBLE);// hide web control
		else if ((searchBar != null) && searchBar.getVisibility() == View.VISIBLE)
			hideSearchBox();
		else if (HOME_BLANK.equals(webAddress.getText().toString())) {
			// hide browser when click back key on homepage.
			// this is a singleTask activity, so if return
			// super.onKeyDown(keyCode, event), app will exit.
			// when use click browser icon again, it will call onCreate,
			// user's page will not reopen.
			// singleInstance will work here, but it will cause
			// downloadControl not work? or select file not work?
			if (serverWebs.size() == 1)
				moveTaskToBack(true);
			else closePage(webIndex, false); // close blank page if more than one page
		} 
		else if (serverWebs.get(webIndex).canGoBack())
			serverWebs.get(webIndex).goBack();
		else
			closePage(webIndex, false);// close current page if can't go back		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getRepeatCount() == 0) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				actionBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	void readTextSize(SharedPreferences sp) {
		int iTextSize = sp.getInt("textsize", 2);
		switch (iTextSize) {
		case 1:
			textSize = TextSize.LARGER;
			break;
		case 2:
			textSize = TextSize.NORMAL;
			break;
		case 3:
			textSize = TextSize.SMALLER;
			break;
		case 4:
			textSize = TextSize.SMALLEST;
			break;
		case 5:
			textSize = TextSize.LARGEST;
			break;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// No call for super(). Bug on API Level > 11. refer to
		// http://stackoverflow.com/questions/7469082
	}

	@Override
	protected void onPause() {
		if (historyChanged) {
			WriteTask wtask = new WriteTask();
			wtask.execute("history");
		}
		if (bookmarkChanged) {
			WriteTask wtask = new WriteTask();
			wtask.execute("bookmark");
		}
		if (downloadsChanged) {
			WriteTask wtask = new WriteTask();
			wtask.execute("downloads");
		}

		if (browserView.getVisibility() == View.GONE) {
			if (mVideoView == null) hideCustomView();//hide VideoSurfaceView otherwise it will force close onResume
			else mVideoView.pause();
		}
			
		sEdit.putBoolean("show_zoom", serverWebs.get(webIndex).zoomVisible);
		sEdit.putBoolean("html5", serverWebs.get(webIndex).html5);
		sEdit.commit();

		try {
			if (baiduPause != null) baiduPause.invoke(this, this);
		} catch (Exception e) {}

		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (browserView.getVisibility() == View.GONE) 
			if (mVideoView != null) mVideoView.start(); 
		
		if (interstitialAd != null && !interstitialAd.isReady()) interstitialAd.loadAd();

		try {if (baiduResume != null) baiduResume.invoke(this, this);} catch (Exception e) {}
	}

	@Override
	public File getCacheDir() {
		// NOTE: this method is used in Android 2.1
		return getApplicationContext().getCacheDir();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		setLayout();
	}

	void setLayout() {
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
        bookmarkWidth = dm.widthPixels * 3 / 4;
        int minWidth = (int) (320 * dm.density);
        if (bookmarkWidth > minWidth) bookmarkWidth = minWidth;
        
		int height = (int) (dm.heightPixels / dm.density);
		height -= urlLine.getHeight();
		height -= webTools.getHeight();
		int size = height / 72;// 72 is the height of each menu item
		if (size > 10) {
			menuWidth = (int) (80*dm.density);// 80 dip for single column
			menuGrid.setNumColumns(1);
		}
		else {
			menuWidth = (int) (120*dm.density);// 120 dip for 2 column
			menuGrid.setNumColumns(2);
		}
		
		updateHistoryViewHeight();
		if (bookmarkOpened) {
			bookmarkDownloads.getLayoutParams().width = bookmarkWidth;
			bookmarkDownloads.requestLayout();
		}
		
		if (menuOpened) {
			menuGrid.getLayoutParams().width = menuWidth;
			menuGrid.requestLayout();
		}
		
		if ((webControl.getVisibility() == View.VISIBLE) && (webControl.getWidth() < minWebControlWidth)) scrollToMain();
		
		if (dm.widthPixels < 320*dm.density) {
			imageBtnList.getLayoutParams().width = dm.widthPixels;
			adContainer2.setVisibility(View.GONE);
		}
		else if (dm.widthPixels <= 640*dm.density) {
			imageBtnList.getLayoutParams().width = (int) (320 * dm.density);
			adContainer2.setVisibility(View.GONE);			
		}
		else {
			imageBtnList.getLayoutParams().width = (int) (320 * dm.density);
			if (adview2 != null) adview2.loadAd();
			adContainer2.setVisibility(View.VISIBLE);
		}
	}

	void createAd() {
		if (adview != null) return;// only create ad for one time.
		
		if (mAdAvailable) {
			adview = new WrapAdView(this, 0, "a1502880ce4208b", null);// AdSize.BANNER require 320*50
			if ((adview != null) && (adview.getInstance() != null)) {
				adContainer.addView(adview.getInstance());
				adview.loadAd();
			}
			
			adview2 = new WrapAdView(this, 0, "a1517e34883f8ce", null);// AdSize.BANNER require 320*50
			if ((adview2 != null) && (adview2.getInstance() != null)) {
				adContainer2.addView(adview2.getInstance());
				adview2.loadAd();
			}
			
			interstitialAd = new WrapInterstitialAd(this, "a14be3f4ec2bb11", mAppHandler);
		}
	}

	class AppHandler extends Handler {

		public void handleMessage(Message msg) {
			if (msg.what == -2) {
				Bundle data = msg.getData();
				String errorMsg = data.getString("msg");
				//if (errorMsg != null) Toast.makeText(mContext, "Can't load AdMob, " + errorMsg, Toast.LENGTH_LONG).show();
			}
			else if (msg.what == -3) {
				interstitialAd.loadAd();
			}
		}
	}

	void updateDownloads() {
		if (downloadsAdapter != null) {
			downloadsAdapter.notifyDataSetChanged();
		}
		downloadsChanged = true;
	}
	
	void updateBookmark() {
		if (bookmarkAdapter != null) {
			bookmarkAdapter.notifyDataSetChanged();
			updateHistoryViewHeight();
		}
		bookmarkChanged = true;
	}
	
	void updateHistory() {
		if (historyAdapter != null) {
			updateHistoryViewHeight();
			
			mHistoryForAdapter.clear();
			for (int i = mHistory.size()-1; i >= 0; i--)
				mHistoryForAdapter.add(mHistory.get(i));
			historyAdapter.notifyDataSetChanged();
		}
		historyChanged = true;
	}
	
	void updateHistoryViewHeight() {
		if (historyList == null) return;
		//calculate height of history list so that it display not too many or too few items
		getTitleHeight();
		int height = dm.heightPixels - statusBarHeight - adContainer.getHeight();//browserView.getHeight() may not correct when rotate. so use this way. but not applicable for 4.x platform
		
		LayoutParams lp = urlLine.getLayoutParams();// urlLine.getHeight() may not correct here, so use lp
		if (lp.height != 0) height -= urlHeight * dm.density;
		lp = webTools.getLayoutParams();
		if (lp.height != 0) height -= barHeight * dm.density;
		
		int maxSize = (int) Math.max(height/2, height-mBookMark.size()*42*dm.density);// 42 is the height of each history with divider. should display equal rows of history and bookmark
		height = (int) (Math.min(maxSize, mHistory.size()*43*dm.density));//select a value from maxSize and mHistory.size().

		lp = historyList.getLayoutParams();
		lp.height = height;
		historyList.requestLayout();
	}
	
	void loadPage() {// load home page
		serverWebs.get(webIndex).getSettings().setJavaScriptEnabled(true);
		
		WebBackForwardList wbfl = serverWebs.get(webIndex).copyBackForwardList();
		if (wbfl != null) {
			int size = wbfl.getSize();
			int current = wbfl.getCurrentIndex();
			for (int i = 0; i < size; i++) {
				if (HOME_PAGE.equals(wbfl.getItemAtIndex(i).getUrl())) {
					serverWebs.get(webIndex).goBackOrForward(i - current);
					return;
				}
			}
		}
		
		if ((mBookMark.size() > 0) || (mHistory.size() > 0)) showBookmark();// show bookmark for load home page too slow
		serverWebs.get(webIndex).loadUrl(HOME_PAGE);
	}
}