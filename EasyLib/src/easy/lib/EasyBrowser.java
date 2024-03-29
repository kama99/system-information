package easy.lib;

import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;

import base.lib.WrapAdView;
import base.lib.WrapInterstitialAd;
import base.lib.util;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import common.lib.EasyWebView;
import common.lib.SimpleBrowser;

public class EasyBrowser extends SimpleBrowser {
	public boolean collapse1 = false;// default open top list and bookmark
	public boolean collapse2 = false;
	public boolean collapse3 = true;
	int countDown = 0;
	
	public ListView downloadsList;

	ImageView imgHome;

	void initImgHome() {
		imgHome = (ImageView) findViewById(R.id.home);
		imgHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if ((m_homepage != null) && (!"".equals(m_homepage))) serverWebs.get(webIndex).loadUrl(m_homepage);
				else if (!HOME_PAGE.equals(serverWebs.get(webIndex).m_url)) loadPage();
			}
		});
		imgHome.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				globalSetting();
				return true;
			}
		});
	}

	void initImgGo() {
		imgGo = (ImageView) findViewById(R.id.go);
		imgGo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				gotoUrl(webAddress.getText().toString().toLowerCase());
			}
		});
		imgGo.setOnLongClickListener(new OnLongClickListener() {// long click to select search engine
			@Override
			public boolean onLongClick(View arg0) {
				CharSequence engine[] = new CharSequence[] {getString(R.string.bing), getString(R.string.baidu), getString(R.string.google), getString(R.string.yandex), getString(R.string.duckduckgo)};
				selectEngine(engine);
				return true;
			}
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		browserName = getString(R.string.browser_name);
		
		setAsDefaultApp();

		setContentView(R.layout.browser);
        
		initViews();
		initImgHome();
		initImgGo();
		
		initFirstPage(new EasyWebView(mContext, this));
	}
	
	public void setDefault(PackageManager pm, Intent intent, IntentFilter filter) {
		List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
		int size = resolveInfoList.size();
		ComponentName[] arrayOfComponentName = new ComponentName[size];
		boolean seted = false;
		for (int i = 0; i < size; i++) {
			ActivityInfo activityInfo = resolveInfoList.get(i).activityInfo;
			String packageName = activityInfo.packageName;
			String className = activityInfo.name;
			//clear default browser
			if (packageName.equals(mContext.getPackageName())) {
				seted = true;
				break;
			}
			try{pm.clearPackagePreferredActivities(packageName);} catch(Exception e) {}
			ComponentName componentName = new ComponentName(packageName, className);
			arrayOfComponentName[i] = componentName;
		}
		
		if (!seted) {
			ComponentName component = new ComponentName(mContext.getPackageName(), "easy.lib.SimpleBrowser");
			pm.addPreferredActivity(filter,	IntentFilter.MATCH_CATEGORY_SCHEME, arrayOfComponentName, component);
		}
	}

	public void setAsDefaultApp() {
		PackageManager pm = getPackageManager();
		
		try {pm.addPackageToPreferred(getPackageName());}// for 1.5 platform 
		catch(Exception e) {
			// set default browser for 1.6-2.1 platform. not work for 2.2 and up platform
			Intent intent = new Intent("android.intent.action.VIEW");
			intent.addCategory("android.intent.category.BROWSABLE");
			intent.addCategory("android.intent.category.DEFAULT");
			
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.intent.action.VIEW");
			filter.addCategory("android.intent.category.BROWSABLE");
			filter.addCategory("android.intent.category.DEFAULT");
			filter.addDataScheme("http");
			
			Uri uri = Uri.parse("http://");
			intent.setDataAndType(uri, null);
			setDefault(pm, intent, filter);			
		} 
	}
	
	public void readPreference() {
		super.readPreference();
		
		collapse1 = sp.getBoolean("collapse1", false);
		collapse2 = sp.getBoolean("collapse2", true);
		collapse3 = sp.getBoolean("collapse3", true);

		if (!mAdAvailable) m_homepage = sp.getString("homepage", null);
	}
	
	AlertDialog menuDialog = null;// menu Dialog
	public void menuOpenAction() {
		if (menuDialog == null) initMenuDialog();
		
		if (menuDialog.isShowing()) menuDialog.dismiss();
		else {
			setUrlHeight(true);
			setBarHeight(true);
			adContainer.setVisibility(View.VISIBLE);
			
			menuDialog.show();
		}
	}
	
	View menuView;
	AlertDialog downloadsDialog = null;
	public void createAd(float width) {
		if (mAdAvailable) {
			removeAd();
			if (width < 320) ;//do nothing for it is too narrow. 
            // but it will cause force close if not create adview?
            if (width < 468)// AdSize.BANNER require 320*50
            	adview = new WrapAdView(this, 0, "a14f3f6bc126143", mAppHandler);
            else if (width < 728)
                 adview = new WrapAdView(this, 1, "a14f3f6bc126143", mAppHandler);
                 // AdSize.IAB_BANNER require 468*60 but return 702*90 on BKB(1024*600) and S1.
                 // return width = request width * density.
            else    // AdSize.IAB_LEADERBOARD require 728*90, return 1092*135 on BKB
                 adview = new WrapAdView(this, 2, "a14f3f6bc126143", mAppHandler);
 			if ((adview != null) && (adview.getInstance() != null)) {
				adContainer.addView(adview.getInstance());
				adview.loadAd();
			}

 			if (adview2 == null) {
 				adview2 = new WrapAdView(this, 0, "a14d662bba1e443", null);// AdSize.BANNER require 320*50
 				if ((adview2 != null) && (adview2.getInstance() != null)) {
 					adContainer2.addView(adview2.getInstance());
 					adview2.loadAd();
 				}
 			}

			if (interstitialAd == null) interstitialAd = new WrapInterstitialAd(this, "a14be3f4ec2bb11", mAppHandler);
		}
	}

	public void actionBack() {
		// press Back key in webview will go backword.
		if (webControl.getVisibility() == View.VISIBLE)
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
		serverWebs.get(webIndex).loadUrl("javascript:inject(\"2::::" + getBookmark("....") + "\");");// call javascript to inject bookmark
	}
	
	public void updateHistory() {
		serverWebs.get(webIndex).loadUrl("javascript:inject(\"3::::" + getHistory("....") + "\");");// call javascript to inject bookmark
	}
	
	public void updateHomePage() {
		serverWebs.get(webIndex).loadUrl("javascript:setTitle(\"" + getString(R.string.browser_name) + "\");");
		// top bar
		String tmp = getString(R.string.top);
		if (mAdAvailable) {
			if (countDown > 0) tmp += getString(R.string.url_can_longclick);
			serverWebs.get(webIndex).loadUrl("javascript:setTitleBar(\"1," + collapse1 + "," + tmp + "\");");
			serverWebs.get(webIndex).loadUrl("javascript:collapse(\"1," + !collapse1 + "\");");
			serverWebs.get(webIndex).loadUrl("javascript:inject(\"1::::" + getTopList("....") + "\");");// call javascript to inject toplist
		}
		else
			serverWebs.get(webIndex).loadUrl("javascript:hideTop();");// hide toplist for pro version
		
		// bookmark bar
		tmp = getString(R.string.bookmark);
		if (countDown > 0) tmp += getString(R.string.pic_can_longclick);
		serverWebs.get(webIndex).loadUrl("javascript:setTitleBar(\"2," + collapse2 + "," + tmp + "\");");

		// history bar
		tmp = getString(R.string.history);
		if (countDown > 0) tmp += getString(R.string.text_can_longclick);
		serverWebs.get(webIndex).loadUrl("javascript:setTitleBar(\"3," + collapse3 + "," + tmp + "\");");

		serverWebs.get(webIndex).loadUrl("javascript:collapse(\"2," + !collapse2 + "\");");
		serverWebs.get(webIndex).loadUrl("javascript:collapse(\"3," + !collapse3 + "\");");
		
		updateBookmark();
		updateHistory();

		serverWebs.get(webIndex).loadUrl("javascript:setButton(\"" + getString(R.string.edit_home) + "," + getString(R.string.delete) + "," + getString(R.string.cancel) + "\");");
		
		if (countDown > 0) countDown -= 1;
	}
	
	String getTopList(String splitter) {
		String fileDir = "<li style='background-image:url(file://" + getFilesDir().getAbsolutePath() + "/";
		
		StringBuilder sb = new StringBuilder("");	
		if (Locale.CHINA.equals(mLocale) || Locale.TAIWAN.equals(mLocale)) {
			sb.append(fileDir);
			sb.append("easybrowser.shupeng.com.png)'><a href='http://easybrowser.shupeng.com'>书朋小说网</a></li>");
			//sb.append("tiantian.m.the9.com.png)'><a href='http://tiantian.m.the9.com'>热门游戏</a></li>");
			sb.append(splitter);
			sb.append(fileDir);
			sb.append("weibo.com.png)'><a href='http://weibo.com'>新浪微博</a></li>");
			sb.append(splitter);
			// sb.append(fileDir);
			// sb.append("www.taobao.com.png)'><a href='http://www.taobao.com'>淘宝</a></li>");
			sb.append(fileDir);
			sb.append("wap.easou.com.png)'><a href='http://i5.easou.com/kw.m?tp=7&p=1&cid=bip1065_10713_001&esid=2GsaHSnARzA&si=a91fc744144b561694707ad18923b4f9&wver=t'>宜搜</a></li>");
			sb.append(splitter);
			//sb.append("<li><a href='http://www.9yu.co/index.html?c=2'>美图</a></li>");// no favicon
			// sb.append(fileDir);
			// sb.append("bpc.borqs.com.png)'><a href='http://bpc.borqs.com'>梧桐</a></li>");
		} else {
			// sb.append("<li><a href='http://www.1mobile.com/app/market/?cid=9'>1mobile</a></li>");// no favicon
			//if (mAdAvailable) sb.append("<li style='background-image:url(file:///android_asset/favicon.ico)'><a href='http://bpc.borqs.com/market.html?id=easy.browser.pro'>Ad free version of Easy Browser</a></li>"); // suspended
			
			//sb.append(fileDir);
			//sb.append("m.admob.com.png)'><a style='text-decoration: underline; color:#0000ff' onclick='javascript:window.JSinterface.showInterstitialAd()'>AdMob</a></li>");
			//sb.append(splitter);
			
			sb.append(fileDir);
			sb.append("helpx.adobe.com.png)'><a href='http://tinyurl.com/4aflash'>Adobe Flash player install/update</a></li>");
			sb.append(splitter);
			
			//sb.append("<li style='background-image:url(file:///android_asset/favicon.ico)'><a href='market://details?id=easy.browser.com'>Easy Browser Pro, no Ads</a></li>");
			//sb.append(splitter);
			
			//sb.append(fileDir);
			//sb.append("duckduckgo.com.png)'><a href='https://duckduckgo.com?t=easybrowser&q=DuckDuckGo'>DuckDuckGo</a></li>");
			//sb.append(splitter);
			
			sb.append(fileDir);
			sb.append("m.facebook.com.png)'><a href='http://www.facebook.com'>Facebook</a></li>");
			sb.append(splitter);
			
			sb.append(fileDir);
			sb.append("www.moborobo.com.png)'><a href='http://www.moborobo.com/app/mobomarket.html'>MoboMarket</a></li>");
			sb.append(splitter);
			
			//sb.append("<li><a href='file:///sdcard/'>SDcard</a></li>");
			//sb.append(splitter);
			
			sb.append(fileDir);
			sb.append("mobile.twitter.com.png)'><a href='http://twitter.com'>Twitter</a></li>");
			sb.append(splitter);
			
			sb.append(fileDir);
			sb.append("www.wikipedia.org.png)'><a href='http://www.wikipedia.org/'>WikipediA</a></li>");
			sb.append(splitter);

			// additional top list for some locale
			if (Locale.JAPAN.equals(mLocale) || Locale.JAPANESE.equals(mLocale)) {
				sb.append(fileDir);
				sb.append("m.yahoo.co.jp.png)'><a href='http://www.yahoo.co.jp'>Yahoo!JAPAN</a></li>");
				sb.append(splitter);
			} else if ("ru_RU".equals(mLocale.toString())) {
				sb.append(fileDir);
				sb.append("www.yandex.ru.png)'><a href='http://www.yandex.ru/?clid=1911433'>Яндекс</a></li>");
				sb.append(splitter);
			}
		}
		
		return sb.toString();
	}
	
	String getBookmark(String splitter) {
		String fileDir = "<li style='background-image:url(file://" + getFilesDir().getAbsolutePath() + "/";
		
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < mBookMark.size(); i++) {
			sb.append(fileDir);
			sb.append(mBookMark.get(i).m_site);
			sb.append(".png)'><input class='bookmark' type='checkbox' style='display:none; margin-right:20px'><a href='");
			sb.append(mBookMark.get(i).m_url);
			sb.append("'>");
			sb.append(mBookMark.get(i).m_title);
			sb.append("</a></li>");
			sb.append(splitter);
		}
		
		return sb.toString();
	}
	
	String getHistory(String splitter) {
		String fileDir = "<li style='background-image:url(file://" + getFilesDir().getAbsolutePath() + "/";
		
		StringBuilder sb = new StringBuilder("");			
		for (int i = mHistory.size() - 1; i >= 0; i--) {
			sb.append(fileDir);
			sb.append(mHistory.get(i).m_site);
			sb.append(".png)'><input class='history' type='checkbox' style='display:none; margin-right:20px'><a href='");
			sb.append(mHistory.get(i).m_url);
			sb.append("'>");
			sb.append(mHistory.get(i).m_title);
			sb.append("</a></li>");
			sb.append(splitter);
		}
		
		return sb.toString();
	}
	
	public void pauseAction() {
		sEdit.putBoolean("collapse1", collapse1);
		sEdit.putBoolean("collapse2", collapse2);
		sEdit.putBoolean("collapse3", collapse3);

		super.pauseAction();
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
		int[] menu_image_array = { R.drawable.html_w, R.drawable.capture,
				R.drawable.copy, R.drawable.exit, R.drawable.downloads,
				R.drawable.share, R.drawable.search, R.drawable.about };
		// menu text
		String[] menu_name_array = { getString(R.string.source),
				getString(R.string.snap), getString(R.string.copy),
				getString(R.string.exit), getString(R.string.downloads),
				getString(R.string.shareurl), getString(R.string.search),
				getString(R.string.settings) };

		// create AlertDialog
		menuView = View.inflate(mContext, R.layout.grid_menu, null);
		menuDialog = new AlertDialog.Builder(mContext).create();
		menuDialog.setView(menuView);
		WindowManager.LayoutParams params = menuDialog.getWindow()
				.getAttributes();
		// 240 for 1024h, 140 for 800h, 70 for 480h, to show menu dialog in
		// correct position
		if (dm.heightPixels <= 480)
			params.y = 70;
		else if (dm.heightPixels <= 800)
			params.y = 140;
		else
			params.y = 240;
		menuDialog.getWindow().setAttributes(params);

		menuDialog.setCanceledOnTouchOutside(true);

		menuDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_MENU)
					dialog.dismiss();
				return false;
			}

		});

		menuGrid = (GridView) menuView.findViewById(R.id.gridview);
		menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
		menuGrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:// view page source
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
				case 1:// view snap
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
				case 2:// copy
					webControl.setVisibility(View.GONE);// hide webControl when copy
					try {
						if (Integer.decode(android.os.Build.VERSION.SDK) > 10)
							Toast.makeText(mContext,
									getString(R.string.copy_hint),
									Toast.LENGTH_LONG).show();
					} catch (Exception e) {
					}

					try {
						KeyEvent shiftPressEvent = new KeyEvent(0, 0,
								KeyEvent.ACTION_DOWN,
								KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
						shiftPressEvent.dispatch(serverWebs.get(webIndex));
					} catch (Exception e) {
					}
					break;
				case 3:// exit
					clearFile("pages");
					ClearCache(); // clear cache when exit
					if (interstitialAd != null && interstitialAd.isReady()) interstitialAd.show();
					finish();
					break;
				case 4:// downloads
					Intent intent = new Intent(
							"com.estrongs.action.PICK_DIRECTORY");
					intent.setData(Uri.parse("file:///sdcard/simpleHome/"));
					if (!util.startActivity(intent, false, mContext)) {
						if (downloadsDialog == null)
							downloadsDialog = new AlertDialog.Builder(
									mContext)
									.setMessage(
											getString(R.string.downloads_to)
													+ appstate.downloadPath
													+ getString(R.string.downloads_open))
									.setPositiveButton(
											R.string.ok,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													Intent intent = new Intent(
															Intent.ACTION_VIEW,
															Uri.parse("market://details?id=com.estrongs.android.pop"));
													util.startActivity(intent,
															true,
															getBaseContext());
												}
											})
									.setNegativeButton(
											R.string.cancel,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
												}
											}).create();
						downloadsDialog.show();
					}
					break;
				case 5:// share url
					shareUrl(serverWebs.get(webIndex).getTitle(), serverWebs.get(webIndex).m_url);
					break;
				case 6:// search
					webControl.setVisibility(View.GONE);// hide webControl when search
						// serverWebs.get(webIndex).showFindDialog("e", false);
					if (searchBar == null) initSearchBar();
					searchBar.bringToFront();
					searchBar.setVisibility(View.VISIBLE);
					etSearch.requestFocus();
					toSearch = "";
					imm.toggleSoftInput(0, 0);
					break;
				case 7:// settings
					intent = new Intent("about");
					intent.setClassName(getPackageName(), AboutBrowser.class.getName());
					startActivityForResult(intent, SETTING_RESULTCODE);
					break;
				}
				menuDialog.dismiss();
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
				case 7:// add short cut
					createShortcut(url, serverWebs.get(item.getOrder()).getTitle());
					Toast.makeText(mContext, getString(R.string.add_shortcut) + " " + serverWebs.get(webIndex).getTitle(), Toast.LENGTH_LONG).show();
					break;
				case 8:// remove bookmark
					removeFavo(item.getOrder());
					break;
				case 9:// remove history
					removeHistory(item.getOrder());
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
				case 11://set homepage
					m_homepage = url;
					sEdit.putString("homepage", url);
					sEdit.commit();
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

			if (HOME_PAGE.equals(serverWebs.get(webIndex).getUrl())) {// only operate bookmark/history in home page
				boolean foundBookmark = false;
				for (int i = mBookMark.size() - 1; i >= 0; i--)
					if ((mBookMark.get(i).m_url.equals(url))
							|| (url.equals(mBookMark.get(i).m_url + "/"))) {
						foundBookmark = true;
						if (!mAdAvailable) {
							menu.add(0, 7, i, R.string.add_shortcut).setOnMenuItemClickListener(handler);// only work in pro version
							menu.add(0, 11, i, R.string.set_homepage).setOnMenuItemClickListener(handler);// only work in pro version
						}
						menu.add(0, 8, i, R.string.remove_bookmark).setOnMenuItemClickListener(handler);
						break;
					}
				if (!foundBookmark)
					menu.add(0, 10, 0, R.string.add_bookmark).setOnMenuItemClickListener(handler);

				for (int i = mHistory.size() - 1; i >= 0; i--)
					if ((mHistory.get(i).m_url.equals(url))
							|| (url.equals(mHistory.get(i).m_url + "/"))) {
						menu.add(0, 9, i, R.string.remove_history)
								.setOnMenuItemClickListener(handler);
						break;
					}
			}

			menu.add(0, 0, 0, R.string.save).setOnMenuItemClickListener(handler);
		}
	}
}
