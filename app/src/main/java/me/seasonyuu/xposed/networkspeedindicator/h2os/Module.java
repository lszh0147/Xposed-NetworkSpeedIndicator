package me.seasonyuu.xposed.networkspeedindicator.h2os;

import android.annotation.SuppressLint;
import android.content.res.XResources;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.seasonyuu.xposed.networkspeedindicator.h2os.logger.Log;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.CommonPositionCallback;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.PositionCallback1p2;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.PositionCallback1p4;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.PositionCallback2p5;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.PositionCallbackMiui8;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.PositionCallbackOreo;
import me.seasonyuu.xposed.networkspeedindicator.h2os.widget.TrafficView;

public final class Module implements IXposedHookLoadPackage {

	private static final String TAG = Module.class.getSimpleName();

	private final View getClock() {
		if (trafficView == null || trafficView.mPositionCallback == null
				|| trafficView.mPositionCallback.getClockParent() == null) {
			return null;
		}

		if (trafficView.mPositionCallback.getClockParent().findViewById(clock.getId()) != null) {
			return clock;
		}

		return null;
	}

	@Override
	public final void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!PKG_NAME_SYSTEM_UI.equals(lpparam.packageName)) {
			return;
		}
		try {
			Class<?> cClock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock",
					lpparam.classLoader);

			// we hook this method to follow alpha changes in kitkat
            Method setAlpha = XposedHelpers.findMethodBestMatch(cClock, "setAlpha", Float.class);
            XposedBridge.hookMethod(setAlpha, new XC_MethodHook() {
                @SuppressLint("NewApi")
                @Override
                protected final void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    try {
                        if (param.thisObject != getClock())
                            return;

                        float targetAlpha = 1;
                        if (trafficView != null) {
                            if (statusIcons != null) {
                                targetAlpha = statusIcons.getAlpha();
                            } else if (clock != null) {
                                targetAlpha = clock.getAlpha();
                            }
                            if (trafficView.getAlpha() != targetAlpha)
                                trafficView.setAlpha(targetAlpha);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "afterHookedMethod (setAlpha) failed: ", e);
                        throw e;
                    }
                }
            });

			if (Build.VERSION.SDK_INT < 26) {
				XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar",
						lpparam.classLoader), "makeStatusBarView", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);
						if (getClock() != null)
							return;
						try {
							final Object phoneStatusBar = param.thisObject;
							final String rootViewClassName = "mStatusBarView";
							final View root = (View) XposedHelpers.getObjectField(phoneStatusBar, rootViewClassName);

							clock = root.findViewById(root.getResources().getIdentifier("clock", "id", PKG_NAME_SYSTEM_UI));
							statusIcons = root
									.findViewById(root.getResources().getIdentifier("statusIcons", "id", PKG_NAME_SYSTEM_UI));

							if (trafficView == null) {
								trafficView = new TrafficView(root.getContext());
								trafficView.clock = clock;
							}
							if (clock != null) {
								trafficView.setLayoutParams(clock.getLayoutParams());
								if (clock instanceof TextView) {
									int color = ((TextView) clock).getCurrentTextColor();
									trafficView.setIconTint(color);
									trafficView.refreshColor();
								} else {
									// probably LinearLayout in VN ROM v14.1 (need
									// to search child elements to find correct text
									// color)
									Log.w(TAG, "clock is not a TextView, it is ", clock.getClass().getSimpleName());
									int color = Common.ANDROID_SKY_BLUE;
									trafficView.setIconTint(color);
									trafficView.refreshColor();
								}
							}
							trafficView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

							Log.i(TAG, "find Clock succeed");

							Process p = null;
							String romFullVersion = "";
							String romVersion = "";
							String miuiVersion = "";
							try {
								p = new ProcessBuilder("/system/bin/getprop", "ro.rom.version").redirectErrorStream(true)
										.start();
								BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
								String line = "";
								while ((line = br.readLine()) != null) {
									romFullVersion = line;
								}
								if (romFullVersion.split("V", 2).length > 1)
									romVersion = romFullVersion.split("V", 2)[1];
								p.destroy();
							} catch (IOException e) {
								e.printStackTrace();
								Log.e(TAG, e);
							}
							if (romVersion.length() > 0) {
								if (romVersion.compareTo("2.5.0") >= 0) {
									Log.i(TAG, "PositionCallback: H2OS 2.5");
									trafficView.mPositionCallback = new PositionCallback2p5();
								} else if (romVersion.compareTo("1.4.0") >= 0) {
									Log.i(TAG, "PositionCallback: H2OS 1.4");
									trafficView.mPositionCallback = new PositionCallback1p4();
								} else if (romVersion.compareTo("1.2.0") >= 0) {
									Log.i(TAG, "PositionCallback: H2OS 1.2");
									trafficView.mPositionCallback = new PositionCallback1p2();
								}
							} else if (romFullVersion.contains("OP3_H2_Open") || romFullVersion.contains("OP3T_H2_Open")) {
								Log.i(TAG, "PositionCallback: H2OS 2.5");
								if (Build.VERSION.SDK_INT > 23)
									trafficView.mPositionCallback = new CommonPositionCallback();
								else
									trafficView.mPositionCallback = new PositionCallback2p5();
							} else {
								p = new ProcessBuilder("/system/bin/getprop", "ro.miui.ui.version.name").redirectErrorStream(true)
										.start();
								BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
								String line = "";
								while ((line = br.readLine()) != null) {
									miuiVersion = line;
								}
								if (miuiVersion.length() > 0) {
									Log.d(TAG, "Find MIUI " + miuiVersion);
									trafficView.mPositionCallback = new PositionCallbackMiui8();
								} else {
									Log.e(TAG, "ROM VERSION is null");
									// May not work
									trafficView.mPositionCallback = new CommonPositionCallback();
								}
							}

							trafficView.mPositionCallback.setup(root, trafficView);
							trafficView.refreshPosition();
						} catch (Exception e) {
							Log.e(TAG, "handleLayoutInflated failed: ", e);
							throw e;
						}
					}
				});
			} else {
				XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarView",
						lpparam.classLoader, "setBar",
						XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar",
								lpparam.classLoader), new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(MethodHookParam param) throws Throwable {
								super.afterHookedMethod(param);
								if (getClock() != null) {
									if (trafficView != null) {
										if (trafficView.getParent() != null) {
											((ViewGroup) trafficView.getParent()).removeView(trafficView);
										}
										trafficView = null;
									}
								}
								try {
									View root = (View) param.thisObject;
									clock = root.findViewById(root.getResources().getIdentifier("clock", "id", PKG_NAME_SYSTEM_UI));
									statusIcons = root
											.findViewById(root.getResources().getIdentifier("statusIcons", "id", PKG_NAME_SYSTEM_UI));

									if (trafficView == null) {
										trafficView = new TrafficView(root.getContext());
										trafficView.clock = clock;
									}
									if (clock != null) {
										trafficView.setLayoutParams(clock.getLayoutParams());
										if (clock instanceof TextView) {
											int color = ((TextView) clock).getCurrentTextColor();
											trafficView.setIconTint(color);
											trafficView.refreshColor();
										} else {
											// probably LinearLayout in VN ROM v14.1 (need
											// to search child elements to find correct text
											// color)
											Log.w(TAG, "clock is not a TextView, it is ", clock.getClass().getSimpleName());
											int color = Common.ANDROID_SKY_BLUE;
											trafficView.setIconTint(color);
											trafficView.refreshColor();
										}
									}
									trafficView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

									Log.i(TAG, "find Clock succeed");

									trafficView.mPositionCallback = new PositionCallbackOreo();
									trafficView.mPositionCallback.setup(root, trafficView);
									trafficView.refreshPosition();
								} catch (Exception e) {
									Log.e(TAG, "handleStatusBarInit failed: ", e);
									throw e;
								}
							}
						});
			}

		} catch (ClassNotFoundError | NoSuchMethodError e) {
			// Clock class not found, ignore
			Log.w(TAG, "handleLoadPackage failure ignored: ", e);
		}

		String iconTintClassName;
		String methodName;
		switch (Build.VERSION.SDK_INT) {
			case 26:
			case 27:
				iconTintClassName = "com.android.systemui.statusbar.phone.DarkIconDispatcherImpl";
				methodName = "applyIconTint";
				break;
			default:
				iconTintClassName = "com.android.systemui.statusbar.phone.StatusBarIconController";
				methodName = "applyIconTint";
				break;

		}
		try {
			final Class<?> sbiCtrlClass = XposedHelpers
					.findClass(iconTintClassName,
							lpparam.classLoader);
			XposedHelpers.findAndHookMethod(sbiCtrlClass, methodName, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (trafficView != null) {
						int iconTint = XposedHelpers.getIntField(param.thisObject, "mIconTint");
						trafficView.setIconTint(iconTint);
						trafficView.refreshColor();
					}
				}
			});
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			// Clock class not found, try to hook text color
			hookClockColor(XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock",
					lpparam.classLoader));
			Log.w(TAG, "handleLoadPackage failure ignored: ", e);
		}

	}

	private void hookClockColor(final Class clazz) {
		try {
			Method setTextColor = XposedHelpers.findMethodBestMatch(clazz, "setTextColor", Integer.class);
			XposedBridge.hookMethod(setTextColor, new XC_MethodHook() {
				@Override
				protected final void afterHookedMethod(final MethodHookParam param) throws Throwable {
					try {
						if (trafficView == null)
							return;
						if (param.thisObject instanceof TextView) {
							TextView textView = (TextView) param.thisObject;
							int color = textView.getCurrentTextColor();
							if (trafficView.getIconTint() != color) {
								trafficView.setIconTint(color);
								trafficView.refreshColor();
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "afterHookedMethod (setTextColor) failed: ", e);
						throw e;
					}
				}
			});
		} catch (ClassNotFoundError | NoSuchMethodError e) {
			// Clock class not found, ignore
			Log.w(TAG, "handleLoadPackage failure ignored: ", e);
		}
	}

	private TrafficView trafficView;
	private View clock;
	private View statusIcons;

	private static final String PKG_NAME_SYSTEM_UI = "com.android.systemui";

	private static final Map<String, String> mLayouts;

	static {
		Map<String, String> tmpMap = new HashMap<String, String>();
		tmpMap.put("tw_super_status_bar", "statusIcons");
		tmpMap.put("status_bar", "statusIcons");

		mLayouts = Collections.unmodifiableMap(tmpMap);
	}

//	@Override
//	public final void handleInitPackageResources(final XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
//		try {
//			if (!resparam.packageName.equals(PKG_NAME_SYSTEM_UI)) {
//				return;
//			}
//			XResources res = resparam.res;
//
//			final Entry<String, String> layoutInfo = findLayoutInfo(res);
//			if (layoutInfo == null) {
//				return;
//			}
//
//			res.hookLayout(PKG_NAME_SYSTEM_UI, "layout", layoutInfo.getKey(), new XC_LayoutInflated() {
//
//				@Override
//				public final void handleLayoutInflated(final LayoutInflatedParam liparam) throws Throwable {
//
//				}
//			});
//		} catch (Exception e) {
//			Log.e(TAG, "handleInitPackageResources failed: ", e);
//			throw e;
//		}
//	}

	private static final Entry<String, String> findLayoutInfo(final XResources res) {
		Iterator<Entry<String, String>> iterator = mLayouts.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			if (res.getIdentifier(entry.getKey(), "layout", PKG_NAME_SYSTEM_UI) != 0
					&& res.getIdentifier(entry.getValue(), "id", PKG_NAME_SYSTEM_UI) != 0)
				return entry;
		}

		return null;
	}

}
