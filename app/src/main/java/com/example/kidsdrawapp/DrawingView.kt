package com.example.kidsdrawapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.test.withTestContext

// we will use this class as a view : )
class DrawingView(context : Context , attrs : AttributeSet) : View(context , attrs) {
    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap : Bitmap? = null
    private var mCanvasPaint : Paint? = null
    private var mDrawPaint : Paint? = null
    private var color = Color.RED
    private var canvas : Canvas? = null
    private var mBrushSize : Float = 0.toFloat()
    private var mPaths = ArrayList<CustomPath>()  // for making lines persist after drawing
    private var mUndoPath = ArrayList<CustomPath>()

    init{
        setUpDrawing()
    }

    fun  onClickUndo(){
        if(mPaths.size>0) {
            mUndoPath.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    } fun onClickRedo(){
        if (mUndoPath.size>0) {
            mPaths.add(mUndoPath.removeAt(mUndoPath.size - 1))
            invalidate()
        }
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color , mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
       // mBrushSize = 20.toFloat()
    }
 //overriding functions of view class

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w , h , Bitmap.Config.ARGB_8888)

        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!! , 0f ,0f , mCanvasPaint)
        for(path in mPaths){ // for making the drawn lines persist
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path , mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness // determined by the coustom path class
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!! , mDrawPaint!!)

        }



    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY) // we can use !! too
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY) // we can use !! too
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color , mBrushSize)

            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize : Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP ,
        newSize , resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize  /*for setting brush size according to the
        screen size of the device we use DisplayMetric */
    }

    fun setColor(newColor : String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }



    internal inner class CustomPath(var color:Int , var brushThickness:Float ) : Path() {

    }


}