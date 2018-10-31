package me.seasonyuu.xposed.networkspeedindicator.h2os.preference;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/**
 * To provide preference saved data
 * Created by seasonyuu on 2018/1/3.
 */

public class PreferenceProvider extends ContentProvider {
    private static final String AUTHORITY = "me.seasonyuu.xposed.networkspeedindicator.h2os";
    public static final String TYPE_PREFERENCE = "preference";
    private static final String DATABASE_NAME = "preference.db";
    private static final String TABLE_NAME = "preference";
    private static final int VERSION = 1;

    public static final Uri RESOLVED_URL = Uri.parse("content://" + AUTHORITY + "/preference");

    private PreferenceDatabase database;

    @Override
    public boolean onCreate() {
        database = new PreferenceDatabase(getContext(), DATABASE_NAME, null, VERSION);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = database.getReadableDatabase();
        return db.query(TABLE_NAME, projection,
                selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        return TYPE_PREFERENCE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = database.getWritableDatabase();
        db.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return uri;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count = db.delete(TABLE_NAME, s, strings);
        db.close();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count = db.update(TABLE_NAME, contentValues, s, strings);
        db.close();
        return count;
    }

    private class PreferenceDatabase extends SQLiteOpenHelper {

        PreferenceDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(
                    "create table " + TABLE_NAME + "(" +
                            Column.PREFERENCE_KEY + " text primary key not null," +
                            Column.INT_VALUE + " int," +
                            Column.BOOLEAN_VALUE + " int," +
                            Column.FLOAT_VALUE + " real," +
                            Column.LONG_VALUE + " int," +
                            Column.STRING_SET_VALUE + " text," +
                            Column.STRING_VALUE + " text);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }

    static final class Column {
        static final String PREFERENCE_KEY = "preference_key";
        static final String INT_VALUE = "int_value";
        static final String BOOLEAN_VALUE = "boolean_value";
        static final String FLOAT_VALUE = "float_value";
        static final String LONG_VALUE = "long_value";
        static final String STRING_VALUE = "string_value";
        static final String STRING_SET_VALUE = "string_set_value";
    }
}
