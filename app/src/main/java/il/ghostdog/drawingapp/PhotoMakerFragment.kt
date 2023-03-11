package il.ghostdog.drawingapp

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape


class PhotoMakerFragment : Fragment(R.layout.fragment_photo_maker) {
    private lateinit var drawingView: DrawingView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawingView = view.findViewById(R.id.dvDrawingView)
        drawingView.setSizeForBrush(6.toFloat())
        drawingView.canDraw = true

        val btnBrushSize = view.findViewById<ImageButton>(R.id.ibBrush)
        btnBrushSize.setOnClickListener{ showBrushSizeChooserDialog() }

        val ibUndo: ImageButton = view.findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener { drawingView.onClickUndo() }

        val btnColor: ImageButton = view.findViewById(R.id.ibColor)
        btnColor.setOnClickListener { showColorPickerDialog() }
    }

    private fun showColorPickerDialog(){
        ColorPickerDialog
            .Builder(activity!!)
            .setTitle("Pick Theme")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(Color.BLACK)
            .setColorListener { color, colorHex ->
                drawingView.setColor(colorHex)
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
}