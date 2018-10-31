package me.seasonyuu.xposed.networkspeedindicator.h2os.preference;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import me.seasonyuu.xposed.networkspeedindicator.h2os.preference.PreferenceProvider.Column;

/**
 * To perform as Preference Manager, but use database to save data.
 * <p>
 * Created by seasonyuu on 2018/1/3.
 */
@SuppressWarnings("unused")
public class PreferenceUtils {
	private static PreferenceUtils preferenceUtils;

	private PreferenceUtils() {

	}

	public static PreferenceUtils get() {
		if (preferenceUtils == null)
			preferenceUtils = new PreferenceUtils();
		return preferenceUtils;
	}

	public void putString(Context context, String key, String value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.STRING_VALUE, value);
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}

	public String getString(Context context, String key, String defaultValue) {
		String result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defaultValue;
		cursor.moveToFirst();
		result = cursor.getString(cursor.getColumnIndex(Column.STRING_VALUE));
		cursor.close();
		return result != null ? result : defaultValue;
	}

	public void putBoolean(Context context, String key, boolean value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.BOOLEAN_VALUE, value ? 1 : 0);
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}

	public boolean getBoolean(Context context, String key, boolean defaultValue) {
		boolean result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defaultValue;
		cursor.moveToFirst();
		result = cursor.getInt(cursor.getColumnIndex(Column.BOOLEAN_VALUE)) == 1;
		cursor.close();
		return result;
	}

	public void putInt(Context context, String key, int value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.INT_VALUE, value);
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}

	public int getInt(Context context, String key, int defaultValue) {
		int result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defaultValue;
		cursor.moveToFirst();
		result = cursor.getInt(cursor.getColumnIndex(Column.INT_VALUE));
		cursor.close();
		return result;
	}

	public void putFloat(Context context, String key, float value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.FLOAT_VALUE, value);
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}


	public float getFloat(Context context, String key, float defaultValue) {
		float result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defaultValue;
		cursor.moveToFirst();
		result = cursor.getFloat(cursor.getColumnIndex(Column.FLOAT_VALUE));
		cursor.close();
		return result;
	}

	public void putStringSet(Context context, String key, Set<String> value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.STRING_SET_VALUE, stringSetToString(value));
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}

	public Set<String> getStringSet(Context context, String key, Set<String> defStringSet) {
		String result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defStringSet;
		cursor.moveToFirst();
		result = cursor.getString(cursor.getColumnIndex(Column.STRING_SET_VALUE));
		cursor.close();
		return stringToStringSet(result);
	}

	public void putLong(Context context, String key, long value) {
		if (key == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues contentValues = new ContentValues();
		contentValues.put(Column.PREFERENCE_KEY, key);
		contentValues.put(Column.LONG_VALUE, value);
		contentResolver.insert(PreferenceProvider.RESOLVED_URL, contentValues);
	}

	public long getLong(Context context, String key, long defaultValue) {
		long result;
		Cursor cursor = context.getContentResolver().query(PreferenceProvider.RESOLVED_URL, null,
				Column.PREFERENCE_KEY + " = ?", new String[]{key}, null);
		if (cursor == null || cursor.getCount() == 0)
			return defaultValue;
		cursor.moveToFirst();
		result = cursor.getLong(cursor.getColumnIndex(Column.LONG_VALUE));
		cursor.close();
		return result;
	}

	private String stringSetToString(Set<String> stringSet) {
		StringBuilder sb = new StringBuilder();
		for (String s : stringSet) {
			sb.append(s).append("\n");
		}
		return sb.toString();
	}

	private Set<String> stringToStringSet(String string) {
		if (string == null || string.length() == 0)
			return null;
		Set<String> stringSet = new HashSet<>();
		String[] strings = string.split("\n");
		stringSet.addAll(Arrays.asList(strings));
		return stringSet;
	}
}
