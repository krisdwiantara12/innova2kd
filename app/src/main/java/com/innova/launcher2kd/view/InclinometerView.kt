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
        linePaint.color = if (isWarning) Color.parseColor("#EF4444") else Color.parseColor("#2A38BDF8")
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius, linePaint)

        // Crosshair vertikal dan horizontal
        canvas.drawLine(cx, cy - radius, cx, cy + radius, linePaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, linePaint)

        // Rotasi horizon berdasarkan Roll (kemiringan) dan pergeseran vertikal berdasarkan Pitch (tanjakan)
        canvas.save()
        // Pitch shift: 1 derajat = 2.5 pixel vertikal
        val pitchShift = (pitchDeg * 2.5f).coerceIn(-radius * 0.7f, radius * 0.7f)
        canvas.translate(cx, cy - pitchShift)
        canvas.rotate(-rollDeg.toFloat())

        // Garis horizon mobil
        horizonPaint.color = if (isWarning) Color.parseColor("#EF4444") else Color.parseColor("#FF9E1B")
        canvas.drawLine(-radius * 0.7f, 0f, radius * 0.7f, 0f, horizonPaint)

        // Siluet simbolis mobil Innova di tengah
        carBox.set(-24f, -12f, 24f, 12f)
        horizonPaint.style = Paint.Style.STROKE
        canvas.drawRoundRect(carBox, 4f, 4f, horizonPaint)

        canvas.restore()

        // Label teks sudut
        textPaint.color = if (isWarning) Color.parseColor("#EF4444") else Color.parseColor("#E2E8F0")
        val pitchSign = if (pitchDeg >= 0) "+$pitchDeg°" else "$pitchDeg°"
        val rollSign = if (rollDeg >= 0) "+$rollDeg°" else "$rollDeg°"
        canvas.drawText("PITCH: $pitchSign | ROLL: $rollSign", cx, h - 8f, textPaint)
    }
}
