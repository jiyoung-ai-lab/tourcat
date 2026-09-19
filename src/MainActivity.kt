package com.waveapp.tourcat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.design.FontScaleManager
import com.waveapp.tourcat.helper.TokenManager
import com.waveapp.tourcat.helper.UserLocationHelper
import com.waveapp.tourcat.util.EnvironmentUtil
import com.waveapp.tourcat.util.FontScaleStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var adContainer: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//
//        val prefs = getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
//        when (prefs.getString(ComConstant.PREF_KEYS.SETTINGS_THEME, "light")) {
//            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//            else   -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//        }

        // Material3 Toolbar 참조
        toolbar = findViewById(R.id.topAppBar)
        adContainer = findViewById(R.id.adContainer)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]


        // 위치정보 갱신
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        UserLocationHelper.updateLocation(this, fusedLocationClient)

        // 네비게이션 컨트롤러 연결
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 목적지에 따라 AppBar 및 버튼 가시성, subtitle 제어
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    toolbar.visibility = View.VISIBLE
                    toolbar.title = getString(R.string.app_name)
                    toolbar.subtitle = "" // 메인에선 서브타이틀 없음
                    toolbar.navigationIcon = null

                    setMenuItemVisible(R.id.action_coin, true)
                    setMenuItemVisible(R.id.action_notification, true)
                    setMenuItemVisible(R.id.action_settings, true)
                    adContainer?.visibility = View.VISIBLE
                }
                else -> {
                    toolbar.visibility = View.VISIBLE
                    toolbar.title =  ""
                    toolbar.subtitle = getSubtitleForDestination(destination.id)
                    toolbar.setNavigationIcon(R.drawable.ic_left_arrow)

                    setMenuItemVisible(R.id.action_coin, false)
                    setMenuItemVisible(R.id.action_notification, false)
                    setMenuItemVisible(R.id.action_settings, false)
                    adContainer?.visibility = View.GONE
                }
            }
        }

        // 메뉴 클릭 이벤트 (코인/알림/설정: actionLayout 기반으로 변경)
        setCustomMenuClickListeners()
        // 뒤로가기(네비게이션) 버튼 이벤트
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPrefs = newBase.getSharedPreferences(ComConstant.PREF_NAME, MODE_PRIVATE)
        var fontScale  = sharedPrefs.getFloat(ComConstant.PREF_KEYS.FONT_SCALE, 1.0f)
        fontScale = fontScale.coerceIn(0.90f, 1.40f) // ← 기존 0.8~1.8 → 0.90~1.40로
        val conf = newBase.resources.configuration
        val newConf = android.content.res.Configuration(conf)
        newConf.fontScale = fontScale
        val wrapped = newBase.createConfigurationContext(newConf)
        super.attachBaseContext(wrapped)
    }

    override fun onResume() {
        super.onResume()
        // Pref에서 코인값 불러오기
       // viewModel.loadCoin(this)

        // 코인, 알림 뱃지 갱신 (onResume에서 처리)
        updateCustomMenuViews()
    }

    // 목적지 ID에 따라 서브타이틀 텍스트 반환 (추가/확장 가능)
    private fun getSubtitleForDestination(destId: Int): String = when (destId) {
        R.id.settingsFragment         -> getString(R.string.preference )
        R.id.aboutUsFragment          -> getString(R.string.about_us )
        R.id.languagePickerFragment   -> getString(R.string.language)
        R.id.registerTravelFragment   -> getString(R.string.add_trip)
        R.id.noticeListFragment       -> getString(R.string.notification)
        R.id.translationFragment      -> getString(R.string.free_translation   )
        R.id.gptTranslationFragment   -> getString(R.string.ai_search_translation)
        R.id.historyTabFragment       -> getString(R.string.history )
        R.id.langPackFragment         -> getString(R.string.manage_lang_pack )
        R.id.cameraCaptureFragment,
        R.id.cameraGptCaptureFragment -> getString(R.string.camera )
        else -> ""
    }

    /** menu item visibility helper */
    private fun setMenuItemVisible(itemId: Int, visible: Boolean) {
        toolbar.menu.findItem(itemId)?.isVisible = visible
    }

    /** 메뉴 커스텀 뷰 클릭 리스너 등록 (actionLayout 기반) */
    private fun setCustomMenuClickListeners() {
// 코인
        toolbar.menu.findItem(R.id.action_coin)?.actionView?.let { actionView ->
            val btnCoin = actionView.findViewById<MaterialButton>(R.id.btnCoin)
            btnCoin.setOnClickListener {
                if (!EnvironmentUtil.isUserLoggedIn()) {
                    // 로그인 화면 이동
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                } else {
                    // 코인 상세/이력 화면 이동 또는 기타 처리
                }
            }
        }
        // 알림
        toolbar.menu.findItem(R.id.action_notification)?.actionView?.let { actionView ->
            val btnNotification = actionView.findViewById<MaterialButton>(R.id.btnNotification)
            btnNotification.setOnClickListener {
                navController.navigate(R.id.noticeListFragment)
            }
        }
        // 설정
        toolbar.menu.findItem(R.id.action_settings)?.actionView?.let { actionView ->
            val btnSettings = actionView.findViewById<MaterialButton>(R.id.btnSettings)
            btnSettings.setOnClickListener {
                navController.navigate(R.id.settingsFragment)
            }
        }
    }

    /** 메뉴 커스텀 뷰(코인, 알림, 뱃지 등) 동적 업데이트 */
    private fun updateCustomMenuViews() {
        // 코인 값
        // 로그인 여부 체크 (EnvironmentUtil 등 활용)
        val isLoggedIn = EnvironmentUtil.isUserLoggedIn()

        // 코인
        toolbar.menu.findItem(R.id.action_coin)?.actionView?.let { actionView ->
            val btnCoin = actionView.findViewById<MaterialButton>(R.id.btnCoin)
            if (!isLoggedIn) {
                btnCoin.text = getString(R.string.sign_in)  // "로그인"
            } else {
                // ★ Pref에서 토큰(코인)값 가져오기 (TokenManager 사용)
                val coin = TokenManager.getTokenFromPref(this)
                btnCoin.text = "\uD83D\uDCB0 $coin"
            }
        }
        // 알림 뱃지 (여기서는 임시 3, 실제는 ViewModel 등에서 받아 처리)
        val count = 3 // 실제 알림 개수로 교체
        toolbar.menu.findItem(R.id.action_notification)?.actionView?.let { actionView ->
            val textBadge = actionView.findViewById<TextView>(R.id.textBadge)
            textBadge.text = count.toString()
            textBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
//            // 뱃지 배경색도 코드에서 동적으로 변경 가능
//            textBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
