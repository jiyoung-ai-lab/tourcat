package com.waveapp.tourcat.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.waveapp.tourcat.R
import com.waveapp.tourcat.item.ImageItem
import com.waveapp.tourcat.util.LogUtil
import java.io.File


class ImageListAdapter(
    private val maxCount: Int =5,
    private val onAddClick: (() -> Unit),
    private val onImageDelete: ((Int) -> Unit),
    private val onImageClick: (ImageItem, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: MutableList<ImageItem> = mutableListOf()
        private set

    override fun getItemCount(): Int {

        if (items.size < maxCount) {
            LogUtil.i("getItemCount   start!!",  items.size.toString())
            return   items.size + 1
        } else {
            LogUtil.i("getItemCount   start2!!",  items.size.toString())
            return items.size
        }
    }

    override fun getItemViewType(position: Int): Int {

        if (position == items.size) {
            LogUtil.i("getItemViewType   start!!",  items.size.toString())
            return   1
        } else {
            LogUtil.i("getItemViewType   start2!!",  items.size.toString())
            return 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1 ) {
            if ( items.size < maxCount  ) {
                LogUtil.i("AddViewHolder를   start!!", "ImageLIstAdapter")
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_photo, parent, false)
                AddViewHolder(view)
            } else {
                LogUtil.i("AddViewHolder를   exception!!", "ImageLIstAdapter")
                // 5개 이상일 경우, AddViewHolder를 반환하지 않음 (아이템이 더 이상 추가되지 않음)
                throw IllegalStateException("AddViewHolder cannot be created as maxCount is reached")
            }

        } else {
            LogUtil.i("ImageViewHolder   start!!", "ImageLIstAdapter")
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            ImageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder  ) {
            LogUtil.i("onBindViewHolder  ImageViewHolder start!!", "ImageLIstAdapter")
            val imageItem = items[position]
            LogUtil.i("onBindViewHolder  ImageViewHolder end!!", "ImageLIstAdapter")
            holder.bind(imageItem, position)
        } else if (holder is AddViewHolder) {
            LogUtil.i("onBindViewHolder  AddViewHolder start!!", "ImageLIstAdapter")
            holder.bind()
            LogUtil.i("onBindViewHolder  AddViewHolder end!!", "ImageLIstAdapter")
        }
    }

    // 실제 이미지 썸네일+삭제 버튼
    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        fun bind(item: ImageItem, position: Int) {
            Glide.with(imageView.context)
                .load(File(item.thumbPath)) // 내부저장소 절대경로면 File 객체로!
                .into(imageView)
//            imageView.setImageURI(android.net.Uri.parse(item.path)) // Glide 없을 때 임시

//            LogUtil.d("ImageViewHolder  RecyclerView  start!!", "ImageLIstAdapter")
            btnDelete.setOnClickListener { onImageDelete.invoke(position) }
            imageView.setOnClickListener { onImageClick.invoke(item, position) } // 추가!
        }
    }

    // 마지막에 + 버튼
    inner class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val addBtn: ImageView = view.findViewById(R.id.btnAddPhoto)
        fun bind() {

            LogUtil.d("AddViewHolder  RecyclerView  start!!", "ImageLIstAdapter")
            addBtn.setOnClickListener { onAddClick.invoke() }
        }
    }
    // 아이템 전체 교체 (수정모드 이미지 불러올 때)
    fun setItems(newList: List<ImageItem>?) {
        items.clear()
        if (newList != null) items.addAll(newList)
        notifyDataSetChanged()
    }

    // 개별 삭제
    fun removeItemAt(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // 아이템 추가 (이미 작성되어 있을 것)
    fun addItem(item: ImageItem) {
        LogUtil.i("addItem start!!", "ImageLIstAdapter")
        if (items.size < maxCount) {
            items.add(item)
//            notifyItemInserted(items.size - 1)
            notifyDataSetChanged() // <-- notifyItemInserted 대신 강제로 전체 갱신
        }
        LogUtil.i("addItem end~~", "ImageLIstAdapter")
    }


}
