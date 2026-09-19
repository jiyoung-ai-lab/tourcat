package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getDoubleOrNull
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.WeatherInfoItem

class CityWeatherAssetDbAdapter(private val context: Context) {

    companion object {
        // 필드명 상수
        const val KEY_ID = "_id"
        const val KEY_CITY = "city"
        const val KEY_MONTH = "month"
        const val KEY_REFERENCE_YEAR = "reference_year"
        const val KEY_TEMP_AVG = "temp_avg"
        const val KEY_VERSION = "version"
        const val KEY_REG_DATE = "reg_date"
        const val KEY_EN = "en"
        const val KEY_KO = "ko"
        const val KEY_JA = "ja"
        const val KEY_ZH = "zh"
        const val KEY_FR = "fr"
        const val KEY_DE = "de"
        const val KEY_IT = "it"
        const val KEY_TH = "th"
        const val KEY_ES = "es"

        // 테이블 생성 쿼리
        private const val DATABASE_CREATE_CITY_WEATHER = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_CITY TEXT NOT NULL,
                $KEY_MONTH INTEGER NOT NULL,
                $KEY_REFERENCE_YEAR INTEGER NOT NULL,
                $KEY_TEMP_AVG REAL,
                $KEY_VERSION INTEGER,
                $KEY_REG_DATE TEXT,
                $KEY_EN TEXT,
                $KEY_KO TEXT,
                $KEY_JA TEXT,
                $KEY_ZH TEXT,
                $KEY_FR TEXT,
                $KEY_DE TEXT,
                $KEY_IT TEXT,
                $KEY_TH TEXT,
                $KEY_ES TEXT,
                UNIQUE($KEY_CITY, $KEY_MONTH, $KEY_REFERENCE_YEAR, $KEY_VERSION)
            );
        """
    }

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    // DB 열기/닫기
    fun open(): CityWeatherAssetDbAdapter {
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

    // INSERT OR REPLACE
    fun insertWeather(
        city: String,
        month: Int,
        referenceYear: Int,
        tempAvg: Double?,
        version: Int,
        regDate: String,
        en: String?, ko: String?, ja: String?, zh: String?, fr: String?, de: String?, it: String?, th: String?, es: String?
    ): Long {
        val values = ContentValues().apply {
            put(KEY_CITY, city)
            put(KEY_MONTH, month)
            put(KEY_REFERENCE_YEAR, referenceYear)
            put(KEY_TEMP_AVG, tempAvg)
            put(KEY_VERSION, version)
            put(KEY_REG_DATE, regDate)
            put(KEY_EN, en)
            put(KEY_KO, ko)
            put(KEY_JA, ja)
            put(KEY_ZH, zh)
            put(KEY_FR, fr)
            put(KEY_DE, de)
            put(KEY_IT, it)
            put(KEY_TH, th)
            put(KEY_ES, es)
        }
        return db?.insertWithOnConflict(
            ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER, null, values, SQLiteDatabase.CONFLICT_REPLACE
        ) ?: -1
    }

    // 대량 insert (트랜잭션)
    fun insertWeatherList(list: List<WeatherInfoItem>): Int {
        var count = 0
        db?.beginTransaction()
        try {
            list.forEach {
                insertWeather(
                    city = it.city,
                    month = it.month,
                    referenceYear = it.referenceYear,
                    tempAvg = it.tempAvg,
                    version = it.version,
                    regDate = it.regDate,
                    en = it.en, ko = it.ko, ja = it.ja, zh = it.zh, fr = it.fr, de = it.de, it = it.it, th = it.th, es = it.es
                )
                count++
            }
            db?.setTransactionSuccessful()
        } finally {
            db?.endTransaction()
        }
        return count
    }

    // 조회 함수 등 (원하는 컬럼에 맞게 수정)
    fun getWeather(city: String, month: Int, referenceYear: Int, version: Int): Cursor? {
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER,
            null,
            "$KEY_CITY=? AND $KEY_MONTH=? AND $KEY_REFERENCE_YEAR=? AND $KEY_VERSION=?",
            arrayOf(city, month.toString(), referenceYear.toString(), version.toString()),
            null, null, null, "1"
        )
    }

    fun getAllWeather(version: Int? = null): Cursor? {
        val selection = version?.let { "$KEY_VERSION=?" }
        val selectionArgs = version?.let { arrayOf(it.toString()) }
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER,
            null, selection, selectionArgs, null, null,
            "$KEY_CITY ASC, $KEY_REFERENCE_YEAR ASC, $KEY_MONTH ASC"
        )
    }
    // CityWeatherAssetDbAdapter 안에 추가!
    fun getWeatherInfo(city: String, month: Int): WeatherInfoItem? {
        // 최신 version, 최신 referenceYear 우선 조회(필요시 ORDER BY 절 조정)
        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER,
            null,
            "city=? AND month=?",
            arrayOf(city, month.toString()),
            null, null,
            "version DESC, reference_year DESC",
            "1" // LIMIT 1
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return WeatherInfoItem(
                    city = it.getString(it.getColumnIndexOrThrow("city")),
                    month = it.getInt(it.getColumnIndexOrThrow("month")),
                    referenceYear = it.getInt(it.getColumnIndexOrThrow("reference_year")),
                    tempAvg = it.getDoubleOrNull(it.getColumnIndexOrThrow("temp_avg")),
                    version = it.getInt(it.getColumnIndexOrThrow("version")),
                    regDate = it.getString(it.getColumnIndexOrThrow("reg_date")),
                    en = it.getStringOrNull("en"),
                    ko = it.getStringOrNull("ko"),
                    ja = it.getStringOrNull("ja"),
                    zh = it.getStringOrNull("zh"),
                    fr = it.getStringOrNull("fr"),
                    de = it.getStringOrNull("de"),
                    it = it.getStringOrNull("it"),
                    th = it.getStringOrNull("th"),
                    es = it.getStringOrNull("es")
                )
            }
        }
        return null
    }
    fun Cursor.getDoubleOrNull(columnName: String): Double? =
        if (isNull(getColumnIndexOrThrow(columnName))) null else getDouble(getColumnIndexOrThrow(columnName))

    fun Cursor.getStringOrNull(columnName: String): String? =
        if (isNull(getColumnIndexOrThrow(columnName))) null else getString(getColumnIndexOrThrow(columnName))

    fun clearAllWeather(): Int {
        return db?.delete(ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER, null, null) ?: 0
    }

    // SQLiteOpenHelper
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_ASSET_NAME, null, ComConstant.DATABASE_ASSET_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_CITY_WEATHER)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER}")
            onCreate(db)
        }
    }
}
