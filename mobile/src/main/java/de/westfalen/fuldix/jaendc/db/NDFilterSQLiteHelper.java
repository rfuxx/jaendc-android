package de.westfalen.fuldix.jaendc.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.westfalen.fuldix.jaendc.model.NDFilter;

public class NDFilterSQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ndfilters";
    private static final int DATABASE_VERSION = 1;

    public static final String NDFILTERS_TABLE = "ndfilters";
    public static final String NDFILTERS_NAME = "name";
    public static final String NDFILTERS_FACTOR = "factor";
    public static final String ID = "_id";
    public static final String ORDERPOS = "orderpos";

    public NDFilterSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table ndfilters(_id integer primary key, name text, factor real, orderpos integer);");
        insertDefaultFilters(db);
    }

    public void insertDefaultFilters(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        int pos=0;
        for(NDFilter fi : NDFilter.builtinFilters) {
            values.put(ID, fi.getId());
            values.put(NDFILTERS_NAME, fi.getName());
            values.put(NDFILTERS_FACTOR, fi.getFactor());
            values.put(ORDERPOS, pos++);
            db.insert(NDFILTERS_TABLE, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.err.println("Hey, I haven't made different versions yet.");
    }
}
