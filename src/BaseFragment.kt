package com.waveapp.tourcat

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * BaseFragment (2025 실무/Material3 기준)
 * - 진행중 UI(progressBar, tv_processing) 자동 지원
 * - showProgress/hideProgress 한 번에 UX 제어
 * - 애니메이팅 지원(showProgressAnimated/hideProgressAnimated)
 * - onViewCreated에서 자동 연결
 * - hideKeyboard, onStop에서 자동 해제
 */
open class BaseFragment : Fragment() {

    // [1] 진행중 ProgressBar / 텍스트뷰 동시 멤버 변수
    private var progressBar: ProgressBar? = null
    private var processingTextView: TextView? = null

    // [2] 진행중 애니메이션 관련 변수(Handler/Runnable)
    private var progressAnimHandler: Handler? = null
    private var progressAnimRunnable: Runnable? = null
    private var progressAnimStep = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.progressBar)
        processingTextView = view.findViewById(R.id.tv_processing)
        if (progressBar == null && processingTextView == null) {
            Log.i("BaseFragment", "진행중 UI(progressBar, tv_processing) 없음: ${this::class.java.simpleName}. Safe to ignore if not needed.")
        }
    }

    /**
     * [showProgress]
     * - progressBar 우선, 없으면 tv_processing 텍스트뷰 표시(애니메이션 없음)
     */
    fun showProgress(message: String = "진행중...") {
        if (progressBar != null) {
            progressBar?.visibility = View.VISIBLE
        } else if (processingTextView != null) {
            processingTextView?.text = message
            processingTextView?.visibility = View.VISIBLE
        } else {
            Log.d("BaseFragment", "showProgress: 진행중 UI 없음. Nothing to show.")
        }
    }

    /**
     * [hideProgress]
     * - progressBar > tv_processing 순서로 숨김
     */
    fun hideProgress() {
        if (progressBar != null) {
            progressBar?.visibility = View.GONE
        }
        if (processingTextView != null) {
            processingTextView?.visibility = View.GONE
        }
        // 애니메이션 진행중이면 항상 중단
        hideProgressAnimated()
    }

    /**
     * [showProgressAnimated]
     * - progressBar가 없고 tv_processing만 있으면 애니메이팅("진행중.", "..", "...") 표시
     * - Handler/Runnable로 0.4초마다 텍스트 변화
     */
    fun showProgressAnimated(baseMsg: String = "진행중") {
        if (progressBar != null) {
            progressBar?.visibility = View.VISIBLE
            return
        }
        if (processingTextView == null) {
            Log.d("BaseFragment", "showProgressAnimated: 진행중 텍스트뷰 없음.")
            return
        }
        processingTextView?.visibility = View.VISIBLE
        progressAnimStep = 0
        if (progressAnimHandler == null) {
            progressAnimHandler = Handler(Looper.getMainLooper())
        }
        progressAnimRunnable = object : Runnable {
            override fun run() {
                val dots = ".".repeat((progressAnimStep % 3) + 1)
                processingTextView?.text = "$baseMsg$dots"
                progressAnimStep++
                progressAnimHandler?.postDelayed(this, 400)
            }
        }
        progressAnimHandler?.post(progressAnimRunnable!!)
    }

    /**
     * [hideProgressAnimated]
     * - 진행중 애니메이션 중단, 텍스트뷰 숨김
     */
    fun hideProgressAnimated() {
        progressAnimHandler?.removeCallbacks(progressAnimRunnable ?: return)
        processingTextView?.visibility = View.GONE
        progressAnimStep = 0
    }

    /**
     * [hideKeyboard]
     * - 키보드 내리기 (공통 유틸)
     */
    fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * [onStop]
     * - 프래그먼트가 중단될 때 진행중 UI 자동 해제
     */
    override fun onStop() {
        super.onStop()
        hideProgress()
    }
}
