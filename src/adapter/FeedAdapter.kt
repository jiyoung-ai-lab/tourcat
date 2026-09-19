package com.waveapp.tourcat.adapter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAdView
import com.waveapp.tourcat.HomeFragment
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.CityList
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.common.LocationList.getLatLng
import com.waveapp.tourcat.common.NationList
import com.waveapp.tourcat.helper.MessageHelper
import com.waveapp.tourcat.item.FeedItem
import com.waveapp.tourcat.util.LocaleUtil

class FeedAdapter(
    private var items: List<FeedItem?> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FEED = 1
//        private const val VIEW_TYPE_CUSTOM_AD = 2

        private const val VIEW_TYPE_EXTERNAL_LINK = 2
//        private const val VIEW_TYPE_ADMOB = 3
    }

    private var listener: ((FeedItem) -> Unit)? = null
    private var selectedCityCode: String = "SEL" // 기본값 서울

    fun setOnItemClickListener(listener: (FeedItem) -> Unit) {
        this.listener = listener
    }

    fun submitList(newList: List<FeedItem?>) {
        items = newList
        selectedCityCode = newList.firstOrNull { it?.city?.isNotBlank() == true }?.city ?: "SEL"
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_FEED
            1 -> VIEW_TYPE_EXTERNAL_LINK
//            2 -> VIEW_TYPE_ADMOB
//            2 -> VIEW_TYPE_CUSTOM_AD
            else -> VIEW_TYPE_FEED
        }
    }

    override fun getItemCount(): Int = 2 // 3개 고정

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FEED -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
                FeedViewHolder(v)
            }
//            VIEW_TYPE_CUSTOM_AD -> {
//                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed_ad, parent, false)
//                CustomAdViewHolder(v)
//            }
//            VIEW_TYPE_ADMOB -> {
//                val v = LayoutInflater.from(parent.context)
//                    .inflate(R.layout.item_ad_container , parent, false)
//                return AdmobViewHolder(v)
//            }
            VIEW_TYPE_EXTERNAL_LINK -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed_link, parent, false)
                ExternalLinkViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_feed, parent, false)
                FeedViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FeedViewHolder -> holder.bind(items[position]) // position별 전달!
//            is CustomAdViewHolder -> holder.bind()
            is AdmobViewHolder -> holder.bind()
            is ExternalLinkViewHolder -> holder.bind()
        }
    }

    // 1. 여행일정 카드 (null이면 기본 안내만 노출)
    inner class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: FeedItem?) {
            if (item == null) {
                // 여행플랜이 아예 없는 경우: 안내 텍스트 등만 노출
//                itemView.findViewById<TextView>(R.id.travelDestination)?.text = itemView.context.getString(R.string.trip_plan)
                itemView.findViewById<TextView>(R.id.travelDestination)?.text =itemView.context.getString(R.string.msg_transplan_add_subtitle  )
                itemView.findViewById<TextView>(R.id.travelTimeDisplay)?.text = ""
                itemView.findViewById<TextView>(R.id.travelPeriod)?.text = ""
                itemView.findViewById<TextView>(R.id.weatherYearAndTemp)?.text = ""
                itemView.findViewById<TextView>(R.id.weatherSummary)?.text = ""
                itemView.findViewById<TextView>(R.id.exchangeDesc)?.text = ""
                itemView.findViewById<TextView>(R.id.holidayDesc)?.text = ""
                itemView.findViewById<TextView>(R.id.festivalInfo)?.text = ""
//                itemView.findViewById<ImageView>(R.id.travelFlag)?.setImageResource(R.drawable.ic_launcher_foreground)
                itemView.findViewById<TextView>(R.id.countryFlag)?.text = ""
                itemView.setOnClickListener(null)
            } else {
                val isTravelInfo = item.planId != null
                if (isTravelInfo) {
                    // 여행 정보 전용 뷰 바인딩
                    itemView.findViewById<TextView>(R.id.travelDestination)?.text =
                                                    "${NationList.findName(item.country?:"KR", ComConstant.USER_LANGUAGE_CODE)} ${CityList.getCityName(item.city?:"SEL", ComConstant.USER_LANGUAGE_CODE)}"
                    itemView.findViewById<TextView>(R.id.travelTimeDisplay)?.text = item.travelTimeDisplay ?: ""
                    itemView.findViewById<TextView>(R.id.travelPeriod)?.text = item.period ?: ""
                    itemView.findViewById<TextView>(R.id.weatherYearAndTemp)?.text =
                                                        LocaleUtil.getAvgTempText(item.tempAvg, item.weatherYear, item.weatherMonth, ComConstant.USER_LANGUAGE_CODE)
//                                                    LocaleUtil.getAvgTempText(item.tempAvg, item.weatherYear, item.weatherMonth, ComConstant.USER_LANGUAGE_CODE)
                    itemView.findViewById<TextView>(R.id.weatherSummary)?.text = item.weatherSummary ?: ""
                    itemView.findViewById<TextView>(R.id.exchangeDesc)?.text = item.exchangeRate ?: ""
                    itemView.findViewById<TextView>(R.id.holidayDesc)?.text = item.holidayInfo ?: ""
                    itemView.findViewById<TextView>(R.id.festivalInfo)?.text = item.festivalInfo ?: ""

//                    // 국기 이미지 리소스
//                    if (item.imageResId != null)
//                        itemView.findViewById<ImageView>(R.id.travelFlag)?.setImageResource(item.imageResId)

                    itemView.setOnClickListener { listener?.invoke(item) }
                } else {
                    // 만약 FeedItem인데 planId가 null(기본 피드), 안내만 노출
                    itemView.findViewById<TextView>(R.id.travelDestination)?.text = item.title ?: itemView.context.getString(R.string.trip_plan)
                    itemView.findViewById<TextView>(R.id.travelTimeDisplay)?.text = ""
                    itemView.findViewById<TextView>(R.id.travelPeriod)?.text = ""
                    itemView.findViewById<TextView>(R.id.weatherYearAndTemp)?.text = ""
                    itemView.findViewById<TextView>(R.id.weatherSummary)?.text = ""
                    itemView.findViewById<TextView>(R.id.exchangeDesc)?.text = ""
                    itemView.findViewById<TextView>(R.id.holidayDesc)?.text = ""
//                    itemView.findViewById<ImageView>(R.id.travelFlag)?.setImageResource(R.drawable.ic_launcher_foreground)
                    itemView.findViewById<TextView>(R.id.countryFlag)?.text = ""
                    itemView.setOnClickListener(null)
                }
            }
        }
    }

    // 2. 자체광고(빈 화면/배너)
    inner class CustomAdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() { /* 빈 화면 or 배너 등 */ }
    }

    // 3. AdMob 광고 카드
    inner class AdmobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val adContainerView = itemView.findViewById<FrameLayout>(R.id.ad_container)

            // adContainerView가 null일 가능성 대비 (안전하게 처리)
            if (adContainerView == null) {
//                Log.e("AdmobViewHolder", "ad_container not found in itemView")
                return
            }

            val context = itemView.context

            // Fragment에서 관리하는 ad list 사용
            val frag = (context as? FragmentActivity)
                ?.supportFragmentManager
                ?.findFragmentById(R.id.nav_host_fragment) as? HomeFragment

            val nativeAd = frag?.nativeAds?.getOrNull(0) // 인덱스는 필요에 따라 조정

            if (nativeAd != null) {
                val adView = LayoutInflater.from(context)
                    .inflate(R.layout.item_feed_admob, adContainerView, false) as NativeAdView

                frag.populateNativeAdView(nativeAd, adView)

                adContainerView.removeAllViews()
                adContainerView.addView(adView)
            }
        }
    }
//    inner class AdmobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        fun bind() {
//            val adView = itemView.findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
//            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
//            adView.loadAd(adRequest)
//        }
//    }

    // 4. 외부링크 카드
    inner class ExternalLinkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {

            val (lat, lng) = getLatLng(selectedCityCode  )
            itemView.findViewById<LinearLayout>(R.id.btnGoogleMap)?.setOnClickListener {
//                openUrl("https://maps.google.com/?q=$lat,$lng", it)
                openMap(lat.toDouble(), lng.toDouble() , itemView.context )
            }
            itemView.findViewById<LinearLayout>(R.id.btnChatGPT)?.setOnClickListener {
                openChatGPT(it.context)
            }
            itemView.findViewById<LinearLayout>(R.id.btnWeather)?.setOnClickListener {
//                openWeatherChannel(lat, lng, weatherLang, it)
                openUrl("https://weather.com/$weatherLang/weather/today/l/$lat,$lng", it)
            }
        }
        private fun openUrl(url: String, view: View) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
            } catch (e: Exception) {
                MessageHelper.showToast(view.context, view.context.getString(R.string.msg_link_failed ))
            }
        }
        fun openMap(lat: Double, lng: Double, context: Context) {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng") // 위치 + 검색 마커 포함
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps") // 구글맵 앱으로 강제
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // 구글맵 앱이 없을 경우 웹 브라우저로 fallback
                val browserUri = Uri.parse("https://maps.google.com/?q=$lat,$lng&z=17")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            }
        }
        fun openChatGPT(context: Context) {
            val packageName = "com.openai.chatgpt"

            val pm = context.packageManager
            try {
                // 앱이 설치되어 있는지 확인
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)

                // 설치되어 있으면 앱 실행
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    MessageHelper.showToast(context, "App Open Failed")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // 설치되어 있지 않으면 Play 스토어로 이동
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")
                        )
                    )
                } catch (ex: ActivityNotFoundException) {
                    // 만약 Play 스토어가 없을 경우 브라우저로 이동
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        )
                    )
                }
            }
        }

        val weatherLang = when (ComConstant.USER_LANGUAGE_CODE) {
            "ko" -> "ko-KR"
            "ja" -> "ja-JP"
            "zh" -> if (ComConstant.USER_NATION_CODE == "TW") "zh-TW" else "zh-CN"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "es" -> "es-ES"
            "pt" -> "pt-BR"
            "it" -> "it-IT"
            else -> "en-US"
        }
    }
}