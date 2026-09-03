package com.innova.launcher2kd.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom 3D Inclinometer View
 * Menampilkan horizon tanjakan/turunan dan sudut kemiringan mobil secara grafis dinamis.
 */
class InclinometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitchDeg = 0
    private var rollDeg = 0
    private var isWarning = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9E1B")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val carBox = RectF()

    fun setAngles(pitch: Int, roll: Int, warning: Boolean) {
        pitchDeg = pitch
        rollDeg = roll
        isWarning = warning
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f

        // Frame lingkaran luar
        val radius = Math.min(cx, cy) - 10f
        linePaint.color = if (isWarning) COLOR_WARNING else COLOR_NORMAL_LINE
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius, linePaint)

        // Crosshair vertikal dan horizontal
        canvas.drawLine(cx, cy - radius, cx, cy + radius, linePaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, linePaint)

        // Rotasi horizon berdasarkan Roll (kemiringan) dan pergeseran vertikal berdasarkan Pitch (tanjakan)
        canvas.save()
        val pitchShift = (pitchDeg * 2.5f).coerceIn(-radius * 0.7f, radius * 0.7f)
        canvas.translate(cx, cy - pitchShift)
        canvas.rotate(-rollDeg.toFloat())

        // Garis horizon mobil
        horizonPaint.color = if (isWarning) COLOR_WARNING else COLOR_NORMAL_HORIZON
        canvas.drawLine(-radius * 0.7f, 0f, radius * 0.7f, 0f, horizonPaint)

        // Siluet simbolis mobil Innova di tengah
        carBox.set(-24f, -12f, 24f, 12f)
        horizonPaint.style = Paint.Style.STROKE
        canvas.drawRoundRect(carBox, 4f, 4f, horizonPaint)

        canvas.restore()

        // Label teks sudut
        textPaint.color = if (isWarning) COLOR_WARNING else COLOR_TEXT_NORMAL
        val pitchSign = if (pitchDeg >= 0) "+$pitchDeg°" else "$pitchDeg°"
        val rollSign = if (rollDeg >= 0) "+$rollDeg°" else "$rollDeg°"
        canvas.drawText("PITCH: $pitchSign | ROLL: $rollSign", cx, h - 8f, textPaint)
    }

    companion object {
        private const val COLOR_WARNING = 0xFFEF4444.toInt()
        private const val COLOR_NORMAL_LINE = 0x2A38BDF8.toInt()
        private const val COLOR_NORMAL_HORIZON = 0xFFFF9E1B.toInt()
        private const val COLOR_TEXT_NORMAL = 0xFFE2E8F0.toInt()
    }
}
