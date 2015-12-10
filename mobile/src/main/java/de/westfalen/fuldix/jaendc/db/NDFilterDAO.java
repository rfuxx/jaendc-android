package de.westfalen.fuldix.jaendc.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.westfalen.fuldix.jaendc.model.NDFilter;

public class NDFilterDAO {
    private static final String[] allColumns = { NDFilterSQLiteHelper.ID, NDFilterSQLiteHelper.NDFILTERS_NAME, NDFilterSQLiteHelper.NDFILTERS_FACTOR, NDFilterSQLiteHelper.ORDERPOS };
    private static final String[] idColumn = { NDFilterSQLiteHelper.ID };
    private static final String WHERE_ID = NDFilterSQLiteHelper.ID+"=?";
    private static final String WHERE_ORDERPOS = NDFilterSQLiteHelper.ORDERPOS+"=?";
    private SQLiteDatabase database;
    private final NDFilterSQLiteHelper dbHelper;

    public NDFilterDAO(Context context) {
        dbHelper = new NDFilterSQLiteHelper(context);
    }

    public void openReadonly() {
        database = dbHelper.getReadableDatabase();
    }

    public void openWritable() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if(database != null) {
            database.close();
            database = null;
        }
    }

    public boolean isOpen() {
        return database != null;
    }

    public boolean isWritable() {
        return database != null && !database.isReadOnly();
    }

    public List<NDFilter> getAllNDFilters() {
        boolean wasOpen = isOpen();
        if(!wasOpen) {
            openReadonly();
        }
        List<NDFilter> filters = new ArrayList<>();

        Cursor cursor = database.query(NDFilterSQLiteHelper.NDFILTERS_TABLE,
                                       allColumns, null, null, null, null, NDFilterSQLiteHelper.ORDERPOS);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            NDFilter filter = getAt(cursor);
            filters.add(filter);
            cursor.moveToNext();
        }
        cursor.close();
        if(!wasOpen) {
            close();
        }
        return filters;
    }

    private NDFilter getAt(Cursor cursor) {
        return new NDFilter(cursor.getLong(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3));
    }

    public NDFilter getNDFilter(long id) {
        boolean wasWritable = isWritable();
        SQLiteDatabase database = wasWritable ? this.database : dbHelper.getWritableDatabase();
        NDFilter result;
        String[] idarg = { Long.toString(id) };
        Cursor cursor = database.query(NDFilterSQLiteHelper.NDFILTERS_TABLE,
                allColumns, WHERE_ID, idarg, null, null, null);
        if(cursor.moveToFirst()) {
            result = getAt(cursor);
        } else {
            result = new NDFilter();
        }
        cursor.close();
        if(!wasWritable) {
            database.close();
        }
        return result;
    }

    public NDFilter getNDFilterAtOrderpos(int orderpos) {
        boolean wasWritable = isWritable();
        SQLiteDatabase database = wasWritable ? this.database : dbHelper.getWritableDatabase();
        NDFilter result;
        String[] idarg = { Integer.toString(orderpos) };
        Cursor cursor = database.query(NDFilterSQLiteHelper.NDFILTERS_TABLE,
                allColumns, WHERE_ORDERPOS, idarg, null, null, null);
        if(cursor.moveToFirst()) {
            result = getAt(cursor);
        } else {
            result = null;
        }
        cursor.close();
        if(!wasWritable) {
            database.close();
        }
        return result;
    }

    public void storeNDFilter(NDFilter filter) {
        boolean wasWritable = isWritable();
        SQLiteDatabase database = wasWritable ? this.database : dbHelper.getWritableDatabase();

        if(filter.getOrderpos() < 0) {
            Cursor cursor = database.rawQuery("select max("+NDFilterSQLiteHelper.ORDERPOS+") from " + NDFilterSQLiteHelper.NDFILTERS_TABLE, null);
            cursor.moveToFirst();
            filter.setOrderpos(cursor.getInt(0)+1);
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put(NDFilterSQLiteHelper.NDFILTERS_NAME, filter.getName());
        values.put(NDFilterSQLiteHelper.NDFILTERS_FACTOR, filter.getFactor());
        values.put(NDFilterSQLiteHelper.NDFILTERS_NAME, filter.getName());
        values.put(NDFilterSQLiteHelper.ORDERPOS, filter.getOrderpos());
        if(filter.getId() != Long.MIN_VALUE) {
            String[] idarg = { Long.toString(filter.getId()) };
            database.update(NDFilterSQLiteHelper.NDFILTERS_TABLE, values, WHERE_ID, idarg);
        } else {
            long rowid = database.insert(NDFilterSQLiteHelper.NDFILTERS_TABLE, null, values);
            String[] rowidarg = { (Long.toString(rowid)) };
            Cursor cursor = database.query(NDFilterSQLiteHelper.NDFILTERS_TABLE,
                    idColumn, "rowid=?", rowidarg, null, null, null);
            if(cursor.moveToFirst()) {
                filter.setId(cursor.getLong(0));
            }
            cursor.close();
        }

        if(!wasWritable) {
            database.close();
        }
    }

    public void deleteNDFilter(NDFilter filter) {
        boolean wasWritable = isWritable();
        SQLiteDatabase database = wasWritable ? this.database : dbHelper.getWritableDatabase();

        int orderPos = filter.getOrderpos();
        String[] idarg = { Long.toString(filter.getId()) };
        database.delete(NDFilterSQLiteHelper.NDFILTERS_TABLE, WHERE_ID, idarg);

        Object[] bindArgs = { orderPos };
        database.execSQL("update " + NDFilterSQLiteHelper.NDFILTERS_TABLE
                + " set " + NDFilterSQLiteHelper.ORDERPOS + " = " + NDFilterSQLiteHelper.ORDERPOS + "-1"
                + " where " + NDFilterSQLiteHelper.ORDERPOS + " > ?", bindArgs);
        if(!wasWritable) {
            database.close();
        }
    }

    public void updateOrderPos(NDFilter filter, int newOrderPos) {
        boolean wasWritable = isWritable();
        SQLiteDatabase database = wasWritable ? this.database : dbHelper.getWritableDatabase();

        int oldOrderPos = filter.getOrderpos();
        if(oldOrderPos < newOrderPos) {
            Object[] bindArgs = { oldOrderPos, newOrderPos };
            database.execSQL("update " + NDFilterSQLiteHelper.NDFILTERS_TABLE
                    + " set " + NDFilterSQLiteHelper.ORDERPOS + " = " + NDFilterSQLiteHelper.ORDERPOS + "-1"
                    + " where " + NDFilterSQLiteHelper.ORDERPOS + " > ?"
                    + " and " + NDFilterSQLiteHelper.ORDERPOS + " <= ?", bindArgs);
        } else {
            Object[] bindArgs = { oldOrderPos, newOrderPos };
            database.execSQL("update " + NDFilterSQLiteHelper.NDFILTERS_TABLE
                    + " set " + NDFilterSQLiteHelper.ORDERPOS + " = " + NDFilterSQLiteHelper.ORDERPOS + "+1"
                    + " where " + NDFilterSQLiteHelper.ORDERPOS + " < ?"
                    + " and " + NDFilterSQLiteHelper.ORDERPOS + " >= ?", bindArgs);
        }
        Object[] bindArgs = { newOrderPos, filter.getId() };
        database.execSQL("update " + NDFilterSQLiteHelper.NDFILTERS_TABLE
                + " set " + NDFilterSQLiteHelper.ORDERPOS + " = ?"
                + " where " + NDFilterSQLiteHelper.ID + " = ?", bindArgs);
        filter.setOrderpos(newOrderPos);
        if(!wasWritable) {
            database.close();
        }
    }

    public void insertDefaultFilters() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.insertDefaultFilters(db);
        db.close();
    }
}
