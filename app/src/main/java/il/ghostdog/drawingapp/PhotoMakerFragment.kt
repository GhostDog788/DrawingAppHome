package il.ghostdog.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape


class PhotoMakerFragment : Fragment(R.layout.fragment_photo_maker) {

    private lateinit var drawingView: DrawingView
    private lateinit var mflDrawingView: FrameLayout
    private lateinit var mBtnColor: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawingView = view.findViewById(R.id.dvDrawingView)
        drawingView.setSizeForBrush(6.toFloat())
        drawingView.canDraw = true

        mflDrawingView = view.findViewById(R.id.flDrawingViewContainer)

        val btnBrushSize = view.findViewById<ImageButton>(R.id.ibBrush)
        btnBrushSize.setOnClickListener{ showBrushSizeChooserDialog() }

        val ibUndo: ImageButton = view.findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener { drawingView.onClickUndo() }

        mBtnColor = view.findViewById(R.id.ibColor)
        mBtnColor.setOnClickListener { showColorPickerDialog() }

        val btnUploadPhoto : Button = view.findViewById(R.id.btnUploadPhoto)
        btnUploadPhoto.setOnClickListener{ uploadPhoto()}

        val btnClear : Button = view.findViewById(R.id.btnClear)
        btnClear.setOnClickListener{ clear()}
    }

    private fun clear() {
        val imageBackground: ImageView = mflDrawingView.findViewById(R.id.ivBackground)
        imageBackground.setImageResource(0)
        drawingView.clear()
    }
    fun getBackgroundImageView() : ImageView{
        return mflDrawingView.findViewById(R.id.ivBackground)
    }

    private fun uploadPhoto() {
        requestStoragePermission()
    }
    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                activity!!,
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
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted =it.value

                if(isGranted){
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            activity!!,
                            "Permission granted to read files",
                            Toast.LENGTH_SHORT
                        ).show()
                        val pickIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        openGalleryLauncher.launch(pickIntent)
                    }
                }else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(activity!!, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null){
                val imageBackground: ImageView = mflDrawingView.findViewById(R.id.ivBackground)
                imageBackground.setImageURI(result.data?.data)
            }
        }
    private fun showRationalDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity!!)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showColorPickerDialog(){
        ColorPickerDialog
            .Builder(activity!!)
            .setTitle("Pick Theme")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(Color.BLACK)
            .setColorListener { color, colorHex ->
                drawingView.setColor(colorHex)
                mBtnColor.setColorFilter(color)
            }
            .show()
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(activity!!)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val button1: ImageView = brushDialog.findViewById(R.id.ibBrush1)
        button1.setOnClickListener{
            drawingView.setSizeForBrush(3.toFloat())
            brushDialog.dismiss()
        }
        val button2: ImageView = brushDialog.findViewById(R.id.ibBrush2)
        button2.setOnClickListener{
            drawingView.setSizeForBrush(6.toFloat())
            brushDialog.dismiss()
        }
        val button3: ImageView = brushDialog.findViewById(R.id.ibBrush3)
        button3.setOnClickListener{
            drawingView.setSizeForBrush(9.toFloat())
            brushDialog.dismiss()
        }
        val button4: ImageView = brushDialog.findViewById(R.id.ibBrush4)
        button4.setOnClickListener{
            drawingView.setSizeForBrush(13.toFloat())
            brushDialog.dismiss()
        }
        val button5: ImageView = brushDialog.findViewById(R.id.ibBrush5)
        button5.setOnClickListener{
            drawingView.setSizeForBrush(21.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }
    fun getBitmapFromView() : Bitmap {
        val view = mflDrawingView
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
}