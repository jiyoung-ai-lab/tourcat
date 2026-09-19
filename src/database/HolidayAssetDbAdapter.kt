package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.HolidayItem

class HolidayAssetDbAdapter(private val context: Context) {

    companion object {
        const val KEY_ID = "_id"
        const val KEY_YEAR = "year"
        const val KEY_MONTH = "month"
        const val KEY_DAY = "day"
        const val KEY_COUNTRY = "country"
        const val KEY_VERSION = "version"
        const val KEY_TYPE = "type"
        const val KEY_EN = "en"
        const val KEY_KO = "ko"
        const val KEY_JA = "ja"
        const val KEY_ZH = "zh"
        const val KEY_FR = "fr"
        const val KEY_DE = "de"
        const val KEY_IT = "it"
        const val KEY_TH = "th"
        const val KEY_ES = "es"
        const val KEY_REG_DATE = "reg_date"

        private const val DATABASE_CREATE_HOLIDAY = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_HOLIDAY} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_YEAR INTEGER NOT NULL,
                $KEY_MONTH INTEGER NOT NULL,
                $KEY_DAY INTEGER NOT NULL,
                $KEY_COUNTRY TEXT NOT NULL,
                $KEY_VERSION INTEGER,
                $KEY_TYPE TEXT,
                $KEY_EN TEXT,
                $KEY_KO TEXT,
                $KEY_JA TEXT,
                $KEY_ZH TEXT,
                $KEY_FR TEXT,
                $KEY_DE TEXT,
                $KEY_IT TEXT,
                $KEY_TH TEXT,
                $KEY_ES TEXT,
                $KEY_REG_DATE TEXT,
                UNIQUE($KEY_YEAR, $KEY_MONTH, $KEY_DAY, $KEY_COUNTRY, $KEY_VERSION)
            );
        """
    }

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    fun open(): HolidayAssetDbAdapter {
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

    fun insertHoliday(item: HolidayItem): Long {
        val values = ContentValues().apply {
            put(KEY_YEAR, item.year)
            put(KEY_MONTH, item.month)
            put(KEY_DAY, item.day)
            put(KEY_COUNTRY, item.country)
            put(KEY_VERSION, item.version)
            put(KEY_TYPE, item.type)
            put(KEY_EN, item.en)
            put(KEY_KO, item.ko)
            put(KEY_JA, item.ja)
            put(KEY_ZH, item.zh)
            put(KEY_FR, item.fr)
            put(KEY_DE, item.de)
            put(KEY_IT, item.it)
            put(KEY_TH, item.th)
            put(KEY_ES, item.es)
            put(KEY_REG_DATE, item.regDate)
        }
        return db?.insertWithOnConflict(
            ComConstant.DATABASE_ASSET_TABLE_HOLIDAY, null, values, SQLiteDatabase.CONFLICT_REPLACE
        ) ?: -1
    }

    fun insertHolidayList(items: List<HolidayItem>) {
        if (items.isEmpty()) return
        db?.beginTransaction()
        try {
            items.forEach { insertHoliday(it) }
            db?.setTransactionSuccessful()
        } finally {
            db?.endTransaction()
        }
    }

    fun getHoliday(year: Int, month: Int, day: Int, country: String): Cursor? {
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_HOLIDAY,
            null,
            "$KEY_YEAR=? AND $KEY_MONTH=? AND $KEY_DAY=? AND $KEY_COUNTRY=?",
            arrayOf(year.toString(), month.toString(), day.toString(), country),
            null, null, null
        )
    }

    fun getAllHoliday(): Cursor? {
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_HOLIDAY,
            null, null, null, null, null,
            "$KEY_YEAR ASC, $KEY_MONTH ASC, $KEY_DAY ASC, $KEY_COUNTRY ASC"
        )
    }

    fun clearAllHoliday(): Int {
        return db?.delete(ComConstant.DATABASE_ASSET_TABLE_HOLIDAY, null, null) ?: 0
    }

    fun getHolidayInfo(year: Int, month: Int, day: Int, country: String): HolidayItem? {
        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_HOLIDAY,
            null,
            "year=? AND month=? AND day=? AND country=?",
            arrayOf(year.toString(), month.toString(), day.toString(), country),
            null, null, null, "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return HolidayItem(
                    year = it.getInt(it.getColumnIndexOrThrow("year")),
                    month = it.getInt(it.getColumnIndexOrThrow("month")),
                    day = it.getInt(it.getColumnIndexOrThrow("day")),
                    country = it.getString(it.getColumnIndexOrThrow("country")),
                    version = it.getInt(it.getColumnIndexOrThrow("version")),
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    en = it.getString(it.getColumnIndexOrThrow("en")),
                    ko = it.getString(it.getColumnIndexOrThrow("ko")),
                    ja = it.getString(it.getColumnIndexOrThrow("ja")),
                    zh = it.getString(it.getColumnIndexOrThrow("zh")),
                    fr = it.getString(it.getColumnIndexOrThrow("fr")),
                    de = it.getString(it.getColumnIndexOrThrow("de")),
                    it = it.getString(it.getColumnIndexOrThrow("it")),
                    th = it.getString(it.getColumnIndexOrThrow("th")),
                    es = it.getString(it.getColumnIndexOrThrow("es")),
                    regDate = it.getString(it.getColumnIndexOrThrow("reg_date"))
                )
            }
        }
        return null
    }

    fun getHolidayInfoRange(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int,
        country: String
    ): HolidayItem? {
        // (year, month, day) >=> (startYear, startMonth, startDay)
        // AND (year, month, day) <= (endYear, endMonth, endDay)
        val selection = """
        country=? AND (
            (year > ? OR (year = ? AND month > ?) OR (year = ? AND month = ? AND day >= ?))
            AND
            (year < ? OR (year = ? AND month < ?) OR (year = ? AND month = ? AND day <= ?))
        )
    """.trimIndent()
        val selectionArgs = arrayOf(
            country,
            startYear.toString(), startYear.toString(), startMonth.toString(), startYear.toString(), startMonth.toString(), startDay.toString(),
            endYear.toString(), endYear.toString(), endMonth.toString(), endYear.toString(), endMonth.toString(), endDay.toString()
        )
        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_HOLIDAY,
            null,
            selection,
            selectionArgs,
            null, null,
            "year ASC, month ASC, day ASC", // 가장 빠른 휴일 반환
            "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return HolidayItem(
                    year = it.getInt(it.getColumnIndexOrThrow("year")),
                    month = it.getInt(it.getColumnIndexOrThrow("month")),
                    day = it.getInt(it.getColumnIndexOrThrow("day")),
                    country = it.getString(it.getColumnIndexOrThrow("country")),
                    version = it.getInt(it.getColumnIndexOrThrow("version")),
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    en = it.getString(it.getColumnIndexOrThrow("en")),
                    ko = it.getString(it.getColumnIndexOrThrow("ko")),
                    ja = it.getString(it.getColumnIndexOrThrow("ja")),
                    zh = it.getString(it.getColumnIndexOrThrow("zh")),
                    fr = it.getString(it.getColumnIndexOrThrow("fr")),
                    de = it.getString(it.getColumnIndexOrThrow("de")),
                    it = it.getString(it.getColumnIndexOrThrow("it")),
                    th = it.getString(it.getColumnIndexOrThrow("th")),
                    es = it.getString(it.getColumnIndexOrThrow("es")),
                    regDate = it.getString(it.getColumnIndexOrThrow("reg_date"))
                )
            }
        }
        return null
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_ASSET_NAME, null, ComConstant.DATABASE_ASSET_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_HOLIDAY)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_ASSET_TABLE_HOLIDAY}")
            onCreate(db)
        }
    }
}
