package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant

class GptSearchDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    companion object {
        // 필드명 상수
        const val KEY_ID = "_id"
        const val KEY_USER_UID = "user_uid"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_CATEGORY = "category"
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_QUERY_TEXT = "query_text"
        const val KEY_RESULT_TEXT = "result_text"
        const val KEY_STATUS = "status"
        const val KEY_PRICE = "price"
        const val KEY_REQUESTED_AT = "requested_at"
        const val KEY_RESPONSE_AT = "response_at"
        const val KEY_FAIL_REASON = "fail_reason"

        private const val DATABASE_CREATE_GPTSEARCH = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_GPT_SEARCH} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_UID TEXT,
                $KEY_USER_EMAIL TEXT,
                $KEY_CATEGORY TEXT,
                $KEY_IMAGE_URL TEXT,
                $KEY_QUERY_TEXT TEXT,
                $KEY_RESULT_TEXT TEXT,
                $KEY_STATUS TEXT,
                $KEY_PRICE INTEGER,
                $KEY_REQUESTED_AT TEXT,
                $KEY_RESPONSE_AT TEXT,
                $KEY_FAIL_REASON TEXT
            );
        """
    }

    fun open(): GptSearchDbAdapter {
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

    fun insertGptSearch(
        userUid: String,
        userEmail: String,
        category: String,
        imageUrl: String?,
        queryText: String,
        resultText: String?,
        status: String,
        price: Int,
        requestedAt: String,
        responseAt: String?,
        failReason: String?
    ): Long {
        val values = ContentValues().apply {
            put(KEY_USER_UID, userUid)
            put(KEY_USER_EMAIL, userEmail)
            put(KEY_CATEGORY, category)
            put(KEY_IMAGE_URL, imageUrl)
            put(KEY_QUERY_TEXT, queryText)
            put(KEY_RESULT_TEXT, resultText)
            put(KEY_STATUS, status)
            put(KEY_PRICE, price)
            put(KEY_REQUESTED_AT, requestedAt)
            put(KEY_RESPONSE_AT, responseAt)
            put(KEY_FAIL_REASON, failReason)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_GPT_SEARCH, null, values) ?: -1
    }

    // 1. 전체 이력 조회 (카테고리 필터링 지원)  -- 실패건 제외
    fun getGptSearchList(category: String? = null): Cursor? {
        return if (category == null) {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null,
                "$KEY_STATUS != ?",
                arrayOf("fail"),
                null, null,
                "$KEY_REQUESTED_AT DESC"
            )
        } else {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null,
                "$KEY_CATEGORY = ? AND $KEY_STATUS != ?",
                arrayOf(category, "fail"),
                null, null,
                "$KEY_REQUESTED_AT DESC"
            )
        }
    }

    // 2. 부분조회(페이징, 오프셋 방식)
    fun getGptSearchListPaged(limit: Int, offset: Int = 0, category: String? = null): Cursor? {
        return if (category == null) {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null, null, null, null, null,
                "$KEY_REQUESTED_AT DESC",
                "$limit OFFSET $offset"
            )
        } else {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null,
                "$KEY_CATEGORY = ?",
                arrayOf(category),
                null, null,
                "$KEY_REQUESTED_AT DESC",
                "$limit OFFSET $offset"
            )
        }
    }

    // 3. 커서 기반 조회(마지막 ID 이후로 추가 조회, 최신순)
    fun getGptSearchListAfterId(lastId: Long, limit: Int, category: String? = null): Cursor? {
        return if (category == null) {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null,
                "$KEY_ID < ?",
                arrayOf(lastId.toString()),
                null, null,
                "$KEY_ID DESC",
                "$limit"
            )
        } else {
            db?.query(
                ComConstant.DATABASE_TABLE_GPT_SEARCH,
                null,
                "$KEY_CATEGORY = ? AND $KEY_ID < ?",
                arrayOf(category, lastId.toString()),
                null, null,
                "$KEY_ID DESC",
                "$limit"
            )
        }
    }

    // 4. 전체 개수 반환 (카테고리별로도 가능)
    fun getTotalCount(category: String? = null): Int {
        val sql = if (category == null) {
            "SELECT COUNT(*) FROM ${ComConstant.DATABASE_TABLE_GPT_SEARCH}"
        } else {
            "SELECT COUNT(*) FROM ${ComConstant.DATABASE_TABLE_GPT_SEARCH} WHERE $KEY_CATEGORY = ?"
        }
        val cursor = if (category == null) {
            db?.rawQuery(sql, null)
        } else {
            db?.rawQuery(sql, arrayOf(category))
        }
        var count = 0
        cursor?.use {
            if (it.moveToFirst()) count = it.getInt(0)
        }
        return count
    }

    fun deleteById(id: Long): Int {
        return db?.delete(
            ComConstant.DATABASE_TABLE_GPT_SEARCH,
            "$KEY_ID=?",
            arrayOf(id.toString())
        ) ?: 0
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_GPTSEARCH)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // 이미지 URL 컬럼 추가 (마이그레이션 예시)
            if (oldVersion < newVersion) {
                try {
                    db.execSQL("ALTER TABLE ${ComConstant.DATABASE_TABLE_GPT_SEARCH} ADD COLUMN $KEY_IMAGE_URL TEXT")
                } catch (e: Exception) {
                    // 이미 컬럼 있으면 무시
                }
            }
        }
    }
}
