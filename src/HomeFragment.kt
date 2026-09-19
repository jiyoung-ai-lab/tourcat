package com.waveapp.tourcat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.appbar.MaterialToolbar
import com.waveapp.tourcat.adapter.FeedAdapter
import com.waveapp.tourcat.adapter.StoryAdapter
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.common.LocationList
import com.waveapp.tourcat.connection.assetdata.ExchangeRateRepository
import com.waveapp.tourcat.database.CityWeatherAssetDbAdapter
import com.waveapp.tourcat.database.FestivalAssetDbAdapter
import com.waveapp.tourcat.database.HolidayAssetDbAdapter
import com.waveapp.tourcat.database.NoticeDbAdapter
import com.waveapp.tourcat.database.TravelPlanDbAdapter
import com.waveapp.tourcat.helper.MessageHelper
import com.waveapp.tourcat.item.FeedItem
import com.waveapp.tourcat.item.HolidayItem
import com.waveapp.tourcat.item.StoryItem
import com.waveapp.tourcat.item.StoryType
import com.waveapp.tourcat.item.getLocalizedSummary
import com.waveapp.tourcat.util.EnvironmentUtil
import com.waveapp.tourcat.util.LocaleUtil

class HomeFragment : BaseFragment() {

    // 뒤로가기 두 번 클릭 flag, handler
    private var backPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    private val viewModel: MainViewModel by activityViewModels()
    private var selectedPlanId: Long? = null

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var storyAdapter: StoryAdapter

    // AdMob 네이티브 광고 리스트
    val nativeAds = mutableListOf<NativeAd>()
    private lateinit var adLoader: AdLoader

    // 레이아웃 바인딩 뷰
    private var _rootView: View? = null
    private val rootView get() = _rootView!!

    // Toolbar 참조
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _rootView = inflater.inflate(R.layout.fragment_home, container, false)

        // 뒤로가기 콜백 등록 (Fragment에서만 동작)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1초 이내 두 번 클릭시 종료, 아니면 안내 메시지
                if (backPressedOnce) {
                    requireActivity().finish()
                } else {
                    backPressedOnce = true
                    MessageHelper.showSnackbar(rootView, getString(R.string.msg_guide_doublebackkey  ))
                    handler.postDelayed({ backPressedOnce = false }, 1000)
                }
            }
        })

        return _rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = requireActivity().findViewById(R.id.topAppBar)
        setupToolbarMenu()

        // 1. 주요 네비게이션 버튼 (AI검색, 사진번역, 히스토리)
        view.findViewById<View>(R.id.btnAiSearch).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_gpttranslation)
        }
        view.findViewById<View>(R.id.btnTranslation).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_translation)
        }
        view.findViewById<View>(R.id.btnHistory).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        // 첫번째 여행 아이템(TRAVEL) 자동 선택
        val storyList = getStoryList()
        if (selectedPlanId == null) {
            val firstTravel = storyList.firstOrNull { it.type == StoryType.TRAVEL && it.planId != null }
            selectedPlanId = firstTravel?.planId
        }

        // 2. 스토리(TripList) RecyclerView
        val storyRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerStory)
        storyAdapter = StoryAdapter(
            onAddClick = { findNavController().navigate(R.id.action_home_to_register) },
            onStoryClick = { item ->
                if (item.type == StoryType.TRAVEL && item.planId != null) {
                    selectedPlanId = item.planId
                    storyAdapter.updateSelected(selectedPlanId)
                    feedAdapter.submitList(getFeedItems(selectedPlanId))
                }
            }
        )
        storyRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        storyRecyclerView.adapter = storyAdapter
        storyAdapter.submitList(getStoryList(), selectedPlanId)

        // 3. 피드(여행정보/광고/외부링크) RecyclerView
        val feedRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerFeed)
        feedAdapter = FeedAdapter(getFeedItems(selectedPlanId)).apply {
            setOnItemClickListener { feedItem ->
                feedItem?.planId?.let { planId ->
                    val bundle = Bundle().apply { putLong("planId", planId) }
                    findNavController().navigate(R.id.action_home_to_register, bundle)
                }
            }
        }
        feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        feedRecyclerView.adapter = feedAdapter

        // 홈 진입 시 Pref에서 코인값 최신화 (onResume에서도 보장)
        viewModel.loadCoin(requireContext())
    }

    override fun onResume() {
        super.onResume()
        storyAdapter.submitList(getStoryList(), selectedPlanId)
        feedAdapter.submitList(getFeedItems(selectedPlanId))
        updateNoticeBadge()
        viewModel.loadCoin(requireContext())
    }

    /** Toolbar 메뉴/액션(코인, 알림, 설정) 연동 */
    private fun setupToolbarMenu() {
        // 1. 코인 관찰 (코인 메뉴 타이틀에 동적 표시/로그인 미진입시 "로그인" 표시)
        toolbar.menu.findItem(R.id.action_coin)?.let { coinItem ->
            viewModel.coin.observe(viewLifecycleOwner) { coin ->
                if (!EnvironmentUtil.isUserLoggedIn()) {
                    coinItem.title = getString(R.string.sign_in) // "로그인"
                } else {
                    coinItem.title = "\uD83D\uDCB0 $coin"
                }
            }
        }

        // 2. 메뉴 클릭
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_coin -> {
                    if (!EnvironmentUtil.isUserLoggedIn()) {
                        // 로그인 안내 및 이동
                        MessageHelper.showLoginAndMoveDialog(
                            context = requireContext(),
                            afterLoginIntent = null,
                            message = getString(R.string.msg_quest_signin)
                        )
                    } else {
                        // 코인 상세/이력 등 액션
                        // TODO: 코인 상세 화면 이동 구현
                    }
                    true
                }
                R.id.action_notification -> {
                    findNavController().navigate(R.id.action_home_to_noticeListFragment)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_home_to_settings)
                    true
                }
                else -> false
            }
        }

        // 3. 알림 actionLayout(뱃지) 클릭 (메뉴 아이템이 actionLayout을 사용하는 경우)
        toolbar.menu.findItem(R.id.action_notification)?.actionView?.apply {
            findViewById<View>(R.id.btnNotification)?.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_noticeListFragment)
            }
        }

        // 4. Toolbar navigation(뒤로가기) 아이콘 표시/동작 (메인에서는 숨김, 필요 시 setNavigationIcon)
        toolbar.navigationIcon = null // 홈화면에서는 숨김
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /** 알림 메뉴 뱃지 숫자 동적 업데이트 */
    private fun updateNoticeBadge() {
        val notificationMenuItem = toolbar.menu.findItem(R.id.action_notification)
        val actionView = notificationMenuItem?.actionView
        val badge = actionView?.findViewById<TextView>(R.id.textBadge)
        val db = NoticeDbAdapter(requireContext()).open()
        val unreadCount = db.getUnreadNoticeCount(LocaleUtil.getValidLangCode(ComConstant.USER_LANGUAGE_CODE))
        db.close()
        badge?.apply {
            text = if (unreadCount > 99) "99" else unreadCount.toString()
            visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        }
    }

    // ------ 데이터 매핑/헬퍼 함수들은 기존 그대로 ------
    private fun getStoryList(): List<StoryItem> {
        val list = TravelPlanDbAdapter(requireContext()).getTravelStoryList()
        val addItem = StoryItem(
            type = StoryType.REGISTER,
            startDate = null,
            endDate = null,
            nation = "",
            city = "",
            imageResId = R.drawable.ic_placeholder,
            planId = null
        )
        return listOf(addItem) + list
    }

    // planId 비교시 안전하게 toString()으로 일치화(타입 혼선 방지)
    private fun getFeedItems(selectedPlanId: Long?): List<FeedItem?> {
        val travelFeedList = TravelPlanDbAdapter(requireContext()).getTravelFeedList()
        val assetMappedFeedList = mapFeedWithAssets(travelFeedList)
        val travelItem = assetMappedFeedList.firstOrNull {
            it.planId?.toString() == selectedPlanId?.toString()
        } ?: assetMappedFeedList.firstOrNull()
        return listOf(
            travelItem,  // 0: 여행정보
            null,        // 1: 자체광고
            null,        // 2: AdMob 광고
            null         // 3: 외부링크
        )
    }

    private fun mapFeedWithAssets(travelFeedList: List<FeedItem>): List<FeedItem> {
        val weatherDb = CityWeatherAssetDbAdapter(requireContext()).open()
        val holidayDb = HolidayAssetDbAdapter(requireContext()).open()
        val festivalDb = FestivalAssetDbAdapter(requireContext()).open()
        val mappedList = travelFeedList.map { feedItem ->
            val year = feedItem.startDate?.substring(0, 4)?.toIntOrNull()
            val month = feedItem.startDate?.substring(4, 6)?.toIntOrNull()
            val day = feedItem.startDate?.substring(6, 8)?.toIntOrNull()
            val yearEnd = feedItem.endDate?.substring(0, 4)?.toIntOrNull()
            val monthEnd = feedItem.endDate?.substring(4, 6)?.toIntOrNull()
            val dayEnd = feedItem.endDate?.substring(6, 8)?.toIntOrNull()
            val city = feedItem.city
            val country = feedItem.country

            val weatherInfo = if (city != null && month != null)
                weatherDb.getWeatherInfo(city, month)
            else null

            val rateMap = ExchangeRateRepository.getRateMap()
            var rateFullstr = ""
            if (rateMap != null) {
                rateFullstr = ExchangeRateRepository.convertStringByCountry(rateMap, country ?: "US", ComConstant.USER_NATION_CODE)
            }

            val holidayInfo = if (year != null && month != null && day != null && yearEnd != null && monthEnd != null && dayEnd != null && country != null)
                holidayDb.getHolidayInfoRange(year, month, day, yearEnd, monthEnd, dayEnd, country)
            else null

            val festivalInfo = if (country != null && city != null && year != null && month != null && day != null && yearEnd != null && monthEnd != null && dayEnd != null)
                festivalDb.getFestivalInfoByDateRange(country, city, year, month, day, yearEnd, monthEnd, dayEnd)
            else null

            val weatherNatonSummary = weatherInfo?.getLocalizedSummary(ComConstant.USER_LANGUAGE_CODE) ?: ""
            val festivalNatonSummary = festivalInfo?.getLocalizedSummary(ComConstant.USER_LANGUAGE_CODE) ?: ""
            val holidayNationSummay = holidayInfo?.holidayNationSummary(ComConstant.USER_LANGUAGE_CODE) ?: ""
            val travelTimeDisplay = LocationList.showTargetTimeWithDDay(city ?: "SEL")

            feedItem.copy(
                weatherYear = weatherInfo?.referenceYear ?: 0,
                weatherMonth = weatherInfo?.month ?: 0,
                tempAvg = weatherInfo?.tempAvg ?: 0.0,
                weatherSummary = weatherNatonSummary,
                exchangeRate = rateFullstr,
                holidayInfo = holidayNationSummay,
                festivalInfo = festivalNatonSummary,
                travelTimeDisplay = travelTimeDisplay
            )
        }
        weatherDb.close()
        holidayDb.close()
        festivalDb.close()
        return mappedList
    }

    // 기타 헬퍼 함수
    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // ...생략(광고용)
    }

    fun HolidayItem.holidayNationSummary(langcode: String): String {
        val dateStr = "%02d/%02d".format(this.month, this.day)
        val title = this.getLocalizedSummary(langcode) ?: ""
        return "$dateStr $title"
    }

    override fun onDestroyView() {
        nativeAds.forEach { it.destroy() }
        nativeAds.clear()
        _rootView = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
