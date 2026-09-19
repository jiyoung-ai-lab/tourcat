package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.util.LogUtil

class TravelImageDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    companion object {
        private const val TAG = "TravelImageDbAdapter"

        const val KEY_ID = "_id"
        const val KEY_TRAVELID = "travelid"
        const val KEY_GUBUN = "gubun"
        const val KEY_URL = "url"
        const val KEY_CONFIRMDATE = "confirmdate"

        private const val DATABASE_CREATE_TRAVEL_IMAGE = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TRAVELID INTEGER,
                $KEY_GUBUN TEXT,
                $KEY_URL TEXT,
                $KEY_CONFIRMDATE TEXT
            );
        """
    }

    fun open(): TravelImageDbAdapter {
        dbHelper = DatabaseHelper(context)
        db = dbHelper?.writableDatabase
        return this
    }

    fun close() {
        dbHelper?.close()
        db?.let { if (it.isOpen) it.close() }
        dbHelper = null
        db = null
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(
            context,
            ComConstant.DATABASE_NAME,
            null,
            ComConstant.DATABASE_VERSION
        ) {
        override fun onCreate(db: SQLiteDatabase) {
            LogUtil.w(TAG, ">>>>>> DB CREATE Start!! : $DATABASE_CREATE_TRAVEL_IMAGE")
            db.execSQL(DATABASE_CREATE_TRAVEL_IMAGE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE}")
            onCreate(db)
        }
    }

    fun insertImage(
        travelId: Long, gubun: String, url: String, confirmDate: String
    ): Long {
        val values = ContentValues().apply {
            put(KEY_TRAVELID, travelId)
            put(KEY_GUBUN, gubun)
            put(KEY_URL, url)
            put(KEY_CONFIRMDATE, confirmDate)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, null, values) ?: -1
    }

    fun getImagesByTravelId(travelId: Long, gubun: String? = null): Cursor? {
        val selection: String
        val selectionArgs: Array<String>
        if (gubun != null) {
            selection = "$KEY_TRAVELID = ? AND $KEY_GUBUN = ?"
            selectionArgs = arrayOf(travelId.toString(), gubun)
        } else {
            selection = "$KEY_TRAVELID = ?"
            selectionArgs = arrayOf(travelId.toString())
        }
        return db?.query(
            ComConstant.DATABASE_TABLE_TRAVEL_IMAGE,
            null,
            selection,
            selectionArgs,
            null, null, null
        )
    }

    fun deleteImageById(imageId: Long): Boolean {
        val whereClause = "$KEY_ID = ?"
        val whereArgs = arrayOf(imageId.toString())
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }

    fun deleteImagesByTravelId(travelId: Long): Boolean {
        val whereClause = "$KEY_TRAVELID = ?"
        val whereArgs = arrayOf(travelId.toString())
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }
}
