package com.waveapp.tourcat.design

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class PulseRadialLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dotCount = 8 // 점 개수(자유롭게 조절)
    private val minRadiusRatio = 0.18f
    private val maxRadiusRatio = 0.52f
    private val minDotSize = 5f
    private val maxDotSize = 15f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2979FF.toInt() // Material3 파랑
        style = Paint.Style.FILL
    }
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600L // ★ 더 느리게 (원래 950ms → 1600ms)
        repeatCount = ValueAnimator.INFINITE
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            progress = it.animatedFraction
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxCircleRadius = width.coerceAtMost(height) / 2f - maxDotSize

        val basePhase = progress

        for (i in 0 until dotCount) {
            val angle = 2 * Math.PI / dotCount * i

            // 애니메이션이 진행되면서 radius/size 증가
            val radiusPhase = basePhase
            val radius = maxCircleRadius * (minRadiusRatio + (maxRadiusRatio - minRadiusRatio) * radiusPhase)
            val dotSize = minDotSize + (maxDotSize - minDotSize) * radiusPhase

            // ★ 중심에 가까울수록 alpha(투명), 바깥으로 갈수록 진해짐
            // minAlpha: 0.28(연하게), maxAlpha: 1f(불투명)
            val centerAlpha = 0.28f
            val edgeAlpha = 1f
            val alphaValue = centerAlpha + (edgeAlpha - centerAlpha) * radiusPhase

            // ★ 바깥에서 점점 사라지는 효과까지 함께 주기
            val fadeAlpha = if (radiusPhase > 0.8f) ((1f - radiusPhase) / 0.2f).coerceIn(0f, 1f) else 1f

            // ★ 최종 alpha = 중심~외곽 진해짐 × 바깥 fade out 효과
            val finalAlpha = alphaValue * fadeAlpha
            paint.alpha = (finalAlpha * 255).toInt()

            val x = (cx + cos(angle) * radius).toFloat()
            val y = (cy + sin(angle) * radius).toFloat()
            canvas.drawCircle(x, y, dotSize, paint)
        }
        // paint alpha 원복
        paint.alpha = 255
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
