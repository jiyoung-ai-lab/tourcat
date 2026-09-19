package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.util.DateTimeUtil

class NoticeDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    companion object {
        // 필드명 상수
        const val KEY_ID = "_id"
        const val KEY_TITLE = "title"
        const val KEY_CONTENT = "content"
        const val KEY_DATE = "date"           // 기준일
        const val KEY_COUNTRY = "country"
        const val KEY_LANGUAGE = "language"
        const val KEY_RECEIVED_AT = "received_at"
        const val KEY_IS_READ = "is_read"     // 0:안읽음, 1:읽음

        // 테이블 생성 쿼리
        private const val DATABASE_CREATE_NOTICE = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_NOTICE} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT,
                $KEY_CONTENT TEXT,
                $KEY_DATE TEXT,
                $KEY_COUNTRY TEXT,
                $KEY_LANGUAGE TEXT,
                $KEY_RECEIVED_AT INTEGER,
                $KEY_IS_READ INTEGER DEFAULT 0                
            );
        """
    }

    // DB 열기/닫기
    fun open(): NoticeDbAdapter {
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

    // 공지 저장
    fun insertNotice(
        title: String?,
        content: String?,
        date: String?,
        country: String?,
        language: String?
    ): Long {
        val values = ContentValues().apply {
            put(KEY_TITLE, title)
            put(KEY_CONTENT, content)
            put(KEY_DATE, date)
            put(KEY_COUNTRY, country)
            put(KEY_LANGUAGE, language)
            put(KEY_RECEIVED_AT, System.currentTimeMillis())
            put(KEY_IS_READ, 0)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_NOTICE, null, values) ?: -1
    }

    // 전체 공지 목록(최신순)
    fun getNoticeList(): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_NOTICE, null, null, null, null, null, "$KEY_RECEIVED_AT DESC"
        )
    }

    // --- 추가: 페이징(최신순 20개 단위로) ---
    fun getNoticeListPaged(offset: Int, limit: Int): Cursor? {
        // LIMIT/OFFSET 직접 쿼리 사용
        val sql = "SELECT * FROM ${ComConstant.DATABASE_TABLE_NOTICE} ORDER BY $KEY_RECEIVED_AT DESC LIMIT $limit OFFSET $offset"
        return db?.rawQuery(sql, null)
    }

    // 읽음 처리
    fun markNoticeAsRead(id: Long) {
        val values = ContentValues().apply { put(KEY_IS_READ, 1) }
        db?.update(
            ComConstant.DATABASE_TABLE_NOTICE, values, "$KEY_ID = ?", arrayOf(id.toString())
        )
    }

    // 개별 공지 조회
    fun getNoticeById(id: Long): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_NOTICE,
            null, "$KEY_ID = ?",
            arrayOf(id.toString()), null, null, null
        )
    }

    // 읽지 않은 공지 카운트
    fun getUnreadNoticeCount(language: String): Int {
        val cursor = db?.rawQuery(
            "SELECT COUNT(*) FROM ${ComConstant.DATABASE_TABLE_NOTICE} WHERE $KEY_IS_READ = 0 AND $KEY_LANGUAGE = ?",
            arrayOf(language)
        )
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    // 언어와 (필요하다면 나라까지)로 필터링해서 공지사항 가져오기
    fun getNoticeListByLangCountry(language: String, country: String? = null): Cursor? {
        val selection = if (country.isNullOrBlank()) {
            "$KEY_LANGUAGE = ?"
        } else {
            "$KEY_LANGUAGE = ? AND $KEY_COUNTRY = ?"
        }
        val selectionArgs = if (country.isNullOrBlank()) {
            arrayOf(language)
        } else {
            arrayOf(language, country)
        }
        return db?.query(
            ComConstant.DATABASE_TABLE_NOTICE,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$KEY_RECEIVED_AT DESC"
        )
    }
    // 내부 SQLiteOpenHelper
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_NOTICE)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_NOTICE}")
            onCreate(db)
        }
    }
}
