package com.waveapp.tourcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textview.MaterialTextView
import com.waveapp.tourcat.R
import com.waveapp.tourcat.helper.ClipboardShareHelper
import com.waveapp.tourcat.item.GptSearchMasterItem
import com.waveapp.tourcat.util.DateTimeUtil
import io.noties.markwon.Markwon

class GptSearchListAdapter3 : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOAD_MORE = 1
    }

    private val items = mutableListOf<GptSearchMasterItem>()
    private var hasMore = false
    private var isLoading = false
    private var markwonInstance: Markwon? = null

    var onLoadMore: (() -> Unit)? = null

    // 외부에서 확장상태 조회만 가능
    private var _expandedPosition = -1  // 단일 확장
    val expandedPosition: Int
        get() = _expandedPosition

    /** 확장될 때 외부에서 콜백을 받을 수 있도록 */
    var onExpand: ((position: Int) -> Unit)? = null

    fun setData(newItems: List<GptSearchMasterItem>, hasMore: Boolean = false) {
        items.clear()
        items.addAll(newItems)
        this.hasMore = hasMore
        isLoading = false
        _expandedPosition = -1
        notifyDataSetChanged()
    }

    fun addData(newItems: List<GptSearchMasterItem>, hasMore: Boolean = false) {
        val start = items.size
        items.addAll(newItems)
        this.hasMore = hasMore
        isLoading = false
        notifyItemRangeInserted(start, newItems.size)
        if (hasMore) notifyItemChanged(itemCount - 1)
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

    fun getItem(position: Int): GptSearchMasterItem? =
        if (position < items.size) items[position] else null

    override fun getItemCount(): Int = items.size + if (hasMore) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (hasMore && position == items.size) VIEW_TYPE_LOAD_MORE else VIEW_TYPE_ITEM
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)

        val btnMore: LinearLayout = view.findViewById(R.id.btnMore)
        val expandArea: FrameLayout = view.findViewById(R.id.expandArea)
        val ivPlus: ImageView = view.findViewById(R.id.ivPlus)
        val tvMore: TextView = view.findViewById(R.id.tvMore)
        val tvTransDetail: MaterialTextView = view.findViewById(R.id.tvTransDetail)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        val tvDateLang: TextView = view.findViewById(R.id.tvDateLang)

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

            // [1] 썸네일 이미지(Glide)
            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.ivIcon)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_gallery)
                    .error(R.drawable.ic_gallery)
                    .centerCrop()
                    .into(holder.ivIcon)
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_gallery)
            }

            // [2] 제목(2줄) (Material3 스타일)
            val plainText = item.resultText.orEmpty()
                .replace(Regex("[^\\p{L}\\p{N}\\p{Zs}]"), "") // 특수문자, 기호, 이모지 제거 (한글, 영문, 숫자, 공백만)
            val title = if (plainText.length > 50) plainText.substring(0, 50) else plainText
            holder.tvTitle.text = title.ifBlank { "-" }
            holder.tvDate.text = "${ DateTimeUtil.formatForDisplayJustDate(item.requestedAt)?: "-"} "
            holder.tvDateLang.text = listOfNotNull(DateTimeUtil.formatForDisplay(item.requestedAt), item.category)
                .joinToString(" | ")

            // [4] 확장/축소(토글)
            val isExpanded = expandedPosition == position

            holder.expandArea.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                holder.ivPlus.setImageResource(R.drawable.ic_minus_vector)
                holder.tvMore.setText(R.string.view_less)
                // 확장됐을 때만 마크다운 적용
                try {
                    val resultText = safeTextForMarkdown(item.resultText)
                    setMarkdown(holder.tvTransDetail, resultText)
                } catch (e: Exception) {
                    holder.tvTransDetail.text = "-"
                }

                holder.btnCopy.setOnClickListener {
                    ClipboardShareHelper.copyAndShare(
                        context = holder.itemView.context,
                        text = holder.tvTransDetail.text?.toString() ?: "",
                        copyIcon = holder.btnCopy,
                        copyResId = R.drawable.ic_copy,
                        doneResId = R.drawable.ic_copy_done
                    )
                }

            } else {
                holder.ivPlus.setImageResource(R.drawable.ic_add_vector)
                holder.tvMore.setText(R.string.view_more)
                // GONE일 때는 텍스트 clear (메모리/레이아웃 오류 방지)
                holder.tvTransDetail.text = ""
            }

            holder.btnMore.setOnClickListener {

                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                if (expandedPosition == pos) {
                    // 이미 확장 중 → 닫기
                    val prev = expandedPosition
                    _expandedPosition = -1
                    notifyItemChanged(prev)
                } else {
                    val prev = expandedPosition
                    _expandedPosition = pos
                    if (prev >= 0) notifyItemChanged(prev)
                    notifyItemChanged(pos)
                    // --- [핵심] 외부 콜백 호출
                    onExpand?.invoke(pos)
                }
            }

        } else if (holder is LoadMoreViewHolder) {
            // 페이징(로딩/더보기)
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

    /**
     * 슬라이드 삭제 시 사용
     */
    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setMarkdown(textView: TextView, markdown: String ) {
        val mdText = markdown ?: ""
        if (markwonInstance == null) {
            markwonInstance = Markwon.create(textView.context)
        }
        val markwon = markwonInstance!!
        markwon.setMarkdown(textView, mdText)

    }
    fun safeTextForMarkdown(input: String?, maxLen: Int = 8000): String {
        if (input.isNullOrBlank()) return "-"
        if (input.length > maxLen)  return input.substring(0, maxLen)
        return input.ifBlank { "-" }
    }
}
