package com.waveapp.tourcat.database

import android.content.ContentValues.TAG
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.waveapp.tourcat.common.ComConstant

class CreateAssetDbAdapter(private val context: Context) {

    companion object {
        private val DATABASE_CREATE_INSTALLCHECK = """
            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_INSTALLCHECK} (
                _id integer primary key autoincrement,
                complete text 
            );
        """.trimIndent()

        // city_weather 테이블 생성 쿼리 (reg_date, version 포함, 컬럼명과 순서 최신 레이아웃 기준)
        private const val DATABASE_CREATE_CITY_WEATHER = """
                CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_CITY_WEATHER} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    city TEXT NOT NULL,
                    month INTEGER NOT NULL,
                    reference_year INTEGER NOT NULL,
                    temp_avg REAL,
                    version INTEGER NOT NULL,
                    reg_date TEXT,
                    en TEXT,
                    ko TEXT,
                    ja TEXT,
                    zh TEXT,
                    fr TEXT,
                    de TEXT,
                    it TEXT,
                    th TEXT,
                    es TEXT,
                    UNIQUE(city, month, reference_year, version)
                );
            """

        // holiday 테이블 생성 쿼리 (version, reg_date 포함)
        private const val DATABASE_CREATE_HOLIDAY = """
                CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_HOLIDAY} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    day INTEGER NOT NULL,
                    country TEXT NOT NULL,
                    version INTEGER,
                    type TEXT,
                    en TEXT,
                    ko TEXT,
                    ja TEXT,
                    zh TEXT,
                    fr TEXT,
                    de TEXT,
                    it TEXT,
                    th TEXT,
                    es TEXT,
                    reg_date TEXT,
                    UNIQUE(year, month, day, country, version)
                );
            """

        // festival 테이블 생성 쿼리 (version, reg_date 포함)
        private const val DATABASE_CREATE_FESTIVAL = """
                CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_FESTIVAL} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country TEXT NOT NULL,
                    city TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    day INTEGER NOT NULL,
                    version INTEGER,
                    en TEXT,
                    ko TEXT,
                    ja TEXT,
                    zh TEXT,
                    fr TEXT,
                    de TEXT,
                    it TEXT,
                    th TEXT,
                    es TEXT,
                    reg_date TEXT,
                    UNIQUE(country, city, month, version)
                );
            """

//        // exchange_rate 테이블 생성 쿼리 (ref_date, version, reg_date 포함)
//        private const val DATABASE_CREATE_EXCHANGE_RATE = """
//                CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_ASSET_TABLE_EXCHANGE_RATE} (
//                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
//                    base_currency TEXT NOT NULL,
//                    base_country_code TEXT NOT NULL,
//                    target_currency TEXT NOT NULL,
//                    target_country_code TEXT NOT NULL,
//                    target_symbol TEXT,
//                    rate REAL NOT NULL,
//                    ref_date TEXT NOT NULL,
//                    version INTEGER,
//                    reg_date TEXT,
//                    UNIQUE(base_currency, base_country_code, target_currency, target_country_code,  version)
//                );
//            """

    }

    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

    fun open(): CreateAssetDbAdapter {
        dbHelper = DatabaseHelper(context)
        db = dbHelper?.writableDatabase
        return this
    }
    fun close() {
        dbHelper?.close()
        dbHelper = null
        db = null
    }

    fun onCompletDeleteDatabase(context: Context) {
        Log.w(TAG, "DB ASSET DELETE START!!", null)
        close()
        val dbName = ComConstant.DATABASE_ASSET_NAME
        val deleted = context.deleteDatabase(dbName)
        if (!deleted)   Log.d(TAG, "DB  ASSET DEL ERROR!")
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(
        context,
        ComConstant.DATABASE_ASSET_NAME, null, ComConstant.DATABASE_ASSET_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            try {
                onCreateTable(db)
                onCreateIdx(db)
            } catch (e: SQLException) {
                Log.w(TAG, "DB ASSET CREATE ERR!!", e)
            }
        }

        private fun onCreateTable(db: SQLiteDatabase) {
            try {
                Log.w(TAG, "DB  ASSET TABLE CREATE Start!!")
                db.execSQL(DATABASE_CREATE_INSTALLCHECK)
                db.execSQL(DATABASE_CREATE_CITY_WEATHER)
                db.execSQL(DATABASE_CREATE_HOLIDAY)
                db.execSQL(DATABASE_CREATE_FESTIVAL)
//                db.execSQL(DATABASE_CREATE_EXCHANGE_RATE)
                Log.w(TAG, "DB ASSET TABLE CREATE End!!")
            } catch (e: SQLException) {
                Log.w(TAG, "DB ASSET TABLE CREATE ERR!!", e)
            }
        }

        private fun onCreateIdx(db: SQLiteDatabase) {
            // 인덱스 필요시 여기에 추가
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            // 필요시 테이블/컬럼 추가 구현
        }
    }
}
