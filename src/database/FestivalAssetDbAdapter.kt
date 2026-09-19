package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.FestivalItem

class FestivalAssetDbAdapter(private val context: Context) {

    companion object {
        private const val KEY_ID = "_id"
        private const val KEY_COUNTRY = "country"
        private const val KEY_CITY = "city"
        private const val KEY_YEAR = "year"
        private const val KEY_MONTH = "month"
        private const val KEY_DAY = "day"
        private const val KEY_VERSION = "version"
        private const val KEY_EN = "en"
        private const val KEY_KO = "ko"
        private const val KEY_JA = "ja"
        private const val KEY_ZH = "zh"
        private const val KEY_FR = "fr"
        private const val KEY_DE = "de"
        private const val KEY_IT = "it"
        private const val KEY_TH = "th"
        private const val KEY_ES = "es"
        private const val KEY_REG_DATE = "reg_date"

        private const val DATABASE_CREATE_FESTIVAL = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_FESTIVAL} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_COUNTRY TEXT NOT NULL,
                $KEY_CITY TEXT NOT NULL,
                $KEY_YEAR INTEGER NOT NULL,
                $KEY_MONTH INTEGER NOT NULL,
                $KEY_DAY INTEGER,
                $KEY_VERSION INTEGER,
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
                UNIQUE($KEY_COUNTRY, $KEY_CITY, $KEY_YEAR, $KEY_MONTH, $KEY_DAY, $KEY_VERSION)
            );
        """
    }

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    fun open(): FestivalAssetDbAdapter {
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

    fun insertFestival(item: FestivalItem): Long {
        val values = ContentValues().apply {
            put(KEY_COUNTRY, item.country)
            put(KEY_CITY, item.city)
            put(KEY_YEAR, item.year)
            put(KEY_MONTH, item.month)
            put(KEY_DAY, item.day)
            put(KEY_VERSION, item.version)
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
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL, null, values, SQLiteDatabase.CONFLICT_REPLACE
        ) ?: -1
    }

    fun insertFestivalList(items: List<FestivalItem>) {
        if (items.isEmpty()) return
        db?.beginTransaction()
        try {
            items.forEach { insertFestival(it) }
            db?.setTransactionSuccessful()
        } finally {
            db?.endTransaction()
        }
    }

    fun getFestival(country: String, city: String, year: Int, month: Int): Cursor? {
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL,
            null,
            "$KEY_COUNTRY=? AND $KEY_CITY=? AND $KEY_YEAR=? AND $KEY_MONTH=?",
            arrayOf(country, city, year.toString(), month.toString()),
            null, null, null
        )
    }

    fun getAllFestivals(): Cursor? {
        return db?.query(
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL,
            null, null, null, null, null,
            "$KEY_COUNTRY ASC, $KEY_CITY ASC, $KEY_YEAR ASC, $KEY_MONTH ASC, $KEY_DAY ASC"
        )
    }

    fun getFestivalInfo(country: String, city: String, year: Int, month: Int): FestivalItem? {
        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL,
            null,
            "$KEY_COUNTRY=? AND $KEY_CITY=? AND $KEY_YEAR=? AND $KEY_MONTH=?",
            arrayOf(country, city, year.toString(), month.toString()),
            null, null, null, "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return FestivalItem(
                    country = it.getString(it.getColumnIndexOrThrow(KEY_COUNTRY)),
                    city = it.getString(it.getColumnIndexOrThrow(KEY_CITY)),
                    year = it.getInt(it.getColumnIndexOrThrow(KEY_YEAR)),
                    month = it.getInt(it.getColumnIndexOrThrow(KEY_MONTH)),
                    day = it.getInt(it.getColumnIndexOrThrow(KEY_DAY)),
                    version = it.getInt(it.getColumnIndexOrThrow(KEY_VERSION)),
                    en = it.getString(it.getColumnIndexOrThrow(KEY_EN)),
                    ko = it.getString(it.getColumnIndexOrThrow(KEY_KO)),
                    ja = it.getString(it.getColumnIndexOrThrow(KEY_JA)),
                    zh = it.getString(it.getColumnIndexOrThrow(KEY_ZH)),
                    fr = it.getString(it.getColumnIndexOrThrow(KEY_FR)),
                    de = it.getString(it.getColumnIndexOrThrow(KEY_DE)),
                    it = it.getString(it.getColumnIndexOrThrow(KEY_IT)),
                    th = it.getString(it.getColumnIndexOrThrow(KEY_TH)),
                    es = it.getString(it.getColumnIndexOrThrow(KEY_ES)),
                    regDate = it.getString(it.getColumnIndexOrThrow(KEY_REG_DATE))
                )
            }
        }
        return null
    }

    fun getFestivalInfoRange(country: String, city: String, startYear: Int, startMonth: Int, endYear: Int, endMonth: Int): FestivalItem? {
        fun getYearMonthPairs(startYear: Int, startMonth: Int, endYear: Int, endMonth: Int): List<Pair<Int, Int>> {
            val list = mutableListOf<Pair<Int, Int>>()
            var y = startYear
            var m = startMonth
            while (y < endYear || (y == endYear && m <= endMonth)) {
                list.add(y to m)
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
            }
            return list
        }

        val ymList = getYearMonthPairs(startYear, startMonth, endYear, endMonth)
        val orList = ymList.map { "(year=? AND month=?)" }
        val selection = "$KEY_COUNTRY=? AND $KEY_CITY=? AND (${orList.joinToString(" OR ")})"
        val selectionArgs = mutableListOf<String>().apply {
            add(country)
            add(city)
            ymList.forEach {
                add(it.first.toString())
                add(it.second.toString())
            }
        }

        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL,
            null,
            selection,
            selectionArgs.toTypedArray(),
            null, null,
            "$KEY_YEAR ASC, $KEY_MONTH ASC, $KEY_DAY ASC",
            "1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return FestivalItem(
                    country = it.getString(it.getColumnIndexOrThrow(KEY_COUNTRY)),
                    city = it.getString(it.getColumnIndexOrThrow(KEY_CITY)),
                    year = it.getInt(it.getColumnIndexOrThrow(KEY_YEAR)),
                    month = it.getInt(it.getColumnIndexOrThrow(KEY_MONTH)),
                    day = it.getInt(it.getColumnIndexOrThrow(KEY_DAY)),
                    version = it.getInt(it.getColumnIndexOrThrow(KEY_VERSION)),
                    en = it.getString(it.getColumnIndexOrThrow(KEY_EN)),
                    ko = it.getString(it.getColumnIndexOrThrow(KEY_KO)),
                    ja = it.getString(it.getColumnIndexOrThrow(KEY_JA)),
                    zh = it.getString(it.getColumnIndexOrThrow(KEY_ZH)),
                    fr = it.getString(it.getColumnIndexOrThrow(KEY_FR)),
                    de = it.getString(it.getColumnIndexOrThrow(KEY_DE)),
                    it = it.getString(it.getColumnIndexOrThrow(KEY_IT)),
                    th = it.getString(it.getColumnIndexOrThrow(KEY_TH)),
                    es = it.getString(it.getColumnIndexOrThrow(KEY_ES)),
                    regDate = it.getString(it.getColumnIndexOrThrow(KEY_REG_DATE))
                )
            }
        }
        return null
    }

    fun getFestivalInfoByDateRange(
        country: String,
        city: String,
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int
    ): FestivalItem? {
        val cursor = db?.query(
            ComConstant.DATABASE_ASSET_TABLE_FESTIVAL,
            null,
            "$KEY_COUNTRY=? AND $KEY_CITY=? AND (strftime('%Y-%m-%d', printf('%04d-%02d-%02d', $KEY_YEAR, $KEY_MONTH, ifnull($KEY_DAY, 1))) BETWEEN ? AND ?)",
            arrayOf(
                country,
                city,
                String.format("%04d-%02d-%02d", startYear, startMonth, startDay),
                String.format("%04d-%02d-%02d", endYear, endMonth, endDay)
            ),
            null, null,
            "$KEY_YEAR ASC, $KEY_MONTH ASC, $KEY_DAY ASC",
            "1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return FestivalItem(
                    country = it.getString(it.getColumnIndexOrThrow(KEY_COUNTRY)),
                    city = it.getString(it.getColumnIndexOrThrow(KEY_CITY)),
                    year = it.getInt(it.getColumnIndexOrThrow(KEY_YEAR)),
                    month = it.getInt(it.getColumnIndexOrThrow(KEY_MONTH)),
                    day = it.getInt(it.getColumnIndexOrThrow(KEY_DAY)),
                    version = it.getInt(it.getColumnIndexOrThrow(KEY_VERSION)),
                    en = it.getString(it.getColumnIndexOrThrow(KEY_EN)),
                    ko = it.getString(it.getColumnIndexOrThrow(KEY_KO)),
                    ja = it.getString(it.getColumnIndexOrThrow(KEY_JA)),
                    zh = it.getString(it.getColumnIndexOrThrow(KEY_ZH)),
                    fr = it.getString(it.getColumnIndexOrThrow(KEY_FR)),
                    de = it.getString(it.getColumnIndexOrThrow(KEY_DE)),
                    it = it.getString(it.getColumnIndexOrThrow(KEY_IT)),
                    th = it.getString(it.getColumnIndexOrThrow(KEY_TH)),
                    es = it.getString(it.getColumnIndexOrThrow(KEY_ES)),
                    regDate = it.getString(it.getColumnIndexOrThrow(KEY_REG_DATE))
                )
            }
        }
        return null
    }

    fun clearAllFestivals(): Int {
        return db?.delete(ComConstant.DATABASE_ASSET_TABLE_FESTIVAL, null, null) ?: 0
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, ComConstant.DATABASE_ASSET_NAME, null, ComConstant.DATABASE_ASSET_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_FESTIVAL)
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_ASSET_TABLE_FESTIVAL}")
            onCreate(db)
        }
    }

    private fun Cursor.getIntOrNull(column: String): Int? =
        if (isNull(getColumnIndexOrThrow(column))) null else getInt(getColumnIndexOrThrow(column))
}
