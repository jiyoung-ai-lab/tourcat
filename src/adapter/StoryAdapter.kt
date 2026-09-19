package com.waveapp.tourcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.CityList
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.item.StoryItem
import com.waveapp.tourcat.item.StoryType
import com.waveapp.tourcat.util.LocaleUtil

class StoryAdapter(
    private val onAddClick: () -> Unit,                // 플러스(여행등록) 버튼 클릭 콜백
    private val onStoryClick: (StoryItem) -> Unit      // 일반 스토리 클릭 콜백
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private var items: List<StoryItem> = emptyList()
    private var selectedPlanId: Long? = null           // 선택된 planId 보관

    // 리스트 및 선택 상태 갱신
    fun submitList(newList: List<StoryItem>, selectedId: Long? = null) {
        items = newList
        selectedPlanId = selectedId
        notifyDataSetChanged()
    }

    fun updateSelected(planId: Long?) {
        selectedPlanId = planId
        notifyDataSetChanged()
    }

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnAddPlan: ImageView = view.findViewById(R.id.btnAddPlan)
        private val monthView: TextView = view.findViewById(R.id.storyMonth)
        private val cityView: TextView = view.findViewById(R.id.storyCity)

        fun bind(item: StoryItem) {
            if (item.type == StoryType.REGISTER) {
                // 플러스(여행등록) 버튼 아이템
                btnAddPlan.visibility = View.VISIBLE
                monthView.visibility = View.GONE
                cityView.visibility = View.GONE

                btnAddPlan.setOnClickListener { onAddClick() }
                itemView.setOnClickListener { onAddClick() }
                itemView.isSelected = false  // 등록 버튼은 선택상태 없음
            } else {
                // 일반 여행 스토리 아이템
                btnAddPlan.visibility = View.GONE
                monthView.visibility = View.VISIBLE
                cityView.visibility = View.VISIBLE

                monthView.text = LocaleUtil.getMonthTextFromDbStringByLocale(
                    item.startDate, ComConstant.USER_LANGUAGE_CODE
                )
                cityView.text = CityList.getCityName(
                    item.city, ComConstant.USER_LANGUAGE_CODE
                )

                // 선택 상태에 따라 배경 변경 (selector)
                itemView.isSelected = (item.planId == selectedPlanId)

                itemView.setOnClickListener { onStoryClick(item) }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == StoryType.REGISTER) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(v)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
