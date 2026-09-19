package com.waveapp.tourcat.database

import android.content.Context
import com.opencsv.CSVReader
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.ExchangeRateItem
import com.waveapp.tourcat.item.FestivalItem
import com.waveapp.tourcat.item.HolidayItem
import com.waveapp.tourcat.item.WeatherInfoItem
import com.waveapp.tourcat.util.DateTimeUtil
import java.io.InputStreamReader

/**
 * Asset CSV 데이터를 DB로 대량 적재 (트랜잭션 기반, regDate/version 관리)
 */
class AssetData {
    companion object {

        /**
         * [날씨 정보] CSV → DB 일괄 입력 (쉼표 등 특수문자 안전)
         */
        @JvmStatic
        fun importCityWeatherCsvToDb(context: Context, dbAdapter: CityWeatherAssetDbAdapter) {
            val assetManager = context.assets
            assetManager.open("city_weather.csv").use { inputStream ->
                CSVReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val allRows = reader.readAll()
                    val dataList = mutableListOf<WeatherInfoItem>()
                    for (i in 1 until allRows.size) {
                        val cols = allRows[i]
                        if (cols.size < 14) continue
                        dataList.add(
                            WeatherInfoItem(
                                city = cols[0].trim(),
                                month = cols[1].trim().toIntOrNull() ?: continue,
                                referenceYear = cols[2].trim().toIntOrNull() ?: continue,
                                tempAvg = cols[3].trim().toDoubleOrNull(),
                                version = cols[4].trim().toIntOrNull() ?: 1,
                                regDate = DateTimeUtil.nowUtcIsoMiliSeconds(),
                                en = cols[5].trim(),
                                ko = cols[6].trim(),
                                ja = cols[7].trim(),
                                zh = cols[8].trim(),
                                fr = cols[9].trim(),
                                de = cols[10].trim(),
                                it = cols[11].trim(),
                                th = cols[12].trim(),
                                es = cols[13].trim()
                            )
                        )
                    }
                    dbAdapter.insertWeatherList(dataList)
                }
            }
        }

        /**
         * [공휴일 정보] CSV → DB 일괄 입력
         */
        @JvmStatic
        fun importHolidayCsvToDb(context: Context, dbAdapter: HolidayAssetDbAdapter) {
            val assetManager = context.assets
            assetManager.open("holiday.csv").use { inputStream ->
                CSVReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val allRows = reader.readAll()
                    val dataList = mutableListOf<HolidayItem>()
                    for (i in 1 until allRows.size) {
                        val cols = allRows[i]
                        if (cols.size < 16) continue
                        dataList.add(
                            HolidayItem(
                                year = cols[0].trim().toIntOrNull() ?: continue,
                                month = cols[1].trim().toIntOrNull() ?: continue,
                                day = cols[2].trim().toIntOrNull() ?: continue,
                                country = cols[3].trim(),
                                version = cols[4].trim().toIntOrNull() ?: 1,
                                type = cols[5].trim(),
                                en = cols[6].trim(),
                                ko = cols[7].trim(),
                                ja = cols[8].trim(),
                                zh = cols[9].trim(),
                                fr = cols[10].trim(),
                                de = cols[11].trim(),
                                it = cols[12].trim(),
                                th = cols[13].trim(),
                                es = cols[14].trim(),
                                regDate = DateTimeUtil.nowUtcIsoMiliSeconds()
                            )
                        )
                    }
                    dbAdapter.insertHolidayList(dataList)
                }
            }
        }

        /**
         * [축제 정보] CSV → DB 일괄 입력
         */
        @JvmStatic
        fun importFestivalCsvToDb(context: Context, dbAdapter: FestivalAssetDbAdapter) {
            val assetManager = context.assets
            assetManager.open("festival.csv").use { inputStream ->
                CSVReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val allRows = reader.readAll()
                    val dataList = mutableListOf<FestivalItem>()
                    for (i in 1 until allRows.size) {
                        val cols = allRows[i]
                        if (cols.size < 15) continue
                        dataList.add(
                            FestivalItem(
                                country = cols[0].trim(),
                                city = cols[1].trim(),
                                year = cols[2].trim().toIntOrNull() ?: continue,
                                month = cols[3].trim().toIntOrNull() ?: continue,
                                day = cols[4].trim().toIntOrNull() ?: 1,
                                version = cols[5].trim().toIntOrNull() ?: 1,
                                en = cols[6].trim(),
                                ko = cols[7].trim(),
                                ja = cols[8].trim(),
                                zh = cols[9].trim(),
                                fr = cols[10].trim(),
                                de = cols[11].trim(),
                                it = cols[12].trim(),
                                th = cols[13].trim(),
                                es = cols[14].trim(),
                                regDate = DateTimeUtil.nowUtcIsoMiliSeconds()
                            )
                        )
                    }
                    dbAdapter.insertFestivalList(dataList)
                }
            }
        }
    }
}
