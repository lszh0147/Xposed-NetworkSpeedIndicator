package me.seasonyuu.xposed.networkspeedindicator.h2os;

import android.view.View;

public class Utils {
	public static <T extends View> T findViewById(View root, String id, String packageName) {
		T result = null;
		result = (T) root.findViewById(root.getResources().getIdentifier(id, "id", packageName));
		if (result == null)
			result = (T) root.findViewById(root.getResources().getIdentifier(id, "id", null));
		return result;
	}
}
