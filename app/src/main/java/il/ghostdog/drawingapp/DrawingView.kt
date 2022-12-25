package il.ghostdog.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    var mPaths = ArrayList<CustomPath>() //need to make private and val

    private var context1 : Context? = null

    private var circleCount : Int = 1

    val mOnDrawChange : Event<String> = Event()

    init {
        this.context1 = context
        setUpDrawing()
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            mPaths.removeAt(mPaths.size -1)
            mOnDrawChange.invoke("str")
            invalidate()
        }
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
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            //canvas.drawPath(path, mDrawPaint!!)
            path.drawCustomPath(canvas, mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty()) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            //canvas.drawPath(mDrawPath!!, mDrawPaint!!)
            mDrawPath!!.drawCustomPath(canvas, mDrawPaint!!)
        }

        //if(circleCount > 7) {
        //    circleCount = 1
        //    mOnDrawChange.invoke("str")
        //}else{
        //    circleCount++
        //}
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()
                if(touchX != null && touchY != null)
                {
                    mDrawPath!!.addPoint(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if(touchX != null && touchY != null)
                {
                    mDrawPath!!.addPoint(touchX, touchY)
                }
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)
                mOnDrawChange.invoke("str")
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
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

    @kotlinx.serialization.Serializable
    class CustomPath(var color: Int, var brushThickness: Float) { //need to make internal inner

        private val points : ArrayList<Vector2> = ArrayList<Vector2>()

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
    }
}