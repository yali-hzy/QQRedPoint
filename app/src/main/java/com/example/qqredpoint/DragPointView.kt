package com.example.qqredpoint

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.*


class DragPointView : View {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : this(context, attributeSet, defStyleAttr, 0)
    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : super(context, attributeSet, defStyleAttr, defStyleRes)

    private val DEFAULT_WIDTH = 24
    private val DEFAULT_HEIGHT = 24

    private val mPaint = Paint()
    private var isInRegion : Boolean = true
    private var originCircleCenter = PointD(500.0,500.0)
    private var dragCircleCenter = PointD(500.0,500.0)
    private val maxDragLength = 300.0
    private var ratio = 1.0
    private val dragCircleRadius: Double = 50.0
    private var originCircleRadius : Double = 50.0
    private val minRatio = 0.3
    private val mPath = Path()
    private var isDragged = false

    init {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.RED
        mPaint.isAntiAlias = true
        mPaint.isDither = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if(widthMode == MeasureSpec.AT_MOST) {
            widthSize = DEFAULT_WIDTH
        }
        if(heightMode == MeasureSpec.AT_MOST) {
            heightSize = DEFAULT_HEIGHT
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        originCircleCenter = PointD(w*0.5,h*0.5)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawCircle(canvas, dragCircleCenter, dragCircleRadius.toFloat())
        if(isInRegion && isDragged) {
            if(ratio<0.999)
                drawCircle(canvas, originCircleCenter, originCircleRadius.toFloat())
            drawBezierLine(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val eventPoint = PointD((event?.x ?: 0F).toDouble(),(event?.y ?: 0F).toDouble())
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                println("DOWN")
                isDragged = inCircle(eventPoint, originCircleCenter, originCircleRadius)
                println("drag = $isDragged event(${event.x},${event.y}) origin(${originCircleCenter.x}, ${originCircleCenter.y}), r = $originCircleRadius ratio = $ratio")
            }
            MotionEvent.ACTION_MOVE -> {
                println("MOVE drag = $isDragged")
                if(isDragged) {
                    dragCircleCenter = eventPoint
                    isInRegion = inCircle(eventPoint, originCircleCenter, maxDragLength)
                    updateRatio()
                    updateOriginCircleRadius()
                }
            }
            MotionEvent.ACTION_UP -> {
                println("UP")
                isInRegion = inCircle(eventPoint, originCircleCenter, maxDragLength)
                if(isInRegion) {
                    springBack()
                } else {
                    reset()
                }
            }
        }
        invalidate()
        return true
    }


    private fun inCircle(P: PointD, O: PointD, r: Double) = dist(P,O)<r

    private fun springBack() {
        val animator = ValueAnimator.ofFloat(1.0f)
        val startPoint = dragCircleCenter
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            dragCircleCenter = getPointDByPercent(startPoint, originCircleCenter, fraction.toDouble())
            updateRatio()
            updateOriginCircleRadius()
            println("drag=$isDragged, region =$isInRegion")
            invalidate()
        }
        animator.interpolator = OvershootInterpolator(4.0f)
        animator.duration = 500
        animator.start()
    }

    override fun onAnimationEnd() {
        reset()
        super.onAnimationEnd()
    }

    fun reset() {
        println("reset")
        dragCircleCenter = originCircleCenter
        updateRatio()
        updateOriginCircleRadius()
        isInRegion = true
        isDragged = false
        println("ratio = $ratio, r = $originCircleRadius")
    }

    private fun drawCircle(canvas: Canvas?, center: PointD, radius: Float) {
        canvas?.drawCircle(center.x.toFloat(), center.y.toFloat(), radius, mPaint)
    }

    private fun updateRatio() {
        ratio = max(minRatio,(maxDragLength-lengthOfVector(dragCircleCenter-originCircleCenter))/maxDragLength)
    }

    private fun updateOriginCircleRadius() {
        originCircleRadius = dragCircleRadius * ratio
    }

    /**
     * 别问我为什么写的这么复杂，因为我不想分类讨论切线怎么画
     */
    private fun drawBezierLine(canvas: Canvas?) {
        println("drawZezierLine")
        val OO = originCircleCenter - dragCircleCenter
        println("R=${dragCircleRadius}, r=${originCircleRadius}")

        val cosTheta = sqrt(square(dragCircleRadius-originCircleRadius) / square(OO))
        val sinTheta = sqrt((square(OO) - square(dragCircleRadius-originCircleRadius))/ square(OO))
        println("cos=${cosTheta},sin=${sinTheta})");

        val direction1 = normalizeVector(OO.spin(cosTheta, sinTheta))
        val direction2 = normalizeVector(OO.spin(cosTheta, -sinTheta))

        println("R=$dragCircleRadius, r=$originCircleRadius")
        println("dir1=(${direction1.x},${direction1.y})");
        println("dir2=(${direction2.x},${direction2.y})");


        val p1 = direction1 * originCircleRadius + originCircleCenter
        val p2 = direction2 * originCircleRadius + originCircleCenter
        val p3 = direction1 * dragCircleRadius + dragCircleCenter
        val p4 = direction2 * dragCircleRadius + dragCircleCenter
        println("p1=(${p1.x},${p1.y})");
        println("p2=(${p2.x},${p2.y})");
        println("p3=(${p3.x},${p3.y})");
        println("p4=(${p4.x},${p4.y})");

        val pointTemp1 = getPointDByPercent(p3, p4,1 - ratio)
        val pointTemp2 = getPointDByPercent(p4, p3,1 - ratio)
        val controlPoint1 = middlePoint(p1, pointTemp1)
        val controlPoint2 = middlePoint(p2, pointTemp2)

        mPath.reset()
        mPath.moveTo(p1.x.toFloat(),p1.y.toFloat())
        mPath.quadTo(controlPoint1.x.toFloat(), controlPoint1.y.toFloat(), p3.x.toFloat(), p3.y.toFloat())
//        mPath.lineTo(p3.x.toFloat(),p3.y.toFloat())
        mPath.lineTo(p4.x.toFloat(), p4.y.toFloat())
        mPath.quadTo(controlPoint2.x.toFloat(), controlPoint2.y.toFloat(), p2.x.toFloat(), p2.y.toFloat())
//        mPath.lineTo(p2.x.toFloat(),p2.y.toFloat())
        mPath.close()
//        mPaint.color = Color.CYAN
        canvas?.drawPath(mPath, mPaint)
    }

    /**
     * @return (A + B) /2
     */
    private fun middlePoint(A: PointD, B: PointD) = (A+B)/2.0

    /**
     * @return x^2
     */
    private fun square(x: Double) = x * x
    private fun square(P: PointD) = square(P.x) + square(P.y)

    /**
     * @return |P|
     */
    private fun lengthOfVector(P: PointD) = sqrt(square(P))

    /**
     * @return distance between P and Q
     */
    private fun dist(P: PointD, Q: PointD) = lengthOfVector(P - Q)

    /**
     * @return P/|P|
     */
    private fun normalizeVector(P: PointD) = P.div(lengthOfVector(P))


    private fun getPointDByPercent(startPoint: PointD, endPoint: PointD, ratio: Double) = startPoint * (1-ratio) + endPoint * ratio
}

