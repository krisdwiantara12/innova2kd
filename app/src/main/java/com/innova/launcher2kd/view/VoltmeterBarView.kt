package com.innova.launcher2kd.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class VoltmeterBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentVoltage = 12.6f
    private var displayedVoltage = 12.6f
    private var animator: ValueAnimator? = null

    private val minV = 10.0f
    private val maxV = 16.0f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
        style = Paint.Style.FILL
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        strokeWidth = 2f
    }

    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    fun setVoltage(v: Float) {
        val clamped = v.coerceIn(minV, maxV)
        if (clamped == currentVoltage) return
        currentVoltage = clamped

        animator?.cancel()
        animator = ValueAnimator.ofFloat(displayedVoltage, currentVoltage).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayedVoltage = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val barH = h * 0.45f
        val top = (h - barH) / 2f
        val bottom = top + barH

        // Draw track background with rounded corners
        rect.set(0f, top, w, bottom)
        canvas.drawRoundRect(rect, 6f, 6f, trackPaint)

        // Draw segmented colored zones
        fun xForV(v: Float): Float = ((v - minV) / (maxV - minV)) * w

        // Zone 1: < 11.8V (Danger Red)
        zonePaint.color = Color.parseColor("#4DEE4444")
        canvas.drawRect(xForV(10.0f), top, xForV(11.8f), bottom, zonePaint)

        // Zone 2: 11.8V - 12.4V (Amber Standby)
        zonePaint.color = Color.parseColor("#4DF59E0B")
        canvas.drawRect(xForV(11.8f), top, xForV(12.4f), bottom, zonePaint)

        // Zone 3: 12.5V - 14.8V (Optimal Green)
        zonePaint.color = Color.parseColor("#4D10B981")
        canvas.drawRect(xForV(12.4f), top, xForV(14.8f), bottom, zonePaint)

        // Zone 4: > 14.8V (Overcharge Red)
        zonePaint.color = Color.parseColor("#4DEF4444")
        canvas.drawRect(xForV(14.8f), top, xForV(16.0f), bottom, zonePaint)

        // Draw Voltage Ticks (11, 12, 13, 14, 15)
        for (v in 11..15) {
            val tx = xForV(v.toFloat())
            canvas.drawLine(tx, top, tx, bottom, tickPaint)
        }

        // Draw Active Needle Indicator
        val needleX = xForV(displayedVoltage).coerceIn(4f, w - 4f)
        needlePaint.color = when {
            displayedVoltage < 11.8f -> Color.parseColor("#EF4444")
            displayedVoltage < 12.4f -> Color.parseColor("#F59E0B")
            displayedVoltage <= 14.8f -> Color.parseColor("#10B981")
            else -> Color.parseColor("#EF4444")
        }

        // Needle Glow Head
        canvas.drawCircle(needleX, h / 2f, h * 0.35f, needlePaint)
    }
}
