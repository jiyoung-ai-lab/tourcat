package com.waveapp.tourcat.helper

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.waveapp.tourcat.R
import com.waveapp.tourcat.SignInActivity

/**
 * 다양한 메시지 출력 기능을 지원하는 유틸리티
 * - Toast, Snackbar
 * - BottomSheetDialog
 * - AlertDialog
 * - Notification
 */
object MessageHelper {

    /** BottomSheet 커스텀 다이얼로그 */
    fun showBottomSheet(
        context: Context,
        title: String,
        message: String,
        onAllow: () -> Unit,
        onDeny: () -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_bottomsheet, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tv_title).text = title
        view.findViewById<TextView>(R.id.tv_message).text = message


        // ★ 여기서 window background 투명하게 처리 (중요)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }


        view.findViewById<Button>(R.id.btn_allow).setOnClickListener {
            dialog.dismiss(); onAllow()
        }
        view.findViewById<Button>(R.id.btn_deny).setOnClickListener {
            dialog.dismiss(); onDeny()
        }
        dialog.show()
    }

    /** Toast 메시지 */
    fun showToast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun showToastLong(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    /** Snackbar 메시지 */
    fun showSnackbar(view: View, message: String) = Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()

    /** AlertDialog (커스텀 레이아웃) */
    fun showAlert(
        context: Context,
        title: String? = null,
        message: String,
        positiveText: String = context.getString(R.string.confirm),
        negativeText: String? = null,
        cancelable: Boolean = false,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottomsheet, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        val btnPositive = dialogView.findViewById<Button>(R.id.btn_allow)
        val btnNegative = dialogView.findViewById<Button>(R.id.btn_deny)

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(cancelable)
            .create()

        // ★ 여기서 window background 투명하게 처리 (중요)
        alertDialog.setOnShowListener {
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        if (!title.isNullOrEmpty()) {
            tvTitle.text = title
            tvTitle.visibility = View.VISIBLE
        } else {
            tvTitle.visibility = View.GONE
        }
        tvMessage.text = message

        btnPositive.text = positiveText
        btnPositive.setOnClickListener {
            onPositive?.invoke()
            alertDialog.dismiss()
        }

        if (negativeText != null) {
            btnNegative.text = negativeText
            btnNegative.setOnClickListener {
                onNegative?.invoke()
                alertDialog.dismiss()
            }
        } else {
            btnNegative.visibility = View.GONE
        }

        alertDialog.show()
    }

    /** Notification 표시 */
    fun showNotification(
        context: Context,
        channelId: String = "default_channel",
        channelName: String = context.getString(R.string.notification),
        title: String,
        message: String,
        notifyId: Int = 1
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        }
        showNotificationInternal(context, channelId, channelName, title, message, notifyId)
    }

    private fun showNotificationInternal(
        context: Context,
        channelId: String,
        channelName: String,
        title: String,
        message: String,
        notifyId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            notify(notifyId, builder.build())
        }
    }

    fun showSimpleAlert(
        context: Context,
        title: String? = null,
        message: String,
        positiveText: String = context.getString(R.string.confirm),
        negativeText: String? = null,
        cancelable: Boolean = false,
        positiveAction: (() -> Unit)? = null,
        negativeAction: (() -> Unit)? = null
    ) {
        showAlert(
            context = context,
            title = title,
            message = message,
            positiveText = positiveText,
            negativeText = negativeText,
            cancelable = cancelable,
            onPositive = positiveAction,
            onNegative = negativeAction
        )
    }

    /** 로그인 필요 알림 후 로그인 화면 이동 */
    fun showLoginAndMoveDialog(
        context: Context,
        afterLoginIntent: Intent? = null,
        message: String? = null
    ) {
//        context.getString(R.string.msg_quest_signin_full )
        val alertMessage = message ?: context.getString(R.string.msg_quest_signin_full)
        val builder = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.notification))
            .setMessage(alertMessage)
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.sign_in)) { dialog, _ ->
                val intent = Intent(context, SignInActivity::class.java)
                afterLoginIntent?.let { intent.putExtra("AFTER_LOGIN_INTENT", it) }
                context.startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    fun <T> showDeleteConfirmSnackbar(
        recyclerView: RecyclerView,
        context: Context,
        message: String = context.getString(R.string.msg_delete_confirm),
        deletedItem: T,
        deletedPosition: Int,
        items: MutableList<T>,
        adapter: RecyclerView.Adapter<*>,
        onDelete: (() -> Unit)? = null,
        onRestore: (() -> Unit)? = null
    ) {
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
            .setAction(context.getString(R.string.confirm)) {
                onDelete?.invoke()
                showToast(recyclerView.context, context.getString(R.string.msg_delete_complet))
            }
//            .addCallback(object : Snackbar.Callback() {
//                override fun onDismissed(snackbar: Snackbar?, event: Int) {
//                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
//                        items.add(deletedPosition, deletedItem)
//                        adapter.notifyItemInserted(deletedPosition)
//                        onRestore?.invoke()
//                    }
//                }
//            })
            .show()
    }
}
