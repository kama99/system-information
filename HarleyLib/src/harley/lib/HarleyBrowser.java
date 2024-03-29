package harley.lib;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Locale;

import base.lib.WebUtil;
import base.lib.WrapAdView;
import base.lib.WrapInterstitialAd;
import base.lib.util;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import common.lib.EasyWebView;
import common.lib.MyListAdapter;
import common.lib.SimpleBrowser;
import common.lib.TitleUrl;
import easy.lib.R;

public class HarleyBrowser extends SimpleBrowser {
	public int bookmarkWidth = LayoutParams.WRAP_CONTENT;
	public int menuWidth = LayoutParams.WRAP_CONTENT;
	
	public FrameLayout adContainer3;	
	public WrapAdView adview3 = null;

	// browser related
	public RelativeLayout browserView;
	boolean shouldGo = false;
	
	ImageView imgMenu;
	public boolean menuOpened = true;
	public boolean bookmarkOpened = true;
	public View bookmarkDownloads;
	public LinearLayout bookmarkView;
	public ListView downloadsList;
	public MyListAdapter bookmarkAdapter, historyAdapter;
	public int minWebControlWidth = 200;
	public ImageView imgBookmark;
	int statusBarHeight;

	public LinearLayout fakeWebControl;
	ArrayList<TitleUrl> mHistoryForAdapter = new ArrayList<TitleUrl>();// the revert for mHistory.
	ListView historyList;
	public boolean reverted = false;

	public void initWebControl() {
		super.initWebControl();
		
		fakeWebControl = (LinearLayout) findViewById(R.id.fakeWebcontrol);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		browserName = getString(R.string.browser_name);
        LayoutInflater inflater = LayoutInflater.from(this);
        
        browserView = (RelativeLayout) inflater.inflate(R.layout.browser, null);
		setContentView(browserView);
        bookmarkDownloads = findViewById(R.id.bookmarkDownloads);
        bookmarkView = (LinearLayout) findViewById(R.id.bookmarkView);
        menuGrid = (GridView) findViewById(R.id.grid_menu);
		adContainer3 = (FrameLayout) findViewById(R.id.adContainer3);

		initViews();
		hideMenu();
		hideBookmark();
		
		initFirstPage(new EasyWebView(mContext, this));
	}
	
	public void initViews() {
		super.initViews();
		
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
				globalSetting();
				return true;
			}
		});
	}
	
	public void hideMenu() {
		menuGrid.getLayoutParams().width = 0;
		menuGrid.requestLayout();
		menuOpened = false;
	}

	public void hideBookmark() {
		bookmarkDownloads.getLayoutParams().width = 0;
		bookmarkDownloads.requestLayout();
		bookmarkView.setVisibility(View.VISIBLE);
		if (downloadsList != null) downloadsList.setVisibility(View.GONE);
		bookmarkOpened = false;
	}

	public void exchangePosition() {
		super.exchangePosition();
		
		//change position of bookmark and menu
		LayoutParams lp1 = bookmarkDownloads.getLayoutParams();
		LayoutParams lp2 = menuGrid.getLayoutParams();
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

		if (revertCount % 2 == 0) reverted = false;
		else reverted = true;
		
		// revert add bookmark and refresh button
		lp1 = imgAddFavo.getLayoutParams();
		lp2 = imgRefresh.getLayoutParams();
		imgAddFavo.setLayoutParams(lp2);
		imgRefresh.setLayoutParams(lp1);
	}
	
	@Override
	protected void onPause() {
		if (browserView.getVisibility() == View.GONE) {
			if (mVideoView == null) hideCustomView();//hide VideoSurfaceView otherwise it will force close onResume
			else mVideoView.pause();
		}

		super.onPause();
	}
	
	public WebChromeClient.CustomViewCallback mCustomViewCallback = null;
	public FrameLayout mCustomViewContainer = null;
	public VideoView mVideoView = null;
	public void hideCustomView() {
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

	@Override
	public void onResume() {
		super.onResume();
		
		if (browserView.getVisibility() == View.GONE) 
			if (mVideoView != null) mVideoView.start();
	}
	
	public void setLayout() {
		super.setLayout();
		
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
	}
	
	public void readPreference() {
		super.readPreference();
		
		if (Locale.CHINA.equals(mLocale)) 
			HOME_PAGE = "file:///android_asset/home-ch.html";
		
		m_homepage = sp.getString("homepage", null);
	}
	
	public void loadPage() {// load home page
		super.loadPage();
		if ((mBookMark.size() > 0) || (mHistory.size() > 0)) showBookmark();// show bookmark for load home page too slow
	}

	public void shareUrl(String title, String url) {
		super.shareUrl(title, url);

		if (shareMode != 1) scrollToMain();
	}
	
	public void menuOpenAction() {
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
	}
	
	public void setUrlHeight(boolean showUrlNow) {
		super.setUrlHeight(showUrlNow);
		
		updateHistoryViewHeight();
	}
	
	public void setBarHeight(boolean showBarNow) {
		super.setBarHeight(showBarNow);
		
		updateHistoryViewHeight();
	}
	
	

	public void showBookmark() {
		bookmarkDownloads.getLayoutParams().width = bookmarkWidth;
		bookmarkDownloads.requestLayout();
		bookmarkOpened = true;
		if (dm.widthPixels-menuWidth-bookmarkWidth < minWebControlWidth) {
			webControl.setVisibility(View.GONE);
			hideMenu();
		}
		if (bookmarkAdapter == null) initBookmarks();
	}
	
	public void scrollToMain() {
		if (bookmarkOpened) hideBookmark();		
		if (menuOpened) hideMenu(); 
	}
	
	public void initBookmarks() {
		bookmarkAdapter = new MyListAdapter(this, mBookMark);
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

		historyAdapter = new MyListAdapter(this, mHistoryForAdapter);
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
	
	public void createAd(float width) {
		if (mAdAvailable) {
			removeAd();
			if (width < 320) ;//do nothing for it is too narrow. 
            // but it will cause force close if not create adview?
            if (width < 468)// AdSize.BANNER require 320*50
            	adview = new WrapAdView(this, 0, "a1502880ce4208b", mAppHandler);
            else if (width < 728)
            	adview = new WrapAdView(this, 1, "a1502880ce4208b", mAppHandler);
                 // AdSize.IAB_BANNER require 468*60 but return 702*90 on BKB(1024*600) and S1.
                 // return width = request width * density.
            else    // AdSize.IAB_LEADERBOARD require 728*90, return 1092*135 on BKB
            	adview = new WrapAdView(this, 2, "a1502880ce4208b", mAppHandler);
 			if ((adview != null) && (adview.getInstance() != null)) {
 				adContainer.addView(adview.getInstance());
 				adview.loadAd();
			}

 			if (adview2 == null) {
 				adview2 = new WrapAdView(this, 0, "a1517e34883f8ce", null);// AdSize.BANNER require 320*50
 				if ((adview2 != null) && (adview2.getInstance() != null)) {
 					adContainer2.addView(adview2.getInstance());
 					adview2.loadAd();
 				}
 			}

 			if ((adview3 == null) && (adContainer3 != null)) {
 				adview3 = new WrapAdView(this, 0, "a14a8e65a47d51f", null);// AdSize.BANNER require 320*50
 				if ((adview3 != null) && (adview3.getInstance() != null)) {
 					adContainer3.addView(adview3.getInstance());
 					adview3.loadAd();
 				}
 			}
 			
			if (interstitialAd == null) interstitialAd = new WrapInterstitialAd(this, "a14be3f4ec2bb11", mAppHandler);
		}
	}

	public void actionBack() {
		if (browserView.getVisibility() == View.GONE) hideCustomView();// playing video. need wait it over?
		else if (menuOpened) hideMenu();
		else if (bookmarkOpened) hideBookmark();
		else if (webControl.getVisibility() == View.VISIBLE)
			webControl.setVisibility(View.GONE);// hide web control
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
			if (serverWebs.size() == 1) {
				if (interstitialAd != null && interstitialAd.isReady()) interstitialAd.show();
				moveTaskToBack(true);
			}
			else closePage(webIndex, false); // close blank page if more than one page
		} else if (serverWebs.get(webIndex).canGoBack())
			serverWebs.get(webIndex).goBack();
		else
			closePage(webIndex, false);// close current page if can't go back
	}
	
	public void updateBookmark() {
		if (bookmarkAdapter != null) {
			bookmarkAdapter.notifyDataSetChanged();
			updateHistoryViewHeight();
		}
		bookmarkChanged = true;
	}
	
	public void updateHistory() {
		if (historyAdapter != null) {
			updateHistoryViewHeight();
			
			mHistoryForAdapter.clear();
			for (int i = mHistory.size()-1; i >= 0; i--)
				mHistoryForAdapter.add(mHistory.get(i));
			historyAdapter.notifyDataSetChanged();
		}
		historyChanged = true;
	}
	
	public void updateHistoryViewHeight() {
		if (historyList == null) return;
		//calculate height of history list so that it display not too many or too few items
		getTitleHeight();
		int height = dm.heightPixels - statusBarHeight - adContainer.getHeight();//browserView.getHeight() may not correct when rotate. so use this way. but not applicable for 4.x platform
		
		LayoutParams lp = urlLine.getLayoutParams();// urlLine.getHeight() may not correct here, so use lp
		if (lp.height != 0) height -= urlHeight * dm.density;
		lp = webTools.getLayoutParams();
		if (lp.height != 0) height -= barHeight * dm.density;
		
		int maxSize = (int) Math.max(height/2, height- mBookMark.size()*42*dm.density);// 42 is the height of each history with divider. should display equal rows of history and bookmark
		height = (int) (Math.min(maxSize, mHistory.size()*43*dm.density));//select a value from maxSize and mHistory.size().

		lp = historyList.getLayoutParams();
		lp.height = height;
		historyList.requestLayout();
	}

	void getTitleHeight() {
		Rect rectgle= new Rect();
		Window window= getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		statusBarHeight = rectgle.top;
	}
	
	public void imgNewClick() {
		super.imgNewClick();
		
		if ((webControl.getVisibility() == View.VISIBLE) && (webControl.getWidth() < minWebControlWidth)) 
			scrollToMain();// otherwise may not display weblist correctly
}
	
	public boolean openNewPage(String url, int newIndex, boolean changeToNewPage, boolean closeIfCannotBack) {
		boolean weblink = super.openNewPage(url, newIndex, changeToNewPage, closeIfCannotBack);
		if (!weblink) return false;

		if (webAdapter.getCount() == 9) {// max pages is 9
			Toast.makeText(mContext, R.string.nomore_pages, Toast.LENGTH_LONG).show();
			return false; // not open new page if got max pages
		} else {
			webAdapter.insert(new EasyWebView(mContext, this), newIndex);
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

		return true;
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

		menuGrid.setFadingEdgeLength(0);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		menuGrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 9:// view page source
					try {
						if ("".equals(serverWebs.get(webIndex).pageSource)) {
							serverWebs.get(webIndex).pageSource = "Loading... Please try again later.";
							serverWebs.get(webIndex).getPageSource();
						}

						sourceOrCookie = serverWebs.get(webIndex).pageSource;
						subFolder = "source";
						showSourceDialog(browserName);
					} catch (Exception e) {
						Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
					}
					break;
				case 11:// cookie
					CookieManager cookieManager = CookieManager.getInstance(); 
					String cookie = cookieManager.getCookie(serverWebs.get(webIndex).m_url);
					if (cookie != null)
						sourceOrCookie = cookie.replaceAll("; ", "\n\n");
					else sourceOrCookie = "No cookie on this page.";
					
					subFolder = "cookie";
					showSourceDialog(browserName);
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
						
						if (snapDialog == null) initSnapDialog(getString(R.string.browser_name));
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
				case 5:// copy
					scrollToMain();
					webControl.setVisibility(View.GONE);// hide webControl when copy
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
				case 0:// exit
					clearFile("pages");
					ClearCache(); // clear cache when exit
					if (interstitialAd != null && interstitialAd.isReady()) interstitialAd.show();
					finish();
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
				case 12:// share url
					shareUrl(serverWebs.get(webIndex).getTitle(), serverWebs.get(webIndex).m_url);
					break;
				case 4:// search
					scrollToMain();
					webControl.setVisibility(View.GONE);// hide webControl when search
						// serverWebs.get(webIndex).showFindDialog("e", false);
					if (searchBar == null) initSearchBar();
					searchBar.bringToFront();
					searchBar.setVisibility(View.VISIBLE);
					etSearch.requestFocus();
					toSearch = "";
					imm.toggleSoftInput(0, 0);
					break;
				case 13:// settings
					Intent intent = new Intent(getPackageName() + "about");
					intent.setClassName(getPackageName(), AboutBrowser.class.getName());
					startActivityForResult(intent, SETTING_RESULTCODE);
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
				case 7:// save
					appstate.startDownload(serverWebs.get(webIndex).m_url, "", "no");
					break;
				case 10:// bookmark
					String url = serverWebs.get(webIndex).m_url;
					if (HOME_PAGE.equals(url)) return;// not add home page
					addRemoveFavo(url, serverWebs.get(webIndex).getTitle());
					break;
				}
			}
		});
	}
	
    public void initDownloads() {
        downloadsAdapter = new MyListAdapter(this, mDownloads);
        downloadsAdapter.type = 2;
        downloadsList = (ListView) findViewById(R.id.downloads);
        downloadsList.inflate(mContext, R.layout.web_list, null);
        downloadsList.setAdapter(downloadsAdapter);
        downloadsList.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
                        	WebUtil.openDownload(mDownloads.get(((ListView) v).getSelectedItemPosition()), mContext);
                        return false;
                }
        });
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
					appstate.startDownload(url, ext, "yes");
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
				case 8:// remove bookmark
					removeFavo(item.getOrder());
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
			menu.add(0, 4, 0, R.string.copy_url).setOnMenuItemClickListener(handler);
			menu.add(0, 5, 0, R.string.shareurl).setOnMenuItemClickListener(handler);
			menu.add(0, 6, 0, R.string.open_background).setOnMenuItemClickListener(handler);

			if (dm.heightPixels > dm.density*480) {// only show this menu item on large screen
				boolean foundBookmark = false;
				for (int i = mBookMark.size() - 1; i >= 0; i--)
					if ((mBookMark.get(i).m_url.equals(url))
							|| (url.equals(mBookMark.get(i).m_url + "/"))) {
						foundBookmark = true;
						menu.add(0, 8, i, R.string.remove_bookmark).setOnMenuItemClickListener(handler);
						break;
					}
				if (!foundBookmark)
					menu.add(0, 10, 0, R.string.add_bookmark).setOnMenuItemClickListener(handler);
			}

			menu.add(0, 0, 0, R.string.save).setOnMenuItemClickListener(handler);
		}
	}

}
