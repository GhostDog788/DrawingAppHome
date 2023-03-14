package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Context

interface IProgressDialogUser {
    var customProgressDialog: Dialog?

    fun showProgressDialog(context: Context){
        customProgressDialog = Dialog(context)
        customProgressDialog?.setCancelable(false)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}