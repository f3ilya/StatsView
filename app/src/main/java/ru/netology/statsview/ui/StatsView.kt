package ru.netology.statsview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.util.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)
    private var lineWidth = AndroidUtils.dp(context, 5F).toFloat()
    private var fontSize = AndroidUtils.dp(context, 40F).toFloat()
    private var colors = emptyList<Int>()
    private var progress = 0F
    private var valueAnimator: ValueAnimator? = null
    private var animationType = AnimationType.PARALLEL

    init {
        context.withStyledAttributes(attrs, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()
            val animType = getInt(R.styleable.StatsView_animationType, AnimationType.PARALLEL.value)
            animationType = AnimationType.fromInt(animType)
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = fontSize
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth / 2
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }

        val total = data.sum()
        val displayPercent = if (total > 0) progress * 100F else 0F

        val rotationAngle = 360F * progress
        var startFrom = -90F + rotationAngle
        if (total > 0) {
            when (animationType) {
                AnimationType.PARALLEL -> {
                    data.forEachIndexed { index, datum ->
                        val angle = 360F * (datum / total)
                        val sweepAngle = angle * progress
                        paint.color = colors.getOrNull(index) ?: randomColor()
                        canvas.drawArc(oval, startFrom, sweepAngle, false, paint)
                        startFrom += angle
                    }
                }

                AnimationType.SEQUENTIAL -> {
                    var accumulatedAngle = 0F
                    data.forEachIndexed { index, datum ->
                        val angle = 360F * (datum / total)
                        paint.color = colors.getOrNull(index) ?: randomColor()
                        val sweepAngle = when {
                            rotationAngle <= accumulatedAngle -> 0F
                            rotationAngle >= accumulatedAngle + angle -> angle
                            else -> rotationAngle - accumulatedAngle
                        }
                        if (sweepAngle > 0F) {
                            canvas.drawArc(oval, startFrom, sweepAngle, false, paint)
                        }
                        startFrom += angle
                        accumulatedAngle += angle
                    }
                }

                AnimationType.BIDIRECTIONAL -> {
                    data.forEachIndexed { index, datum ->
                        val angle = 360F * (datum / total)
                        val halfAngle = angle / 2F
                        val startAngle = startFrom + halfAngle
                        val sweepAngle = halfAngle * progress
                        paint.color = colors.getOrNull(index) ?: randomColor()
                        canvas.drawArc(oval, startAngle, sweepAngle, false, paint)
                        canvas.drawArc(oval, startAngle, -sweepAngle, false, paint)
                        startFrom += angle
                    }
                }
            }
            if (data.isNotEmpty() && progress >= 0.9F) {
                paint.color = colors.getOrNull(0) ?: randomColor()
                canvas.drawArc(oval, startFrom, 1F, false, paint)
            }
        } else {
            val oldColor = paint.color
            paint.color = Color.LTGRAY
            canvas.drawArc(oval, startFrom, 360F, false, paint)
            paint.color = oldColor
        }

        canvas.drawText(
            "%.2f%%".format(displayPercent),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint,
        )
    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
        }.also {
            it.start()
        }
    }

    private fun randomColor(): Int = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

    enum class AnimationType(val value: Int) {
        PARALLEL(0),
        SEQUENTIAL(1),
        BIDIRECTIONAL(2);

        companion object {
            fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: PARALLEL
        }
    }
}