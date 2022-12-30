package il.ghostdog.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

    private var mDatabaseInstance: FirebaseDatabase? = null
    private var mDatabase: DatabaseReference? = null
    var mflDrawingView: FrameLayout? = null
    private var count : Int = 0
    private var dbPathsCount : Int = 0
    private var mAuth : FirebaseAuth? = null
    private var mDrawerUid: String? = null

    val pathsChildListener = object  : ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if(dbPathsCount > drawingView!!.mPaths.size) {
                val encoded = snapshot.getValue(String::class.java) ?: return
                val data = ungzip(encoded)
                val path: DrawingView.StandardPath = Json.decodeFromString(data)
                val customPath = DrawingView.CustomPath(path)
                drawingView!!.mPaths.add(customPath)

                drawingView!!.invalidate()
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val encoded = snapshot.getValue(String::class.java) ?: return
            val data = ungzip(encoded)
            val path: DrawingView.StandardPath = Json.decodeFromString(data)
            val customPath = DrawingView.CustomPath(path)
            drawingView!!.mPaths[snapshot.key!!.toInt()] = customPath

            drawingView!!.invalidate()
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            if(dbPathsCount < drawingView!!.mPaths.size) {
                drawingView!!.mPaths.removeAt(snapshot.key!!.toInt())

                drawingView!!.invalidate()
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                if (result.resultCode == RESULT_OK && result.data != null){
                    val imageBackground: ImageView = findViewById(R.id.ivBackground)
                    imageBackground.setImageURI(result.data?.data)
                }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
                permissions.entries.forEach{
                    val permissionName = it.key
                    val isGranted =it.value

                    if(isGranted){
                        Toast.makeText(this, "Permission granted to read files", Toast.LENGTH_SHORT).show()
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)

                    }else{
                        if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.dvDrawingView)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val widthR = displayMetrics.widthPixels
        (drawingView!!.parent as View).updateLayoutParams{
            height = (widthR * 1.47).toInt()
            width = (widthR - widthR * 0.02).toInt()
            Constants.viewWidth = width
            Toast.makeText(applicationContext, widthR.toString(), Toast.LENGTH_SHORT).show()
        }
        drawingView?.setSizeForBrush(12.toFloat())

        mAuth = FirebaseAuth.getInstance()
        drawingView?.mOnDrawChange!!.plusAssign(::handleDrawChange)
        mDatabaseInstance = FirebaseDatabase.getInstance()
        mDatabase = mDatabaseInstance!!.getReference("paths")
        mflDrawingView = findViewById(R.id.flDrawingViewContainer)
        setupGame() //have to be before listeners
        addDrawerIdListener()
        addPathsValueListener()

        //left to add auth to have uid foreach device
        //there will be a variable that holds the uid of the drawing user
        //the variable will change to the latest user that touched the screen
        //add listener to the variable that holds the uid of the drawer
        //on uid of drawer changed:
        //if the uid on the db is equal to the uid of the current user -> removePathsValueListener
        //if the uid on the db is not equal to the uid of the current user -> addPathsValueListener
        //limitations: can't draw on two screens at once

        addPathsCountListener()

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.llPaintColors)
        mImageButtonCurrentPaint = linearLayoutPaintColors.findViewWithTag("#ff000000") as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))

        val ibBrush: ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        val ibGallery: ImageButton = findViewById(R.id.ibGallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }
        val ibUndo: ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }
        val ibSave: ImageButton = findViewById(R.id.ibSave)
        ibSave.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.flDrawingViewContainer)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }
    }

    private fun setupGame() {
        mDatabase!!.setValue("")
        mDatabaseInstance!!.getReference("PathsCount").setValue(0)
    }

    private fun addPathsValueListener() {
        mDatabase!!.addChildEventListener(pathsChildListener)
    }
    private fun removePathsValueListener() {
        mDatabase!!.removeEventListener(pathsChildListener)
    }

    private fun addDrawerIdListener() {
        mDatabaseInstance!!.getReference("drawerID").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                mDrawerUid = snapshot.getValue(String::class.java)

                if(mAuth!!.currentUser!!.uid == mDrawerUid)
                {
                    removePathsValueListener()
                    return
                }else{
                    addPathsValueListener()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun addPathsCountListener() {
        mDatabaseInstance!!.getReference("PathsCount").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                dbPathsCount = snapshot.getValue(Long::class.java)!!.toInt()

                if(dbPathsCount == null)
                {
                    Toast.makeText(applicationContext, "Error: paths count is null", Toast.LENGTH_SHORT).show()
                    dbPathsCount = 0
                    return
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun handleDrawChange(myPathsCount : Int) : Unit{
        count++
        mDatabaseInstance!!.getReference("count").setValue(count)

        if(mAuth!!.currentUser!!.uid != mDrawerUid) {
            mDatabaseInstance!!.getReference("drawerID").setValue(mAuth!!.currentUser!!.uid)
        }

        if(dbPathsCount != myPathsCount){
            mDatabaseInstance!!.getReference("PathsCount").setValue(myPathsCount)
        }

        if(dbPathsCount < myPathsCount){
            var counter = dbPathsCount
            while(counter < myPathsCount){
                val path = drawingView!!.mPaths[counter]
                val standardPath = path.toStandardPath()
                val data = Json.encodeToString(standardPath)
                val compressedData = gzip(data)

                mDatabase!!.child(counter.toString()).setValue(compressedData)
                counter++
            }
        }else if(dbPathsCount > myPathsCount){
            var counter = myPathsCount
            while(counter < dbPathsCount) {
                mDatabase!!.child(counter.toString()).removeValue()
                counter++
            }
        }else{
            val path = drawingView!!.mPaths[myPathsCount - 1]
            val standardPath = path.toStandardPath()
            val data = Json.encodeToString(standardPath)
            val compressedData = gzip(data)


            mDatabase!!.child((myPathsCount - 1).toString()).setValue(compressedData)
        }
    }

    fun gzip(content: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        val b = bos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun ungzip(content: String): String =
        GZIPInputStream(Base64.decode(content, Base64.DEFAULT).inputStream()).bufferedReader(UTF_8).use { it.readText() }

    private fun BitMapToString(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("Permission denied - access storage"
                ,"Can not access storage of device for functions of the device for the application" )
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val button1: ImageView = brushDialog.findViewById(R.id.ibBrush1)
        button1.setOnClickListener{
            drawingView?.setSizeForBrush(3.toFloat())
            brushDialog.dismiss()
        }
        val button2: ImageView = brushDialog.findViewById(R.id.ibBrush2)
        button2.setOnClickListener{
            drawingView?.setSizeForBrush(7.toFloat())
            brushDialog.dismiss()
        }
        val button3: ImageView = brushDialog.findViewById(R.id.ibBrush3)
        button3.setOnClickListener{
            drawingView?.setSizeForBrush(12.toFloat())
            brushDialog.dismiss()
        }
        val button4: ImageView = brushDialog.findViewById(R.id.ibBrush4)
        button4.setOnClickListener{
            drawingView?.setSizeForBrush(21.toFloat())
            brushDialog.dismiss()
        }
        val button5: ImageView = brushDialog.findViewById(R.id.ibBrush5)
        button5.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view != mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private fun showRationalDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    var f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath
                    
                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}