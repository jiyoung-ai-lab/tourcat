package com.waveapp.tourcat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.CityList
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.common.NationList
import com.waveapp.tourcat.item.FeedItem
import com.waveapp.tourcat.item.StoryItem
import com.waveapp.tourcat.item.StoryType
import com.waveapp.tourcat.util.DateTimeUtil
import com.waveapp.tourcat.util.LocaleUtil
import com.waveapp.tourcat.util.LogUtil

class TravelPlanDbAdapter(private val context: Context) {

    private var dbHelper: DatabaseHelper? = null
    var db: SQLiteDatabase? = null

    companion object {
        private const val TAG = "TravelPlanDbAdapter"

        // 여행계획(Plan)
        const val KEY_ID = "_id"
        const val KEY_STARTDATE = "startdate"
        const val KEY_ENDDATE = "enddate"
        const val KEY_NATION = "nation"
        const val KEY_CITY = "city"
        const val KEY_CONFIRMDATE = "confirmdate"
        const val KEY_MODIFYDATE = "modifydate"

        // 이미지(Image)
        const val KEY_IMAGE_ID = "_id"
        const val KEY_TRAVELID = "travelid"
        const val KEY_GUBUN = "gubun"
        const val KEY_URL = "url"
        const val KEY_IMAGE_CONFIRMDATE = "confirmdate"

        private const val DATABASE_CREATE_TRAVEL_PLAN = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_PLAN} (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_STARTDATE TEXT,
                $KEY_ENDDATE TEXT,
                $KEY_NATION TEXT,
                $KEY_CITY TEXT,
                $KEY_CONFIRMDATE TEXT,
                $KEY_MODIFYDATE TEXT
            );
        """

        private const val DATABASE_CREATE_TRAVEL_IMAGE = """
            CREATE TABLE IF NOT EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE} (
                $KEY_IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TRAVELID INTEGER,
                $KEY_GUBUN TEXT,
                $KEY_URL TEXT,
                $KEY_IMAGE_CONFIRMDATE TEXT
            );
        """
    }

    fun open(): TravelPlanDbAdapter {
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

    fun beginTransaction() {
        db?.beginTransaction()
    }

    fun setTransactionSuccessful() {
        db?.setTransactionSuccessful()
    }

    fun endTransaction() {
        db?.endTransaction()
    }

    // --- 내부 DB Helper ---
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(
            context,
            ComConstant.DATABASE_NAME,
            null,
            ComConstant.DATABASE_VERSION
        ) {

        override fun onCreate(db: SQLiteDatabase) {
            LogUtil.w(TAG, ">>>>>> DB CREATE Start!! : $DATABASE_CREATE_TRAVEL_PLAN")
            db.execSQL(DATABASE_CREATE_TRAVEL_PLAN)
            db.execSQL(DATABASE_CREATE_TRAVEL_IMAGE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_PLAN}")
            db.execSQL("DROP TABLE IF EXISTS ${ComConstant.DATABASE_TABLE_TRAVEL_IMAGE}")
            onCreate(db)
        }
    }

    // -------------------------------
    // 1. 여행계획(Plan) 관련
    // -------------------------------
    fun insertTravelPlan(
        startdate: String, enddate: String, nation: String, city: String,
        confirmdate: String, modifydate: String
    ): Long {
        val values = ContentValues().apply {
            put(KEY_STARTDATE, startdate)
            put(KEY_ENDDATE, enddate)
            put(KEY_NATION, nation)
            put(KEY_CITY, city)
            put(KEY_CONFIRMDATE, confirmdate)
            put(KEY_MODIFYDATE, modifydate)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_TRAVEL_PLAN, null, values) ?: -1
    }

    fun deleteTravelPlanComplete(_id: String): Boolean {
        val whereClause = "$KEY_ID = ?"
        val whereArgs = arrayOf(_id)
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_PLAN, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }

    fun getTravelPlanById(_id: String): Cursor? {
        return db?.query(
            ComConstant.DATABASE_TABLE_TRAVEL_PLAN,
            null,
            "$KEY_ID = ?",
            arrayOf(_id),
            null, null, null
        )
    }

    // -------------------------------
    // 2. 이미지(Image) 관련
    // -------------------------------
    fun insertImage(
        travelId: Long, gubun: String, url: String, confirmDate: String
    ): Long {
        val values = ContentValues().apply {
            put(KEY_TRAVELID, travelId)
            put(KEY_GUBUN, gubun)
            put(KEY_URL, url)
            put(KEY_IMAGE_CONFIRMDATE, confirmDate)
        }
        return db?.insert(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, null, values) ?: -1
    }
    fun updateTravelPlan(
        id: Long,
        startdate: String, enddate: String,
        nation: String, city: String,
        confirmdate: String, modifydate: String
    ): Int {
        val values = ContentValues().apply {
            put(KEY_STARTDATE, startdate)
            put(KEY_ENDDATE, enddate)
            put(KEY_NATION, nation)
            put(KEY_CITY, city)
            put(KEY_CONFIRMDATE, confirmdate)
            put(KEY_MODIFYDATE, modifydate)
        }
        return db?.update(
            ComConstant.DATABASE_TABLE_TRAVEL_PLAN,
            values, "$KEY_ID = ?", arrayOf(id.toString())
        ) ?: 0
    }
    fun deleteTravelPlanById(id: Long): Boolean {
        val whereClause = "$KEY_ID = ?"
        val whereArgs = arrayOf(id.toString())
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_PLAN, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }

    fun getImagesByTravelId(travelId: Long, gubun: String? = null): Cursor? {
        val selection: String
        val selectionArgs: Array<String>
        if (gubun != null) {
            selection = "$KEY_TRAVELID = ? AND $KEY_GUBUN = ?"
            selectionArgs = arrayOf(travelId.toString(), gubun)
        } else {
            selection = "$KEY_TRAVELID = ?"
            selectionArgs = arrayOf(travelId.toString())
        }
        return db?.query(
            ComConstant.DATABASE_TABLE_TRAVEL_IMAGE,
            null,
            selection,
            selectionArgs,
            null, null, null
        )
    }
    fun deleteImageByPath(travelId: Long, gubun: String, url: String): Boolean {
        val whereClause = "$KEY_TRAVELID = ? AND $KEY_GUBUN = ? AND $KEY_URL = ?"
        val whereArgs = arrayOf(travelId.toString(), gubun, url)
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }
    fun deleteImageById(imageId: Long): Boolean {
        val whereClause = "$KEY_IMAGE_ID = ?"
        val whereArgs = arrayOf(imageId.toString())
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }

    fun deleteImagesByTravelId(travelId: Long): Boolean {
        val whereClause = "$KEY_TRAVELID = ?"
        val whereArgs = arrayOf(travelId.toString())
        val deletedRows = db?.delete(ComConstant.DATABASE_TABLE_TRAVEL_IMAGE, whereClause, whereArgs) ?: 0
        return deletedRows > 0
    }

    /** [홈 화면 등에서 사용] 여행 스토리 리스트 반환 (플러스 버튼 없이 TRAVEL 정보만!) */
    fun getTravelStoryList(): List<StoryItem> {
        val storyList = mutableListOf<StoryItem>()
        val dbAdapter = TravelPlanDbAdapter(context).open()
        val cursor = dbAdapter.db?.query(
            ComConstant.DATABASE_TABLE_TRAVEL_PLAN,
            arrayOf(
                TravelPlanDbAdapter.KEY_ID,
                TravelPlanDbAdapter.KEY_STARTDATE,
                TravelPlanDbAdapter.KEY_ENDDATE,
                TravelPlanDbAdapter.KEY_NATION,
                TravelPlanDbAdapter.KEY_CITY
            ),
            null, null, null, null,
            "${TravelPlanDbAdapter.KEY_STARTDATE} ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val planId = it.getLong(it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_ID))
                val startDate = it.getString(it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_STARTDATE))
                val endDate = it.getString(it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_ENDDATE))
                val nation = it.getString(it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_NATION))
                val city = it.getString(it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_CITY))

                storyList.add(
                    StoryItem(
                        type = StoryType.TRAVEL,
                        startDate = startDate,
                        endDate = endDate,
                        nation = nation ?: "",
                        city = city ?: "",
                        imageResId = R.drawable.ic_placeholder,
                        planId = planId
                    )
                )
            }
        }
        dbAdapter.close()
        return storyList
    }

    fun getTravelFeedList(): List<FeedItem> {
        val feedList = mutableListOf<FeedItem>()
        val dbAdapter = TravelPlanDbAdapter(context).open()
        val cursor = dbAdapter.db?.query(
            ComConstant.DATABASE_TABLE_TRAVEL_PLAN,
            arrayOf(
                TravelPlanDbAdapter.KEY_ID,
                TravelPlanDbAdapter.KEY_STARTDATE,
                TravelPlanDbAdapter.KEY_ENDDATE,
                TravelPlanDbAdapter.KEY_NATION,
                TravelPlanDbAdapter.KEY_CITY
            ),
            "${TravelPlanDbAdapter.KEY_ENDDATE} >= ?",  // 종료일이 오늘보다 작은 것만!
            arrayOf(DateTimeUtil.getTodayCompact()),
            null, null,
            "${TravelPlanDbAdapter.KEY_STARTDATE} ASC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_ID)
            val startIdx = it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_STARTDATE)
            val endIdx = it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_ENDDATE)
            val nationIdx = it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_NATION)
            val cityIdx = it.getColumnIndexOrThrow(TravelPlanDbAdapter.KEY_CITY)
            while (it.moveToNext()) {
                val planId = it.getLong(idIdx)
                val startDate = it.getString(startIdx)
                val endDate = it.getString(endIdx)
                val nation = it.getString(nationIdx)
                val city = it.getString(cityIdx)

                val cityName = CityList.getCityName(city, ComConstant.USER_LANGUAGE_CODE)
                val nationName = NationList.findName(nation, ComConstant.USER_LANGUAGE_CODE)
                val period = LocaleUtil.formatPeriod(startDate, endDate)

                feedList.add(
                    FeedItem(
                        imageResId = R.drawable.ic_placeholder, // 대표 이미지
                        title = "$cityName, $nationName",
                        subtitle = period,
                        planId = planId,
                        startDate = startDate,
                        endDate = endDate,
                        country = nation,
                        city = city,
                        period = period
                    )
                )
            }
        }
        dbAdapter.close()
        return feedList
    }

}
