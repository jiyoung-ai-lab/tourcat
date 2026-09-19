package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant

class PushDataDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    companion object {

        // 필드명 상수
        const val KEY_ID = "_id"
        const val KEY_TYPE = "type"
        const val KEY_COUNTRY = "country"
        const val KEY_CITY = "city"
        const val KEY_DATE = "date"
        const val KEY_CONTENT = "content"
        const val KEY_RECEIVED_AT = "received_at"


        // 테이블 생성 쿼리
        private const val DATABASE_CREATE_PUSHDATA = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_PUSH} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TYPE TEXT,
                $KEY_COUNTRY TEXT,
                $KEY_CITY TEXT,
                $KEY_DATE TEXT,
                $KEY_CONTENT TEXT,
                $KEY_RECEIVED_AT INTEGER
            );
        """
    }

    // DB 열기/닫기
    fun open(): PushDataDbAdapter {
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

    // 데이터 insert
    fun insertPushData(
        type: String,
        country: String?,
        city: String?,
        date: String?,
        content: String?
    ): Long {
        val values = ContentValues().apply {
            put(KEY_TYPE, type)
            put(KEY_COUNTRY, country)
            put(KEY_CITY, city)
            put(KEY_DATE, date)
            put(KEY_CONTENT, content)
            put(KEY_RECEIVED_AT, System.currentTimeMillis())
        }
        return db?.insert(ComConstant.DATABASE_TABLE_PUSH, null, values) ?: -1
    }

    // 데이터 전체 조회(타입 필터링 지원)
    fun getPushDataList(type: String? = null): Cursor? {
        return if (type == null) {
            db?.query(ComConstant.DATABASE_TABLE_PUSH, null, null, null, null, null, "$KEY_RECEIVED_AT DESC")
        } else {
            db?.query(
                ComConstant.DATABASE_TABLE_PUSH,
                null,
                "$KEY_TYPE = ?",
                arrayOf(type),
                null, null,
                "$KEY_RECEIVED_AT DESC"
            )
        }
    }

    // 내부 SQLiteOpenHelper
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_PUSHDATA)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $ComConstant.DATABASE_TABLE_PUSH")
            onCreate(db)
        }
    }
}
