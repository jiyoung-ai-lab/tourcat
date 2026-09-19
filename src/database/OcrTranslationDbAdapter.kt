package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.TranslationItem

class OcrTranslationDbAdapter(private val context: Context) {
    companion object {
        const val KEY_ID = "_id"
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_OCR_TEXT = "ocr_text"
        const val KEY_TRANSLATED_TEXT = "translated_text"
        const val KEY_LANG_CODE = "lang_code"
        const val KEY_CREATED_AT = "created_at"
    }

    private var dbHelper: SQLiteOpenHelper? = null
    private var db: SQLiteDatabase? = null

    fun open(): OcrTranslationDbAdapter {
        dbHelper = object : SQLiteOpenHelper(context, ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION} (
                        $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $KEY_IMAGE_PATH TEXT,
                        $KEY_OCR_TEXT TEXT,
                        $KEY_TRANSLATED_TEXT TEXT,
                        $KEY_LANG_CODE TEXT,
                        $KEY_CREATED_AT TEXT
                    );
                """.trimIndent())
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION}")
                onCreate(db)
            }
        }
        db = dbHelper!!.writableDatabase
        return this
    }

    fun close() {
        dbHelper?.close()
        db?.let { if (it.isOpen) it.close() }
        dbHelper = null
        db = null
    }

    fun beginTransaction() = db?.beginTransaction()
    fun setTransactionSuccessful() = db?.setTransactionSuccessful()
    fun endTransaction() = db?.endTransaction()

    fun insertOcrTranslation(
        imagePath: String,
        ocrText: String,
        translatedText: String,
        langCode: String,
        createdAt: String
    ): Long {
        val values = ContentValues().apply {
            put(KEY_IMAGE_PATH, imagePath)
            put(KEY_OCR_TEXT, ocrText)
            put(KEY_TRANSLATED_TEXT, translatedText)
            put(KEY_LANG_CODE, langCode)
            put(KEY_CREATED_AT, createdAt)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_OCR_TRANSLATION, null, values) ?: -1
    }

    fun insertResult(item: TranslationItem): Long {
        val values = ContentValues().apply {
            put(KEY_IMAGE_PATH, item.imagePath)
            put(KEY_OCR_TEXT, item.ocrText)
            put(KEY_TRANSLATED_TEXT, item.translatedText)
            put(KEY_LANG_CODE, item.langCode)
            put(KEY_CREATED_AT, item.createdAt)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_OCR_TRANSLATION, null, values) ?: -1L
    }

    fun deleteById(id: Long): Int {
        return db?.delete(
            ComConstant.DATABASE_TABLE_OCR_TRANSLATION,
            "$KEY_ID=?",
            arrayOf(id.toString())
        ) ?: 0
    }

    // 1. 전체조회 (기존)
    fun getAllResults(): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_OCR_TRANSLATION,
            null, null, null, null, null,
            "$KEY_CREATED_AT DESC"
        )
    }

    // 2. 부분조회(페이징)
    fun getResults(limit: Int, offset: Int = 0): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_OCR_TRANSLATION,
            null,
            null,
            null,
            null,
            null,
            "$KEY_CREATED_AT DESC",
            "$limit OFFSET $offset"
        )
    }

    // 3. 커서 기반 조회 (lastId 보다 작은 것만, 최신순)
    fun getResultsAfterId(lastId: Long, limit: Int): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_OCR_TRANSLATION,
            null,
            "$KEY_ID < ?",
            arrayOf(lastId.toString()),
            null,
            null,
            "$KEY_ID DESC",
            "$limit"
        )
    }

    // 4. 전체 데이터 개수 반환
    fun getTotalCount(): Int {
        val cursor = db?.rawQuery(
            "SELECT COUNT(*) FROM ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION}",
            null
        )
        var count = 0
        cursor?.use {
            if (it.moveToFirst()) count = it.getInt(0)
        }
        return count
    }
}
