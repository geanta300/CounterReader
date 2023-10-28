package com.example.counterreader.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "CountersDB.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "contors";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CHIRIAS = "chirias";
    public static final String COLUMN_LOCATIE = "locatie";
    public static final String COLUMN_FEL_CONTOR = "fel_contor";
    public static final String COLUMN_SERIE = "serie";
    public static final String COLUMN_INDEX_VECHI = "index_vechi";
    public static final String COLUMN_INDEX_NOU = "index_nou";
    public static final String COLUMN_IMAGE_URI = "image_uri";
    public static final String COLUMN_COD_QR = "cod_qr";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CHIRIAS + " TEXT, " +
                COLUMN_LOCATIE + " TEXT, " +
                COLUMN_FEL_CONTOR + " TEXT, " +
                COLUMN_SERIE + " TEXT, " +
                COLUMN_INDEX_VECHI + " REAL, " +
                COLUMN_INDEX_NOU + " REAL, " +
                COLUMN_IMAGE_URI + " TEXT, " +
                COLUMN_COD_QR + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {
                COLUMN_CHIRIAS,
                COLUMN_LOCATIE,
                COLUMN_FEL_CONTOR,
                COLUMN_SERIE,
                COLUMN_INDEX_VECHI,
                COLUMN_INDEX_NOU,
                COLUMN_IMAGE_URI,
                COLUMN_COD_QR
        };

        // Execute the query to fetch all rows from the table
        return db.query(TABLE_NAME, columns, null, null, null, null, null);
    }

    public int getRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;

        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }


    public void addPhotoPath(int id, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_URI, imageUri);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        db.update(TABLE_NAME, values, whereClause, whereArgs);
    }

    public void addNewIndex(int id, double newIndex) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_INDEX_NOU, newIndex);

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        db.update(TABLE_NAME, values, whereClause, whereArgs);
    }

    public void insertData(String chirias, String locatie, String felContor, String serie, double indexVechi, String codQR) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CHIRIAS, chirias);
        values.put(COLUMN_LOCATIE, locatie);
        values.put(COLUMN_FEL_CONTOR, felContor);
        values.put(COLUMN_SERIE, serie);
        values.put(COLUMN_INDEX_VECHI, indexVechi);
        values.put(COLUMN_COD_QR, codQR);

        db.insert(TABLE_NAME, null, values);
    }

    public void updateData(int id, String chirias, String locatie, String felContor, String serie, double indexVechi, double indexNou, String codQR) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};

        values.put(COLUMN_CHIRIAS, chirias);
        values.put(COLUMN_LOCATIE, locatie);
        values.put(COLUMN_FEL_CONTOR, felContor);
        values.put(COLUMN_SERIE, serie);
        values.put(COLUMN_INDEX_VECHI, indexVechi);
        values.put(COLUMN_INDEX_NOU, indexNou);
        values.put(COLUMN_COD_QR, codQR);

        db.update(TABLE_NAME, values, whereClause, whereArgs);
    }

    public Cursor getDataByQR(String qr) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_COD_QR + " = ?";
        String[] selectionArgs = {qr};

        return db.query(
                TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
    }

    public int getIndexesHigherThanZero() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_INDEX_NOU};
        Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                double newIndex = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_INDEX_NOU));
                if (newIndex > 0) {
                    count++;
                }
            } while (cursor.moveToNext());

            cursor.close();
        }

        return count;
    }

    public Cursor getCountersLeft() {
        SQLiteDatabase db = getReadableDatabase();

        String[] columns = {
                COLUMN_CHIRIAS,
                COLUMN_LOCATIE,
                COLUMN_FEL_CONTOR,
                COLUMN_SERIE
        };

        String selection = COLUMN_INDEX_NOU + "=?";
        String[] selectionArgs = {"0"};

        return db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);
    }

    public boolean qrCodeExists(String qrCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COLUMN_ID},
                COLUMN_COD_QR + " = ?",
                new String[]{qrCode},
                null,
                null,
                null)
        ){
            return cursor != null && cursor.moveToFirst();
        }
    }
}
