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

    val mOnDrawChange : Event<Int> = Event()

    init {
        this.context1 = context
        setUpDrawing()
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            mPaths.removeAt(mPaths.size -1)
            mOnDrawChange.invoke(mPaths.size)
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
            mDrawPaint!!.strokeWidth = path.bs
            mDrawPaint!!.color = path.c
            path.drawCustomPath(canvas, mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty()) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.bs
            mDrawPaint!!.color = mDrawPath!!.c
            mDrawPath!!.drawCustomPath(canvas, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.c = color
                mDrawPath!!.bs = mBrushSize
                mDrawPath!!.reset()
                if(touchX != null && touchY != null)
                {
                    mDrawPath!!.addPoint(touchX, touchY)
                    mPaths.add(mDrawPath!!)
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if(touchX != null && touchY != null)
                {
                    mPaths.removeAt(mPaths.size - 1)
                    mDrawPath!!.addPoint(touchX, touchY)
                    mPaths.add(mDrawPath!!)
                }
            }
            MotionEvent.ACTION_UP ->{
                //mPaths.add(mDrawPath!!)
                mOnDrawChange.invoke(mPaths.size)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        if(circleCount > 10) {
            circleCount = 1
            mOnDrawChange.invoke(mPaths.size)
        }else{
            circleCount++
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
    class CustomPath(var c: Int, var bs: Float) { //need to make internal inner

        private val ps : ArrayList<Vector2> = ArrayList<Vector2>() //name - point

        fun isEmpty() : Boolean{
            return ps.isEmpty()
        }

        fun reset(){
            ps.clear()
        }

        fun addPoint(x : Float, y : Float){
            val vector = Vector2(x.toInt() , y.toInt())
            ps.add(vector)
        }

        fun drawCustomPath(canvas: Canvas, paint: Paint) {
            var count = 0
            while(count < ps.size - 1){
                canvas.drawLine(ps[count].x.toFloat(), ps[count].y.toFloat()
                    , ps[count+1].x.toFloat(), ps[count+1].y.toFloat(), paint)
                count++
            }
        }
    }
}