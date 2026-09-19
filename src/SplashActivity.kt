package com.waveapp.tourcat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.connection.assetdata.ExchangeRateRepository
import com.waveapp.tourcat.connection.assetdata.ExchangeRateSyncWorker
import com.waveapp.tourcat.database.AssetData
import com.waveapp.tourcat.database.CityWeatherAssetDbAdapter
import com.waveapp.tourcat.database.CreateAssetDbAdapter
import com.waveapp.tourcat.database.CreateDbAdapter
import com.waveapp.tourcat.database.FestivalAssetDbAdapter
import com.waveapp.tourcat.database.HolidayAssetDbAdapter
import com.waveapp.tourcat.database.NoticeDbAdapter
import com.waveapp.tourcat.helper.MLKitTranslatorModule
import com.waveapp.tourcat.helper.MessageHelper
import com.waveapp.tourcat.util.ComUtil
import com.waveapp.tourcat.util.LocaleUtil
import com.waveapp.tourcat.util.LogUtil
import com.waveapp.tourcat.util.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SplashActivity : AppCompatActivity() {

    val ctx = this

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(R.layout.activity_splash)

            lifecycleScope.launch {
                //초기화 작업과 splahs 작업 병렬처리 (둘중 오래걸리는 것 기준으로 다음 작업진행)
                val initJob  = async { initApp( ctx ) }
                val splashJob = async {
                    Handler(Looper.getMainLooper()).postDelayed({
                            //스플레쉬 작업동안 진행할 다른 것들(화면에 알림같은거)
                    }, 1000) // 초기화+애니메이션 시간
                }

                // 두 작업 중 더 오래 걸리는 쪽까지 기다림
                initJob.await()
                splashJob.await()

            }
    }

    /**
     *  앱 실행시 필수 처리사항 (최초 실행시 기본언어팩 다운로드, 권한동의 화면 전환 추가)
     */
    suspend fun initApp(context: Context ) {

            // Database 최초실행 (Db 생성 및 version update)
            val mDbCreate: CreateDbAdapter = CreateDbAdapter(ctx)
            mDbCreate.open()
            mDbCreate.close()

            val mAssetDbCreate: CreateAssetDbAdapter = CreateAssetDbAdapter(ctx)
            mAssetDbCreate.open()
            mAssetDbCreate.close()

            //최초 사용인지 확인(DB Create , Preference 값 참조, 초기 언어값 setting
            //언어값 setting ( prefs 값이 있으면 가져오고 아니면 local -> 앱 local 설정)
            val sharedPrefs = context.getSharedPreferences(ComConstant.PREF_NAME, MODE_PRIVATE)
            var language  = sharedPrefs.getString(ComConstant.PREF_KEYS.LANGUAGE, "")
            var nation = sharedPrefs.getString(ComConstant.PREF_KEYS.NATION, "")


            val sharedEditor = sharedPrefs.edit()
            val phoneLocale: String = ComUtil.geLocalLanguage()
            if (language == null || (language != null && language.trim { it <= ' ' } == "")) {
                language = phoneLocale
                sharedEditor.putString(ComConstant.PREF_KEYS.LANGUAGE, language.trim { it <= ' ' })
                sharedEditor.commit()
            }

            // 2) Nation 초기설정 : Pref 에 없으면 단말기 locale 정보로 setting
            val phoneNation: String = ComUtil.geLocalNation()
            if (nation == null || (nation != null && nation.trim { it <= ' ' } == "")) {
                nation = phoneNation
                sharedEditor.putString(ComConstant.PREF_KEYS.NATION, nation.trim { it <= ' ' })
                sharedEditor.commit()
            }
            ComConstant.USER_LANGUAGE_CODE = language
            ComConstant.USER_NATION_CODE = nation

            // 추가! 앱 내 LocaleHelper 동기화
            LocaleUtil.setLocale(language, nation)
            LocaleUtil.changeAppLanguage( this,language )


            // 앱 실행할때마다 파일 존재 확인 없으면 downlaod + parsing, 있으면 parsing
            ExchangeRateRepository.ensureJsonFile ( this )  { ok ->
                if (!ok) {
                    LogUtil.e( " ExchangeRateRepository : file load and parsing error !!"  )
                } else {
                    // 성공시 이후 로직
                }
            }

            // 최초 실행 로직 추가 ( 언어팩 일과 다운로드 처리 및 권한동의 화면으로 전환)
            val isFirstRun = sharedPrefs.getBoolean(ComConstant.PREF_KEYS.FIRST_LAUNCH, true)
            if ( isFirstRun == true) {

                //Asset DB 데이터 생성 -> TEMP (버전관리로직은 나중에)
                val dbAdapter = CityWeatherAssetDbAdapter(ctx).open()
                AssetData.importCityWeatherCsvToDb(ctx, dbAdapter)
                dbAdapter.close()

                // 공휴일(Holiday)
                val holidayDbAdapter = HolidayAssetDbAdapter(ctx).open()
                AssetData.importHolidayCsvToDb(ctx, holidayDbAdapter)
                holidayDbAdapter.close()

                // 축제/페스티벌(Festival)
                val festivalDbAdapter = FestivalAssetDbAdapter(ctx).open()
                AssetData.importFestivalCsvToDb(ctx, festivalDbAdapter)
                festivalDbAdapter.close()


                //네트워크 상황 체크
                MessageHelper.showAlert(
                    ctx,
                    title =ctx.getString(R.string.lang_pack),
                    message = ctx.getString(R.string.msg_quest_languagepack_download),
                    positiveText = ctx.getString(R.string.download),
                    negativeText = ctx.getString(R.string.later),
                    cancelable = false,
                    onPositive = {
                        if (NetworkUtil.isNetworkConnected(ctx)) {
                            //ML KIT 기본언어팩 다운로드(한국,중국,일본,영어 & 선택언어)
                            lifecycleScope.launch {
                                MLKitTranslatorModule.downloadMLkitLanguage(ComConstant.LOCALE_EN, ComConstant.LOCALE_KO)
                                MLKitTranslatorModule.downloadMLkitLanguage(ComConstant.LOCALE_JA, ComConstant.USER_LANGUAGE_CODE)
                            }
                            sharedPrefs.edit().putBoolean(ComConstant.PREF_KEYS.FIRST_LAUNCH, false).apply()
                            goPermissionAgree()
                        } else {
                            // 네트워크 안내 + 다음 화면 이동
                            MessageHelper.showToast(ctx, ctx.getString(R.string.msg_error_network_disconnected_unabledownload))
                            sharedPrefs.edit().putBoolean(ComConstant.PREF_KEYS.FIRST_LAUNCH, false).apply()
                            goPermissionAgree()
                        }
                    },
                    onNegative = {
                        sharedPrefs.edit().putBoolean(ComConstant.PREF_KEYS.FIRST_LAUNCH, false).apply()
                        goPermissionAgree()
                    }

                )

                val noticeList = loadNoticeJsonFromAssets(context)
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val db = NoticeDbAdapter(context).open()

                for (notice in noticeList) {
                    for (lang in listOf("ko", "en", "ja", "zh")) {
                        val (title, content) = notice.noticeMap[lang] ?: ("" to "")
                        db.insertNotice(
                            title = title,
                            content = content,
                            date = notice.date, // 기존 today에서 notice.date로 수정
                            country = "",
                            language = lang
                        )
                    }
                }
                db.close()

        } else {

                // 최초실행이 아니면 메인으로
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
        }
    }


    data class NoticeItem(
        val id: Int,
        val date: String,
        val noticeMap: Map<String, Pair<String, String>> // lang → (title, body)
    )
    fun loadNoticeJsonFromAssets(
        context: Context,
        fileName: String = "notices_welcome.json"
    ): List<NoticeItem> {
        val jsonStr = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val jsonArr = org.json.JSONArray(jsonStr)
        val result = mutableListOf<NoticeItem>()
        for (i in 0 until jsonArr.length()) {
            val obj = jsonArr.getJSONObject(i)
            val id = obj.optInt("id")
            val date = obj.optString("date")
            val noticesObj = obj.getJSONObject("notices")
            val langMap = mutableMapOf<String, Pair<String, String>>() // lang -> Pair<title, body>
            for (lang in listOf("ko", "en", "ja", "zh")) {
                val noticeLangObj = noticesObj.optJSONObject(lang)
                if (noticeLangObj != null) {
                    val title = noticeLangObj.optString("title", "")
                    val body = noticeLangObj.optString("body", "")
                    langMap[lang] = title to body
                } else {
                    langMap[lang] = "" to ""
                }
            }
            result.add(NoticeItem(id = id, date = date, noticeMap = langMap))
        }
        return result
    }

    private fun goPermissionAgree() {
        // 권한동의 Activity로 이동(뒤로가기 방지, finish() 필수)
        val intent = Intent(this, PermissionAgreeActivity::class.java)
        // 필요하면 추가 데이터 putExtra
        startActivity(intent)
        finish()
    }
}