package me.seasonyuu.xposed.networkspeedindicator.h2os.widget;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import me.seasonyuu.xposed.networkspeedindicator.h2os.Common;
import me.seasonyuu.xposed.networkspeedindicator.h2os.TrafficStats;
import me.seasonyuu.xposed.networkspeedindicator.h2os.Utils;
import me.seasonyuu.xposed.networkspeedindicator.h2os.logger.Log;
import me.seasonyuu.xposed.networkspeedindicator.h2os.preference.PreferenceUtils;

@SuppressLint("HandlerLeak")
public final class TrafficView extends TextView {

	public PositionCallback mPositionCallback = null;
	public View clock = null;

	private static final String TAG = TrafficView.class.getSimpleName();
	private static final DecimalFormat formatWithDecimal = new DecimalFormat(" ##0.0");
	private static final DecimalFormat formatWithoutDecimal = new DecimalFormat(" ##0");

	private static final int DEFAULT_ICON_TINT = Color.WHITE;

	private boolean mAttached;

	private long uploadSpeed;
	private long downloadSpeed;
	private long totalTxBytes;
	private long totalRxBytes;
	private long lastUpdateTime;
	private boolean justLaunched = true;
	private boolean loggedZero = false;
	private String networkType;
	private boolean networkState;
	private boolean bootCompleted = false;

	private int iconTint = DEFAULT_ICON_TINT;

	private int prefPosition = Common.DEF_POSITION;
	private int prefForceUnit = Common.DEF_FORCE_UNIT;
	private int prefMinUnit = Common.DEF_MIN_UNIT;
	private int prefUnitMode = Common.DEF_UNIT_MODE;
	private int prefSpeedWay = Common.DEF_SPEED_WAY;
	private float prefFontSize = Common.DEF_FONT_SIZE;
	private int prefSuffix = Common.DEF_SUFFIX;
	private int prefDisplay = Common.DEF_DISPLAY;
	private boolean prefSwapSpeeds = Common.DEF_SWAP_SPEEDS;
	private int prefUpdateInterval = Common.DEF_UPDATE_INTERVAL;
	private boolean prefFontColor = Common.DEF_FONT_COLOR;
	private int prefColor = Common.DEF_COLOR;
	private int prefHideBelow = Common.DEF_HIDE_BELOW;
	private int prefMinWidth = Common.DEF_MIN_WIDTH;
	private boolean prefShowSuffix = Common.DEF_SHOW_SUFFIX;
	private Set<String> prefUnitFormat = Common.DEF_UNIT_FORMAT;
	private Set<String> prefNetworkType = Common.DEF_NETWORK_TYPE;
	private Set<String> prefNetworkSpeed = Common.DEF_NETWORK_SPEED;
	private Set<String> prefFontStyle = Common.DEF_FONT_STYLE;
	private boolean prefPreventFontAlphaChanging = Common.DEF_PREVENT_FONT_ALPHA_CHANGING;

	public TrafficView(final Context context) {
		super(context, null, 0);
		loadPreferences();
		updateConnectionInfo();
		updateViewVisibility();
		mAttached = false;
	}

	public final void refreshPosition() {
		switch (prefPosition) {
			case 0:
				mPositionCallback.setLeft();
				break;
			case 1:
				mPositionCallback.setRight();
				break;
		}
	}

	public void setIconTint(int iconTint) {
		this.iconTint = iconTint;
	}

	public int getIconTint() {
		return iconTint;
	}

	@SuppressLint("NewApi")
	public final void refreshColor() {
		if (prefFontColor) {
			setTextColor(prefColor);
		} else {
			setTextColor(iconTint);
		}
	}

	public void setAlpha(float alpha) {
		if(!prefPreventFontAlphaChanging)
			super.setAlpha(alpha);
	}

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@SuppressWarnings("unchecked")
		@Override
		public final void onReceive(final Context context, final Intent intent) {
			try {
				String action = intent.getAction();

				if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
					Log.i(TAG, "Connectivity changed");
					updateConnectionInfo();
					updateViewVisibility();
				} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
					bootCompleted = true;
					loadPreferences();
					updateViewVisibility();
					update();
				} else if (action.equals(Common.ACTION_SETTINGS_CHANGED)) {
					Log.i(TAG, "Settings changed");
					Log.d(TAG, intent.getExtras().keySet().toArray());

					if (intent.hasExtra(Common.KEY_FORCE_UNIT)) {
						prefForceUnit = intent.getIntExtra(Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
					}
					if (intent.hasExtra(Common.KEY_MIN_UNIT)) {
						prefMinUnit = intent.getIntExtra(Common.KEY_MIN_UNIT, Common.DEF_MIN_UNIT);
					}
					if (intent.hasExtra(Common.KEY_MIN_WIDTH)) {
						prefMinWidth = intent.getIntExtra(Common.KEY_MIN_WIDTH, Common.DEF_MIN_WIDTH);
						setMinimumWidth(prefMinWidth);
					}
					if (intent.hasExtra(Common.KEY_UNIT_MODE)) {
						prefUnitMode = intent.getIntExtra(Common.KEY_UNIT_MODE, Common.DEF_UNIT_MODE);
					}
					if (intent.hasExtra(Common.KEY_UNIT_FORMAT)) {
						prefUnitFormat = (Set<String>) intent.getSerializableExtra(Common.KEY_UNIT_FORMAT);
					}
					if (intent.hasExtra(Common.KEY_HIDE_BELOW)) {
						prefHideBelow = intent.getIntExtra(Common.KEY_HIDE_BELOW, Common.DEF_HIDE_BELOW);
					}
					if (intent.hasExtra(Common.KEY_SHOW_SUFFIX)) {
						prefShowSuffix = intent.getBooleanExtra(Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
					}
					if (intent.hasExtra(Common.KEY_FONT_SIZE)) {
						prefFontSize = intent.getFloatExtra(Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
					}
					if (intent.hasExtra(Common.KEY_POSITION)) {
						prefPosition = intent.getIntExtra(Common.KEY_POSITION, Common.DEF_POSITION);
						refreshPosition();
					}
					if (intent.hasExtra(Common.KEY_SUFFIX)) {
						prefSuffix = intent.getIntExtra(Common.KEY_SUFFIX, Common.DEF_SUFFIX);
					}
					if (intent.hasExtra(Common.KEY_NETWORK_TYPE)) {
						prefNetworkType = (Set<String>) intent.getSerializableExtra(Common.KEY_NETWORK_TYPE);
					}
					if (intent.hasExtra(Common.KEY_NETWORK_SPEED)) {
						prefNetworkSpeed = (Set<String>) intent.getSerializableExtra(Common.KEY_NETWORK_SPEED);
					}
					if (intent.hasExtra(Common.KEY_DISPLAY)) {
						prefDisplay = intent.getIntExtra(Common.KEY_DISPLAY, Common.DEF_DISPLAY);
					}
					if (intent.hasExtra(Common.KEY_SWAP_SPEEDS)) {
						prefSwapSpeeds = intent.getBooleanExtra(Common.KEY_SWAP_SPEEDS, Common.DEF_SWAP_SPEEDS);
					}
					if (intent.hasExtra(Common.KEY_UPDATE_INTERVAL)) {
						prefUpdateInterval = intent.getIntExtra(Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
					}
					if (intent.hasExtra(Common.KEY_FONT_COLOR)) {
						prefFontColor = intent.getBooleanExtra(Common.KEY_FONT_COLOR, Common.DEF_FONT_COLOR);
					}
					if (intent.hasExtra(Common.KEY_COLOR)) {
						prefColor = intent.getIntExtra(Common.KEY_COLOR, Common.DEF_COLOR);
					}
					if (intent.hasExtra(Common.KEY_FONT_STYLE)) {
						prefFontStyle = (Set<String>) intent.getSerializableExtra(Common.KEY_FONT_STYLE);
					}
					if (intent.hasExtra(Common.KEY_ENABLE_LOG)) {
						Log.enableLogging = intent.getBooleanExtra(Common.KEY_ENABLE_LOG, Common.DEF_ENABLE_LOG);
					}
					if (intent.hasExtra(Common.KEY_PREVENT_FONT_ALPHA_CHANGING)) {
						prefPreventFontAlphaChanging = intent.getBooleanExtra(Common.KEY_PREVENT_FONT_ALPHA_CHANGING,
								Common.DEF_PREVENT_FONT_ALPHA_CHANGING);
					}

					updateViewVisibility();
				}
			} catch (Exception e) {
				Log.e(TAG, "onReceive failed: ", e);
				Common.throwException(e);
			}
		}
	};

	@SuppressLint("HandlerLeak")
	private final Handler mTrafficHandler = new Handler() {
		@Override
		public final void handleMessage(final Message msg) {
			try {
				// changing values must be fetched together and only once
				long lastUpdateTimeNew = SystemClock.elapsedRealtime();
				int way = prefSpeedWay == 0 ? TrafficStats.DEFAULT_WAY : TrafficStats.GRAVITY_BOX_WAY;
				long[] totalBytes = TrafficStats.getTotalBytes(way);
				long totalTxBytesNew = totalBytes[1];
				long totalRxBytesNew = totalBytes[0];

				long elapsedTime = lastUpdateTimeNew - lastUpdateTime;

				if (elapsedTime == 0) {
					Log.w(TAG, "Elapsed time is zero");
					uploadSpeed = 0;
					downloadSpeed = 0;
				} else {
					uploadSpeed = ((totalTxBytesNew - totalTxBytes) * 1000) / elapsedTime;
					downloadSpeed = ((totalRxBytesNew - totalRxBytes) * 1000) / elapsedTime;
				}

				if (!loggedZero || uploadSpeed != 0 || downloadSpeed != 0) {
//					Log.d(TAG, totalTxBytes, ",", totalTxBytesNew, ";", totalRxBytes, ",", totalRxBytesNew, ";",
//							lastUpdateTime, ",", lastUpdateTimeNew, ";", uploadSpeed, ",", downloadSpeed);
					loggedZero = (uploadSpeed == 0 && downloadSpeed == 0);
				}

				totalTxBytes = totalTxBytesNew;
				totalRxBytes = totalRxBytesNew;
				lastUpdateTime = lastUpdateTimeNew;

				SpannableString spanString = new SpannableString(createText());

				if (prefFontStyle.contains("B")) {
					spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
				}
				if (prefFontStyle.contains("I")) {
					spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
				}

				setText(spanString);
				setTextSize(TypedValue.COMPLEX_UNIT_SP, prefFontSize);
				refreshColor();

				update();

				super.handleMessage(msg);
			} catch (Exception e) {
				Log.e(TAG, "handleMessage failed: ", e);
				Common.throwException(e);
			}
		}
	};

	private boolean isReady() {
		return bootCompleted || PreferenceUtils.get().isReady(getContext());
	}

	@Override
	protected final void onAttachedToWindow() {
		try {
			super.onAttachedToWindow();

			if (!mAttached) {
				mAttached = true;

				IntentFilter filter = new IntentFilter();
				filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				filter.addAction(Common.ACTION_SETTINGS_CHANGED);
				filter.addAction(Intent.ACTION_BOOT_COMPLETED);
				getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
			}
			updateViewVisibility();
		} catch (Exception e) {
			Log.e(TAG, "onAttachedToWindow failed: ", e);
			Common.throwException(e);
		}
	}

	@Override
	protected final void onDetachedFromWindow() {
		try {
			super.onDetachedFromWindow();
			if (mAttached) {
				getContext().unregisterReceiver(mIntentReceiver);
				mAttached = false;
			}
		} catch (Exception e) {
			Log.e(TAG, "onDetachedFromWindow failed: ", e);
			Common.throwException(e);
		}
	}

	private final void updateConnectionInfo() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		Log.d(TAG, networkInfo);

		if (networkInfo != null) {
			networkState = networkInfo.isAvailable();
			networkType = String.valueOf(networkInfo.getType());
			Log.i(TAG, "Network type: ", networkType);
		} else {
			networkState = false;
		}
		Log.d(TAG, "Network State: ", networkState);
	}

	private final void updateTraffic() {
		if (justLaunched) {
			// get the values for the first time
			lastUpdateTime = SystemClock.elapsedRealtime();
			int way = prefSpeedWay == 0 ? TrafficStats.DEFAULT_WAY : TrafficStats.GRAVITY_BOX_WAY;
			long[] totalBytes = TrafficStats.getTotalBytes(way);
			totalTxBytes = totalBytes[1];
			totalRxBytes = totalBytes[0];

			// don't get the values again
			justLaunched = false;

			setMinimumWidth(prefMinWidth);
		}

		mTrafficHandler.sendEmptyMessage(0);
	}

	private final String createText() {
		String uploadSuffix, downloadSuffix;
		String strUploadValue, strDownloadValue;

		switch (prefSuffix) {
			default:
			case 0:
				uploadSuffix = downloadSuffix = " ";
				break;
			case 1:
				uploadSuffix = Common.BIG_UP_TRIANGLE;
				downloadSuffix = Common.BIG_DOWN_TRIANGLE;
				break;
			case 2:
				uploadSuffix = Common.BIG_UP_HOLLOW_TRIANGLE;
				downloadSuffix = Common.BIG_DOWN_HOLLOW_TRIANGLE;
				break;
			case 3:
				uploadSuffix = Common.SMALL_UP_TRIANGLE;
				downloadSuffix = Common.SMALL_DOWN_TRIANGLE;
				break;
			case 4:
				uploadSuffix = Common.SMALL_UP_HOLLOW_TRIANGLE;
				downloadSuffix = Common.SMALL_DOWN_HOLLOW_TRIANGLE;
				break;
			case 5:
				uploadSuffix = Common.ARROW_UP;
				downloadSuffix = Common.ARROW_DOWN;
				break;
			case 6:
				uploadSuffix = Common.ARROW_UP_HOLLOW;
				downloadSuffix = Common.ARROW_DOWN_HOLLOW;
				break;
		}

		boolean showUploadSpeed = prefNetworkSpeed.contains("U");
		boolean showDownloadSpeed = prefNetworkSpeed.contains("D");
		boolean showInExactPosition = (showUploadSpeed && showDownloadSpeed);

		if (showUploadSpeed) {
			strUploadValue = formatSpeed(uploadSpeed > 0 ? uploadSpeed : 0, uploadSuffix);
		} else {
			strUploadValue = "";
		}

		if (showDownloadSpeed) {
			strDownloadValue = formatSpeed(downloadSpeed > 0 ? downloadSpeed : 0, downloadSuffix);
		} else {
			strDownloadValue = "";
		}

		String delimiter = "";
		if (prefDisplay == 0) {
			delimiter = "\n";
		} else {
			delimiter = " ";
			showInExactPosition = false; // irrelevant in one-line mode
		}

		String ret = "";
		boolean showBothSpeeds = strUploadValue.length() > 0 && strDownloadValue.length() > 0;

		if (showBothSpeeds == false && showInExactPosition == false) {
			delimiter = "";
		}

		if (prefSwapSpeeds) {
			ret = strDownloadValue + delimiter + strUploadValue;
		} else {
			ret = strUploadValue + delimiter + strDownloadValue;
		}
		return ret;
	}

	private final String formatSpeed(final long transferSpeedBytes, final String transferSuffix) {
		float unitFactor;
		long transferSpeed = transferSpeedBytes;

		switch (prefUnitMode) {
			case 0: // Binary bits
				transferSpeed *= 8;
				// no break
			case 1: // Binary bytes
				unitFactor = 1024f;
				break;
			case 2: // Decimal bits
				transferSpeed *= 8;
				// no break
			default:
			case 3: // Decimal bytes
				unitFactor = 1000f;
				break;
		}

		int tempPrefUnit = prefForceUnit;
		float megaTransferSpeed = ((float) transferSpeed) / (unitFactor * unitFactor);
		float kiloTransferSpeed = ((float) transferSpeed) / unitFactor;

		float transferValue;
		DecimalFormat transferDecimalFormat;

		if (prefForceUnit == 0) { // Auto mode

			if (megaTransferSpeed >= 1) {
				tempPrefUnit = 3;

			} else if (kiloTransferSpeed >= 1) {
				tempPrefUnit = 2;

			} else {
				tempPrefUnit = 1;
			}

			if (prefMinUnit != 0) {
				if (prefMinUnit == 1) {
					if (transferSpeed < 1) {
						transferSpeed = 0;
						tempPrefUnit = 1;
					}
				} else if (prefMinUnit == 2) {
					if (kiloTransferSpeed < 1) {
						kiloTransferSpeed = 0;
						tempPrefUnit = 2;
					}
				} else if (prefMinUnit == 3) {
					if (megaTransferSpeed < 1) {
						tempPrefUnit = 3;
					} else if (megaTransferSpeed < 0.1) {
						megaTransferSpeed = 0;
						tempPrefUnit = 3;
					}
				}
			}
		}


		switch (tempPrefUnit) {
			case 3:
				transferValue = megaTransferSpeed;
				if (transferValue < 0.1)
					transferDecimalFormat = formatWithoutDecimal;
				else
					transferDecimalFormat = formatWithDecimal;
				break;
			case 2:
				transferValue = kiloTransferSpeed;
				transferDecimalFormat = formatWithoutDecimal;
				break;
			default:
			case 1:
				transferValue = transferSpeed;
				transferDecimalFormat = formatWithoutDecimal;
				break;
		}

		String strTransferValue;

		if (transferValue < prefHideBelow) {
			strTransferValue = "";
		} else {
			strTransferValue = transferDecimalFormat.format(transferValue);
		}

		if (strTransferValue.length() > 0) {
			strTransferValue += Common.formatUnit(prefUnitMode, tempPrefUnit, prefUnitFormat);
			if (prefPosition == 0)
				strTransferValue = transferSuffix + strTransferValue;
			else
				strTransferValue += transferSuffix;

		} else if (prefShowSuffix) {
			if (prefPosition == 0)
				strTransferValue = transferSuffix + strTransferValue;
			else
				strTransferValue += transferSuffix;
		}

		return strTransferValue;
	}

	private final void update() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.postDelayed(mRunnable, prefUpdateInterval);
	}

	private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				mTrafficHandler.sendEmptyMessage(0);
			} catch (Exception e) {
				Log.e(TAG, "run failed: ", e);
				Common.throwException(e);
			}
		}
	};

	private final void updateViewVisibility() {
		if (isReady() && networkState && prefNetworkType.contains(networkType)) {
			if (mAttached) {
				updateTraffic();
			}
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
//		Log.d(TAG, "Visibility (visible: ", View.VISIBLE, "; gone: ", View.GONE, "): ", getVisibility());
	}

	private final void loadPreferences() {
		try {
			PreferenceUtils preferenceUtils = PreferenceUtils.get();

			// fetch all preferences first
			int localPrefForceUnit = preferenceUtils.getInt(getContext(), Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
			int localPrefMinUnit = preferenceUtils.getInt(getContext(), Common.KEY_MIN_UNIT, Common.DEF_MIN_UNIT);
			int localPrefUnitMode = preferenceUtils.getInt(getContext(), Common.KEY_UNIT_MODE, Common.DEF_UNIT_MODE);
			Set<String> localPrefUnitFormat = preferenceUtils.getStringSet(getContext(), Common.KEY_UNIT_FORMAT, Common.DEF_UNIT_FORMAT);
			int localPrefHideBelow = preferenceUtils.getInt(getContext(), Common.KEY_HIDE_BELOW, Common.DEF_HIDE_BELOW);
			boolean localPrefShowSuffix = preferenceUtils.getBoolean(getContext(), Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
			float localPrefFontSize = preferenceUtils.getFloat(getContext(), Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
			int localPrefPosition = preferenceUtils.getInt(getContext(), Common.KEY_POSITION, Common.DEF_POSITION);
			int localPrefSuffix = preferenceUtils.getInt(getContext(), Common.KEY_SUFFIX, Common.DEF_SUFFIX);
			Set<String> localPrefNetworkType = preferenceUtils.getStringSet(getContext(), Common.KEY_NETWORK_TYPE, Common.DEF_NETWORK_TYPE);
			Set<String> localPrefNetworkSpeed = preferenceUtils.getStringSet(getContext(), Common.KEY_NETWORK_SPEED, Common.DEF_NETWORK_SPEED);
			int localPrefDisplay = preferenceUtils.getInt(getContext(), Common.KEY_DISPLAY, Common.DEF_DISPLAY);
			boolean localPrefSwapSpeeds = preferenceUtils.getBoolean(getContext(), Common.KEY_SWAP_SPEEDS, Common.DEF_SWAP_SPEEDS);
			int localPrefUpdateInterval = preferenceUtils.getInt(getContext(), Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
			boolean localPrefFontColor = preferenceUtils.getBoolean(getContext(), Common.KEY_FONT_COLOR, Common.DEF_FONT_COLOR);
			int localPrefColor = preferenceUtils.getInt(getContext(), Common.KEY_COLOR, Common.DEF_COLOR);
			Set<String> localPrefFontStyle = preferenceUtils.getStringSet(getContext(), Common.KEY_FONT_STYLE, Common.DEF_FONT_STYLE);
			boolean localEnableLogging = preferenceUtils.getBoolean(getContext(), Common.KEY_ENABLE_LOG, Common.DEF_ENABLE_LOG);
			int localSpeedWay = preferenceUtils.getInt(getContext(), Common.KEY_GET_SPEED_WAY, Common.DEF_SPEED_WAY);
			int localMinWidth = preferenceUtils.getInt(getContext(), Common.KEY_MIN_WIDTH, Common.DEF_MIN_WIDTH);

//			only when all are fetched, set them to fields
			prefForceUnit = localPrefForceUnit;
			prefMinUnit = localPrefMinUnit;
			prefUnitMode = localPrefUnitMode;
			prefUnitFormat = localPrefUnitFormat;
			prefSpeedWay = localSpeedWay;
			prefHideBelow = localPrefHideBelow;
			prefShowSuffix = localPrefShowSuffix;
			prefFontSize = localPrefFontSize;
			prefPosition = localPrefPosition;
			prefSuffix = localPrefSuffix;
			prefNetworkType = localPrefNetworkType;
			prefNetworkSpeed = localPrefNetworkSpeed;
			prefDisplay = localPrefDisplay;
			prefSwapSpeeds = localPrefSwapSpeeds;
			prefUpdateInterval = localPrefUpdateInterval;
			prefFontColor = localPrefFontColor;
			prefColor = localPrefColor;
			prefFontStyle = localPrefFontStyle;
			prefMinWidth = localMinWidth;
			Log.enableLogging = localEnableLogging;

		} catch (Exception e) {
			Log.e(TAG, "loadPreferences failure ignored, using defaults. Exception: ", e);
		}
	}
}
