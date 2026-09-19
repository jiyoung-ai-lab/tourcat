package com.waveapp.tourcat.design

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.waveapp.tourcat.databinding.ItemAiResultBinding

/**
 * AI 결과 실시간 스트리밍 리스트용 Adapter
 * - partial append (addResult) 및 전체 덮어쓰기 (setResult) 지원
 */
class AiResultAdapter : RecyclerView.Adapter<AiResultAdapter.ResultViewHolder>() {

    private val items = mutableListOf<String>()

    /**
     * 결과를 한 줄씩 누적 추가 (partial append)
     */
    fun addResult(result: String) {
        items.add(result)
        notifyItemInserted(items.size - 1)
        // 필요시 RecyclerView 인스턴스를 받아서 scrollToPosition(items.size - 1) 가능
    }

    /**
     * 전체 결과 덮어쓰기 (초기화 or 최종 결과용)
     */
    fun setResult(text: String) {
        items.clear()
        items.add(text)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemAiResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResultViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ResultViewHolder(private val binding: ItemAiResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.tvResult.text = text
        }
    }
}
