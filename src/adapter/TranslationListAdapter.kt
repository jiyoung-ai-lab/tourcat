package com.waveapp.tourcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.waveapp.tourcat.R
import com.waveapp.tourcat.helper.ClipboardShareHelper
import com.waveapp.tourcat.item.GptSearchMasterItem
import com.waveapp.tourcat.item.TranslationItem
import com.waveapp.tourcat.util.DateTimeUtil
import io.noties.markwon.Markwon

class TranslationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOAD_MORE = 1
    }

    private val items = mutableListOf<TranslationItem>()
    private var hasMore = false
    private var isLoading = false

    private var markwonInstance: Markwon? = null
    var onLoadMore: (() -> Unit)? = null

    private var _expandedPosition = -1  // 단일 확장
    val expandedPosition: Int
        get() = _expandedPosition

    /** 확장될 때 외부에서 콜백을 받을 수 있도록 */
    var onExpand: ((position: Int) -> Unit)? = null

    // 데이터 전체 교체
    fun setData(newItems: List<TranslationItem>, hasMore: Boolean = false) {
        items.clear()
        items.addAll(newItems)
        this.hasMore = hasMore
        isLoading = false
        _expandedPosition = -1
        notifyDataSetChanged()
    }

    // 데이터 추가(append)
    fun addData(newItems: List<TranslationItem>, hasMore: Boolean = false) {
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

    // 개별 아이템 삭제
    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // 전체 삭제
    fun clear() {
        items.clear()
        _expandedPosition = -1
        notifyDataSetChanged()
    }

//    fun getItem(position: Int): TranslationItem? =
//        if (position < items.size) items[position] else null


    override fun getItemCount() = items.size + if (hasMore) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (hasMore && position == items.size) VIEW_TYPE_LOAD_MORE else VIEW_TYPE_ITEM
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
//        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val btnMore: LinearLayout = view.findViewById(R.id.btnMore)
        val expandArea: ViewGroup  = view.findViewById(R.id.expandArea)
        val tvOcrDetail: TextView = view.findViewById(R.id.tvOcrDetail)
        val tvTransDetail: TextView = view.findViewById(R.id.tvTransDetail)
        val tvDateLang: TextView = view.findViewById(R.id.tvDateLang)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ivPlus: ImageView = view.findViewById(R.id.ivPlus)
        val tvMore: TextView = view.findViewById(R.id.tvMore)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        val divider: View = view.findViewById(R.id.divider)

    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnLoadMore: Button = view.findViewById(R.id.btnLoadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_translation_list, parent, false)
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

            holder.divider.visibility =
                if (position == items.size - 1) View.GONE else View.VISIBLE

            if (!item.imagePath.isNullOrEmpty()) {
                Glide.with(holder.ivIcon)
                    .load(item.imagePath)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(holder.ivIcon)
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_placeholder)
            }
            holder.tvTitle.text = item.translatedText ?: holder.itemView.context.getString(R.string.no_data)
//            holder.tvContent.text = item.translatedText ?: holder.itemView.context.getString(R.string.no_data)


            holder.tvDate.text = "${ DateTimeUtil.formatForDisplayJustDate(item.createdAt)?: "-"} "

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
                    _expandedPosition = -1
                    notifyItemChanged(prev)
                } else {
                    val prev = expandedPosition
                    _expandedPosition = pos
                    if (prev >= 0) notifyItemChanged(prev)
                    notifyItemChanged(pos)
                }
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

            holder.tvOcrDetail.text = "[OCR]\n${item.ocrText ?: "-"}"
            holder.tvTransDetail.text = "[${ holder.itemView.context.getString(R.string.translation) }]\n${item.translatedText ?: "-"}"
            holder.tvDateLang.text = "${DateTimeUtil.formatForDisplay(item.createdAt) ?: "-"} | ${item.langCode ?: "-"}"
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

    fun getItem(position: Int): TranslationItem? = items.getOrNull(position)
}
