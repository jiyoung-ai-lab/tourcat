package com.waveapp.tourcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.waveapp.tourcat.R
import com.waveapp.tourcat.item.GptSearchMasterItem

class GptSearchMasterAdapter2 : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOAD_MORE = 1
    }

    private val items = mutableListOf<GptSearchMasterItem>()
    private var hasMore = false
    private var isLoading = false
    private var expandedPosition = -1 // 단일 확장형

    var onLoadMore: (() -> Unit)? = null

    fun setData(newItems: List<GptSearchMasterItem>, hasMore: Boolean = false) {
        items.clear()
        items.addAll(newItems)
        this.hasMore = hasMore
        isLoading = false
        expandedPosition = -1
        notifyDataSetChanged()
    }

    fun addData(newItems: List<GptSearchMasterItem>, hasMore: Boolean = false) {
        val start = items.size
        items.addAll(newItems)
        this.hasMore = hasMore
        isLoading = false
        notifyItemRangeInserted(start, newItems.size)
        notifyItemChanged(itemCount - 1) // 더보기 뷰 새로고침
    }

    fun setHasMore(hasMore: Boolean) {
        this.hasMore = hasMore
        isLoading = false
        notifyItemChanged(itemCount - 1)
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        notifyItemChanged(itemCount - 1)
    }

    fun getItem(position: Int): GptSearchMasterItem? = if (position < items.size) items[position] else null

    override fun getItemCount(): Int = items.size + if (hasMore) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (hasMore && position == items.size) VIEW_TYPE_LOAD_MORE else VIEW_TYPE_ITEM
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvQueryDetail: TextView = view.findViewById(R.id.tvQueryDetail)
        val tvResultDetail: TextView = view.findViewById(R.id.tvResultDetail)
        val tvCategoryDate: TextView = view.findViewById(R.id.tvCategoryDate)
        val btnMore: LinearLayout = view.findViewById(R.id.btnMore)
        val expandArea: LinearLayout = view.findViewById(R.id.expandArea)
        val ivPlus: ImageView = view.findViewById(R.id.ivPlus)
        val tvMore: TextView = view.findViewById(R.id.tvMore)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnLoadMore: Button = view.findViewById(R.id.btnLoadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gpt_search_list, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_load_more, parent, false)
            LoadMoreViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val item = items[position]
            // 이미지 (Glide로 파일/URL 모두 지원)
            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.ivIcon)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(holder.ivIcon)
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_placeholder)
            }

            // 헤더(제목, 간단설명)
//            holder.tvTitle.text = item.queryText ?: "AI 질문"
            val title = item.resultText
                ?.lineSequence()
                ?.drop(1)                // 첫 줄(제품명) 건너뛰기
                ?.firstOrNull()          // 두 번째 줄(자세한상품명) 추출
                ?.takeIf { it.isNotBlank() }
                ?: item.queryText ?: "AI Request"

            holder.tvTitle.text = title
            holder.tvContent.text = item.resultText ?: "AI Response"

            // 확장 영역(질문, 답변, 카테고리+날짜)
            //holder.tvQueryDetail.text = "[질문]\n${item.queryText ?: "-"}"
            holder.tvResultDetail.text = "${item.resultText ?: "-"}"
            holder.tvCategoryDate.text = "${item.category ?: "-"} | ${item.requestedAt ?: "-"}"

            // 확장/축소 상태
            val isExpanded = expandedPosition == position
            holder.expandArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
            if (isExpanded) {
                holder.ivPlus.setImageResource(R.drawable.ic_minus_vector)
                holder.tvMore.text = holder.itemView.context.getString(R.string.view_less)
            } else {
                holder.ivPlus.setImageResource(R.drawable.ic_add_vector)
                holder.tvMore.text = holder.itemView.context.getString(R.string.view_more)
            }
            holder.btnMore.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                if (expandedPosition == pos) {
                    val prev = expandedPosition
                    expandedPosition = -1
                    notifyItemChanged(prev)
                } else {
                    val prev = expandedPosition
                    expandedPosition = pos
                    if (prev >= 0) notifyItemChanged(prev)
                    notifyItemChanged(pos)
                }
            }
        } else if (holder is LoadMoreViewHolder) {
            holder.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            holder.btnLoadMore.visibility = if (isLoading) View.GONE else View.VISIBLE
            holder.btnLoadMore.setOnClickListener {
                if (!isLoading) {
                    isLoading = true
                    notifyItemChanged(position)
                    onLoadMore?.invoke()
                }
            }
        }
    }

    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
