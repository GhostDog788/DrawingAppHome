package il.ghostdog.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.log

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    var mPaths = ArrayList<CustomPath>()

    private var context1 : Context? = null
    var canDraw : Boolean = false

    private var circleCount : Int = 1

    val mOnDrawChange : Event<Int> = Event()

    init {
        this.context1 = context

        setUpDrawing()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushSize
            mDrawPaint!!.color = path.color
            path.drawCustomPath(canvas, mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty()) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushSize
            mDrawPaint!!.color = mDrawPath!!.color
            mDrawPath!!.drawCustomPath(canvas, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //Toast.makeText(context1, "$canDraw", Toast.LENGTH_SHORT).show()
        if(!canDraw) return true

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushSize = mBrushSize
                mDrawPath!!.reset()
                if(touchX != null && touchY != null)
                {
                    mDrawPath!!.addPoint(touchX, touchY)
                    mPaths.add(mDrawPath!!)
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if(mPaths.size > 0 && touchX != null && touchY != null && pointOnView(touchX, touchY))
                {
                    mPaths.removeAt(mPaths.size - 1)
                    mDrawPath!!.addPoint(touchX, touchY)
                    mPaths.add(mDrawPath!!)
                }
            }
            MotionEvent.ACTION_UP ->{
                if(mPaths.size > 0) {
                    mOnDrawChange.invoke(mPaths.size)
                    mDrawPath = CustomPath(color, mBrushSize)
                }
            }
            else -> return false
        }
        if(circleCount > 4 && mPaths.size > 0) {
            circleCount = 1
            mOnDrawChange.invoke(mPaths.size)
        }else{
            circleCount++
        }
        invalidate()

        return true
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            mPaths.removeAt(mPaths.size -1)
            mOnDrawChange.invoke(mPaths.size)
            invalidate()
        }
    }

    private fun pointOnView(touchX: Float, touchY: Float): Boolean {
        return touchX >= 0 &&
                touchX <= this.width &&
                touchY >= 0 &&
                touchY <= this.height
    }

    fun setSizeForBrush(newSize: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    fun clear(){
        mPaths.clear()
        mDrawPath!!.reset()
        invalidate()
    }

    class CustomPath(var color: Int, var brushSize: Float) {

        private val points : ArrayList<Vector2> = ArrayList<Vector2>()

        constructor(standardPath: StandardPath) : this(standardPath.c, standardPath.bs) {
            val multiplier : Float = Constants.viewWidth.toFloat() / Constants.defaultWidth.toFloat()
            brushSize *= multiplier

            for (point in standardPath.ps){
                val vector = Vector2(point.x, point.y)
                vector.x = (multiplier * vector.x).toInt()
                vector.y = (multiplier * vector.y).toInt()
                points.add(vector)
            }
        }

        fun isEmpty() : Boolean{
            return points.isEmpty()
        }

        fun reset(){
            points.clear()
        }

        fun addPoint(x : Float, y : Float){
            val vector = Vector2(x.toInt() , y.toInt())
            points.add(vector)
        }

        fun drawCustomPath(canvas: Canvas, paint: Paint) {
            var count = 0
            while(count < points.size - 1){
                canvas.drawLine(points[count].x.toFloat(), points[count].y.toFloat()
                    , points[count+1].x.toFloat(), points[count+1].y.toFloat(), paint)
                count++
            }
        }

        fun toStandardPath() : StandardPath{
            val multiplier : Float = Constants.defaultWidth.toFloat() / Constants.viewWidth.toFloat()

            val standardPath = StandardPath(color, brushSize * multiplier)

            for (point in points){
                val vector = Vector2(point.x, point.y)
                vector.x = (multiplier * vector.x).toInt()
                vector.y = (multiplier * vector.y).toInt()
                standardPath.ps.add(vector)
            }

            return standardPath
        }
    }
    @kotlinx.serialization.Serializable
    class StandardPath(var c: Int, var bs: Float) {
        val ps : ArrayList<Vector2> = ArrayList<Vector2>() //name - point
    }
}