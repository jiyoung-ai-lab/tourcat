package com.waveapp.tourcat.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.ImageButton

object ClipboardShareHelper {

    /**
     * 텍스트를 클립보드에 복사하고, 공유 패널을 띄운다.
     * @param context : Context (Fragment/Activity의 requireContext() 등)
     * @param text : 복사할 텍스트
     * @param copyIcon : 복사완료시 임시로 바꿀 아이콘(ImageButton, optional)
     * @param copyResId : 복사전 아이콘 리소스(R.drawable.ic_copy 등)
     * @param doneResId : 복사완료 아이콘 리소스(R.drawable.ic_copy_done 등)
     */
    fun copyAndShare(
        context: Context,
        text: String,
        copyIcon: ImageButton? = null,
        copyResId: Int? = null,
        doneResId: Int? = null
    ) {
        if (text.isBlank()) return

        // 1. 클립보드 복사
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copy Text", text)
        clipboard.setPrimaryClip(clip)

        // 2. 아이콘 효과(선택)
        if (copyIcon != null && doneResId != null && copyResId != null) {
            copyIcon.setImageResource(doneResId)
            copyIcon.postDelayed({
                copyIcon.setImageResource(copyResId)
            }, 1000)
        }

        // 3. 공유 패널 띄우기
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share App"))
    }
}

/*
btnCopy.setOnClickListener {
    ClipboardShareHelper.copyAndShare(
        context = requireContext(),
        text = tvTranslated.text?.toString() ?: "",
        copyIcon = btnCopy,
        copyResId = R.drawable.ic_copy,
        doneResId = R.drawable.ic_copy_done
    )
}
 */