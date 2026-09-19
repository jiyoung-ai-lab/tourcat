package com.waveapp.tourcat.design

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.waveapp.tourcat.R
import com.waveapp.tourcat.databinding.BottomSheetAiSearchBinding
import io.noties.markwon.Markwon

class AiSearchBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAiSearchBinding? = null
    private val binding get() = _binding!!

    // 마크다운 파서 (전역 1회 생성)
    private var markwon: Markwon? = null

    // 현재 누적 결과
    private var currentMarkdown: String = ""

    override fun getTheme(): Int = R.style.Theme_TourCat_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAiSearchBinding.inflate(inflater, container, false)
        // isCancelable = false  // 드래그/백버튼 닫기 금지 → 드래그는 허용하되 완전 닫힘만 막음
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        markwon = Markwon.create(requireContext())

        // 로딩 애니메이션 표시
        binding.progressBarDots.visibility = View.VISIBLE
        // 최초 결과 초기화
        setResultText("")
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dlg ->
            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                // ✨ 1. "최소 peekHeight"를 dp로 명확하게 지정! (예: 220dp)
                behavior.peekHeight = dpToPx(500)

                // ✨ 2. "드래그로만 확장/축소", append로는 크기 변화 없음(자동 확장X)
                behavior.isDraggable = true
                behavior.isHideable = false // 완전 닫힘 막기

                // ✨ 3. (유지) 닫힘 시도하면 다시 peekHeight(축소)로 복귀
                behavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })

                // ✨ 4. (변경) "자동 확장" 부분 삭제/주석처리!
                //    → 오직 드래그로만 확장! (자동 EXPANDED 없음)
                // behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    // dp → px 변환 함수(클래스 내부에 추가)
    private fun dpToPx(dp: Int): Int =
        (dp * requireContext().resources.displayMetrics.density).toInt()

    /** AI 작업 완료 시(수동 닫힘 허용) */
    fun onAiTaskDone() {
        isCancelable = true
        dismissAllowingStateLoss()
    }
//
//    /** 실시간 누적 append (마크다운) */
//    fun addResult(text: String) {
//        activity?.runOnUiThread {
//            if (text.isNotBlank()) {
//                if (currentMarkdown.isNotEmpty()) {
//                    currentMarkdown += "\n\n" + text
//                } else {
//                    currentMarkdown = text
//                }
//                markwon?.setMarkdown(binding.tvMarkdown, currentMarkdown)
//                // 스크롤 맨 아래로
//                binding.scrollMarkdown.post {
//                    binding.scrollMarkdown.fullScroll(View.FOCUS_DOWN)
//                }
//            }
//        }
//    }


    fun addResultSmart(text: String) {
        activity?.runOnUiThread {
            if (text.isNotBlank()) {
                val scrollView = binding.scrollMarkdown
                val textView = binding.tvMarkdown

                val isFirstAppend = currentMarkdown.isEmpty()

                // 1. append 전 스크롤 위치 저장 (읽고 있는 상태)
                val prevScrollY = scrollView.scrollY

                // 2. 기존 append 방식 그대로
                if (!isFirstAppend) {
                    currentMarkdown += "\n\n" + text
                } else {
                    currentMarkdown = text
                }
                markwon?.setMarkdown(textView, currentMarkdown)

                scrollView.post {
                    if (isFirstAppend) {
                        // 최초 append만 최상단으로
                        scrollView.fullScroll(View.FOCUS_UP)
                    } else {
                        // 3. append "직전"에 스크롤이 최하단이었는지 판단
                        val isAtBottom =
                            prevScrollY + scrollView.height >= textView.height - 8

                        if (isAtBottom) {
                            // 최하단이었던 경우만 fullScroll(FOCUS_DOWN)
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        } else {
                            // 그 외에는 이전 위치로 강제 복원
                            scrollView.scrollTo(0, prevScrollY)
                        }
                    }
                }
            }
        }
    }

    /** 전체 결과 새로 세팅 (마크다운) */
    fun setResultText(text: String) {
        activity?.runOnUiThread {
            currentMarkdown = text ?: ""
            markwon?.setMarkdown(binding.tvMarkdown, currentMarkdown)
            binding.scrollMarkdown.post {
                binding.scrollMarkdown.fullScroll(View.FOCUS_UP)
            }
        }
    }

    /** 로딩 애니메이션 on/off */
    fun setLoading(visible: Boolean) {
        binding.progressBarDots.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        markwon = null
    }
}
