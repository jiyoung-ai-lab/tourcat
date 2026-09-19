package com.waveapp.tourcat.database

import android.content.ContentValues.TAG
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.waveapp.tourcat.common.ComConstant


class CreateDbAdapter(private val context: Context) {

    companion object {
        private val DATABASE_CREATE_INSTALLCHECK = """
            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_INSTALLCHECK} (
                _id integer primary key autoincrement,
                complete text 
            );
        """.trimIndent()

//        //고객정보
//        private val DATABASE_CREATE_MEMBERSHIP = """
//            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_MEMBERSHIP} (
//                _id integer primary key autoincrement,
//                userkey text   ,
//                email text   ,
//                password text   ,
//                confirmdate text   ,
//                modifydate text
//            );
//        """.trimIndent()

//        private val DATABASE_CREATE_IDX1_MEMBERSHIP = """
//            create index IF NOT EXISTS ${ComConstant.DATABASE_INDEX1_MEMBERSHIP}
//                ON ${ComConstant.DATABASE_TABLE_MEMBERSHIP}
//                (userkey asc, email asc);
//        """.trimIndent()


        // 여행정보(TravelPlan) 테이블
        private val DATABASE_CREATE_TRAVEL_PLAN = """
            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_PLAN} (
                    _id integer primary key autoincrement,
                    startdate text ,
                    enddate text ,
                    nation text ,
                    city text ,
                    confirmdate text,
                    modifydate text
                );
                """.trimIndent()

        private val DATABASE_CREATE_TRAVEL_IMAGE  = """
            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE} (
                    _id integer primary key autoincrement,
                    travelid integer ,          
                    gubun text ,                 
                    url text ,                  
                    confirmdate text 
                );
                """.trimIndent()
            private val DATABASE_CREATE_IDX1_TRAVEL_IMAGE  = """
                create index IF NOT EXISTS ${ComConstant.DATABASE_INDEX1_TRAVEL_IMAGE}
                    ON ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE}
                    (travelid asc, gubun asc);
            """.trimIndent()

        // OCR/번역 결과 저장 테이블 생성 쿼리 (id, imagePath, ocrText, translatedText, langCode, createdAt)
        private val DATABASE_CREATE_OCR_TRANSLATION_RESULT = """
                create table IF NOT EXISTS  ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    image_path TEXT,
                    ocr_text TEXT,
                    translated_text TEXT,
                    lang_code TEXT,
                    created_at TEXT
                );
            """.trimIndent()
        //푸쉬 데이터 분류별로 담기
        private val DATABASE_CREATE_PUSHDATA = """
                create table IF NOT EXISTS  ${ComConstant.DATABASE_TABLE_PUSH} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT,
                    country TEXT,
                    city TEXT,
                    date TEXT,
                    content TEXT,
                    received_at TEXT
                );
            """.trimIndent()
        //푸쉬 공지
        private val DATABASE_CREATE_NOTICE = """
                create table IF NOT EXISTS  ${ComConstant.DATABASE_TABLE_NOTICE} (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT,
                    content TEXT,
                    date TEXT, 
                    country TEXT,
                    language TEXT,                     
                    received_at TEXT,                     
                    is_read INTEGER DEFAULT 0
                    
                );
            """.trimIndent()

        // GPT 검색 마스터 테이블
        private val DATABASE_CREATE_GPTSEARCH = """
                create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_GPT_SEARCH} (
                    _id integer primary key autoincrement,
                    user_uid text,
                    user_email text,
                    category text,
                    image_url text,
                    query_text text,
                    result_text text,
                    status text,
                    price integer,
                    requested_at text,
                    response_at text,
                    fail_reason text
                );
            """.trimIndent()


        // GPT 크레딧(과금) 테이블
        private val DATABASE_CREATE_GPT_CREDIT = """
            create table IF NOT EXISTS ${ComConstant.DATABASE_TABLE_GPT_CREDIT} (
                _id integer primary key autoincrement,
                gptsearch_id integer,
                user_uid text,
                user_email text,
                prev_credit integer,
                used_credit integer,
                final_credit integer,
                created_at text
            );
        """.trimIndent()

    }


    private var dbHelper: DatabaseHelper? = null
    private var db: SQLiteDatabase? = null

//
//    /** 반드시 Application context로 init! (ex: MyApp에서 한 번만) */
//    fun init(context: Context) {
//        if (!isInitialized) {
//            dbHelper = DatabaseHelper(context.applicationContext)
//            db = dbHelper?.writableDatabase
//            checkDBVersion()
//            isInitialized = true
//        }
//    }

//    fun getDb(): SQLiteDatabase? = db
    fun open(): CreateDbAdapter {
        dbHelper = DatabaseHelper(context)
        db = dbHelper?.writableDatabase
//        checkDBVersion()
        return this
    }
    fun close() {
        dbHelper?.close()
        dbHelper = null
        db = null
    }

//    private fun checkDBVersion() {

//        if (oldVersion < 2) {
//            // 예시: 2버전에서 컬럼 추가
//            try {
//                db.execSQL("ALTER TABLE ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION} ADD COLUMN lang_code TEXT;")
//            } catch (e: Exception) { /* 이미 추가된 경우 무시 */ }
//        }
//        if (oldVersion < 3) {
//            // 예시: 인덱스 추가 등
//            try {
//                db.execSQL(DATABASE_CREATE_IDX1_TRAVEL_IMAGE)
//            } catch (e: Exception) { }
//        }
//    }
    //테스트 전용(싱글턴이라 삭제시 close 처리)
    fun onCompletDeleteDatabase(context : Context) {
        Log.w(TAG, "DB DELETE START!!", null)

        close()
        val dbName = ComConstant.DATABASE_NAME
        val deleted = context.deleteDatabase(dbName)
        if (!deleted)   Log.d(TAG, "DB  DEL ERRER!")

    }
    // SQLiteOpenHelper

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(
        context,
        ComConstant.DATABASE_NAME, null, ComConstant.DATABASE_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            try {
                onCreateTable(db)
                onCreateIdx(db)
            } catch (e: SQLException) {
                Log.w(TAG, "DB CREATE ERR!!", e)
            }
        }

        private fun onCreateTable(db: SQLiteDatabase) {
            try {
                Log.w(TAG, "DB TABLE CREATE Start!!")
                db.execSQL(DATABASE_CREATE_INSTALLCHECK)
//                db.execSQL(DATABASE_CREATE_MEMBERSHIP)
                db.execSQL(DATABASE_CREATE_TRAVEL_PLAN)
                db.execSQL(DATABASE_CREATE_TRAVEL_IMAGE)
                db.execSQL(DATABASE_CREATE_OCR_TRANSLATION_RESULT)
                db.execSQL(DATABASE_CREATE_PUSHDATA)
                db.execSQL(DATABASE_CREATE_NOTICE)
                db.execSQL(DATABASE_CREATE_GPTSEARCH)
                db.execSQL(DATABASE_CREATE_GPT_CREDIT)


                Log.w(TAG, "DB TABLE CREATE End!!")
                // db.execSQL(다른 테이블 생성 SQL)
            } catch (e: SQLException) {
                Log.w(TAG, "DB TABLE CREATE ERR!!", e)
            }
        }

        private fun onCreateIdx(db: SQLiteDatabase) {
            try {
//                db.execSQL(DATABASE_CREATE_IDX1_MEMBERSHIP)
                db.execSQL(DATABASE_CREATE_IDX1_TRAVEL_IMAGE)
                // db.execSQL(다른 인덱스 생성 SQL)
            } catch (e: SQLException) {
                Log.w(TAG, "DB INDEX CREATE ERR!!", e)
            }
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            // 필요시 테이블/컬럼 추가 등 구현  -->TEMP
//            db.execSQL(DATABASE_CREATE_OCR_TRANSLATION_RESULT)
//            db.execSQL(DATABASE_CREATE_PUSHDATA)
//            db.execSQL(DATABASE_CREATE_NOTICE)

        }

        fun addTableColumn(db: SQLiteDatabase, tableName: String, columnName: String, type: String) {
            db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $type;")
        }

        fun updateJustOneRow(db: SQLiteDatabase, tableName: String, columnName: String, condition: String, value: String) {
            db.execSQL("UPDATE $tableName SET $columnName = '$value' WHERE $columnName = '$condition';")
        }

        fun onDropTable(db: SQLiteDatabase) {
//            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_MEMBERSHIP}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_PLAN}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_OCR_TRANSLATION}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_PUSH}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_NOTICE}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_GPT_SEARCH}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_GPT_CREDIT}")

        }

        fun onDropIndex(db: SQLiteDatabase) {
//            db.execSQL("DROP INDEX IF EXISTS ${ComConstant.DATABASE_INDEX1_MEMBERSHIP}")
            db.execSQL("DROP INDEX IF EXISTS ${ComConstant.DATABASE_INDEX1_TRAVEL_IMAGE}")
        }


    }
}