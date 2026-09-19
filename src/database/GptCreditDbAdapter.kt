package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant

class GptCreditDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    companion object {
        // 필드명 상수
        const val KEY_ID = "_id"
        const val KEY_GPTSEARCH_ID = "gptsearch_id" // GPT 검색 마스터 테이블의 PK (외래키 개념)
        const val KEY_USER_UID = "user_uid"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_PREV_CREDIT = "prev_credit"
        const val KEY_USED_CREDIT = "used_credit"
        const val KEY_FINAL_CREDIT = "final_credit"
        const val KEY_CREATED_AT = "created_at"

        // 테이블 생성 쿼리
        private const val DATABASE_CREATE_GPT_CREDIT = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_GPT_CREDIT} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_GPTSEARCH_ID INTEGER,
                $KEY_USER_UID TEXT,
                $KEY_USER_EMAIL TEXT,
                $KEY_PREV_CREDIT INTEGER,
                $KEY_USED_CREDIT INTEGER,
                $KEY_FINAL_CREDIT INTEGER,
                $KEY_CREATED_AT TEXT
            );
        """
    }

    // DB 열기/닫기
    fun open(): GptCreditDbAdapter {
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
    fun insertGptCredit(
        gptsearchId: Long,
        userUid: String,
        userEmail: String,
        prevCredit: Int,
        usedCredit: Int,
        finalCredit: Int,
        createdAt: String
    ): Long {
        val values = ContentValues().apply {
            put(KEY_GPTSEARCH_ID, gptsearchId)
            put(KEY_USER_UID, userUid)
            put(KEY_USER_EMAIL, userEmail)
            put(KEY_PREV_CREDIT, prevCredit)
            put(KEY_USED_CREDIT, usedCredit)
            put(KEY_FINAL_CREDIT, finalCredit)
            put(KEY_CREATED_AT, createdAt)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_GPT_CREDIT, null, values) ?: -1
    }

    // GPT 검색 ID로 과금 이력 단건 조회
    fun getGptCreditBySearchId(gptsearchId: Long): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_GPT_CREDIT,
            null,
            "$KEY_GPTSEARCH_ID = ?",
            arrayOf(gptsearchId.toString()),
            null, null, "$KEY_CREATED_AT DESC"
        )
    }

    // 전체 이력 조회
    fun getAllGptCredits(): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_GPT_CREDIT,
            null, null, null, null, null, "$KEY_CREATED_AT DESC"
        )
    }

    // 내부 SQLiteOpenHelper
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_GPT_CREDIT)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_GPT_CREDIT}")
            onCreate(db)
        }
    }
    fun getLatestCredit(userUid: String): Int {
        val cursor = db?.query(
            ComConstant.DATABASE_TABLE_GPT_CREDIT,
            arrayOf(KEY_FINAL_CREDIT),
            "$KEY_USER_UID = ?",
            arrayOf(userUid),
            null, null,
            "$KEY_CREATED_AT DESC",
            "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getInt(it.getColumnIndexOrThrow(KEY_FINAL_CREDIT))
            }
        }
        return 0 // 기본값: 크레딧 없으면 0
    }
}
