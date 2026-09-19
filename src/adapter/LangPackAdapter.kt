package com.waveapp.tourcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.waveapp.tourcat.R
import com.waveapp.tourcat.databinding.ItemLangpackBinding
import com.waveapp.tourcat.item.LangPackItem

class LangPackAdapter(
    private val items: List<LangPackItem>,
    private val onCheckedChange: (Int, Boolean) -> Unit,
    private val onToggle: ((Int, Boolean) -> Unit)? = null // 옵션
) : RecyclerView.Adapter<LangPackAdapter.LangPackViewHolder>() {

    inner class LangPackViewHolder(private val binding: ItemLangpackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LangPackItem, position: Int) {
            val context = binding.root.context
            val isEnglish = item.code == "en"

            // 체크박스 리스너 잠시 해제
            binding.ckInstalled.setOnCheckedChangeListener(null)

            // 체크박스 상태 & 활성화 제어
            binding.ckInstalled.isChecked = item.isChecked
            binding.ckInstalled.isEnabled = !item.isInProgress && !isEnglish

            // 영어는 "항상 설치됨", 회색 표시
            if (isEnglish) {
                binding.ckInstalled.text = context.getString(R.string.install) // 혹은 "삭제 불가"
                binding.ckInstalled.setTextColor(ContextCompat.getColor(context, R.color.gray_500))
            } else {
                binding.ckInstalled.text = if (item.isInstalled) context.getString(R.string.install) else context.getString(R.string.not_installed)
                binding.ckInstalled.setTextColor(ContextCompat.getColor(context, R.color.black))
            }

            // 프로그레스바 처리
            binding.progressInstalling.visibility = if (item.isInProgress) View.VISIBLE else View.GONE

            // 언어명
            binding.tvLangpackName.text = item.name

            // 클릭 리스너(영어/작업중 제외)
            if (!item.isInProgress && !isEnglish) {
                binding.ckInstalled.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(position, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangPackViewHolder {
        val binding = ItemLangpackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LangPackViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LangPackViewHolder, position: Int) {
        holder.bind(items[position], position)
    }
}
