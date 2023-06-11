package com.example.kidsdrawapp

import android.annotation.SuppressLint
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
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var brushBtn : ImageButton? = null
    private var mImageButtonCurrentPaint : ImageButton? =null

    var customProgressDialog : Dialog? = null
    val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground : ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }
    val requestPermission :ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(this , "Permission is granted for accessing the storage files" ,
                    Toast.LENGTH_LONG).show()

                    val pickIntent = Intent(Intent.ACTION_PICK ,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }else{
                    if(permissionName==android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity , "oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }
        }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        // FOE SETTING LIGHT MODE DEFAULT.
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20.toFloat())
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this ,R.drawable.pallet_selected)
        )
        brushBtn= findViewById(R.id.brush)
        brushBtn!!.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibRevert : ImageButton = findViewById(R.id.undo)
        ibRevert.setOnClickListener{
            drawingView!!.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.redoo)
        ibRedo.setOnClickListener{
            drawingView!!.onClickRedo()
        }

        val ibGallery : ImageButton = findViewById(R.id.gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ibSave : ImageButton = findViewById(R.id.save)
        ibSave.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_conatainer)
                    savedBitmapFile(getBitmapFromView(flDrawingView))

                }
            }
        }
        val ibShare : ImageButton = findViewById(R.id.share)
        ibShare.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_conatainer)
                    shareImage(savedBitmapFile(getBitmapFromView(flDrawingView)))
                }
            }
        }





    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("kids Drawing App" ,
                    "kids Drawing app needs to access your external Storage"
            )
        }else{
            requestPermission.launch(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE))
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this) /* if u want to add another dialog(another
         xml file) use  dialog */
        brushDialog.setContentView(R.layout.button_size_ops)
        brushDialog.setTitle("Brush Size : ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss() // used to dismiss view when clicked
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }
    fun paintClicked(view : View){
        Toast.makeText(this , "clicked Paint" , Toast.LENGTH_LONG).show()
        if (view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this ,R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat
                    .getDrawable(this , R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }

    private fun showRationaleDialog(
        title:String,
        message : String
    ){
        val builder :AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("you denied")
            .setPositiveButton("cancel"){dialog , _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width , view.height , Bitmap.Config.ARGB_8888)
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

    private suspend fun savedBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "kidsDrawing" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgreeDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved Successfully : $result", Toast.LENGTH_LONG
                            ).show()


                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something Went Wrong ", Toast.LENGTH_LONG
                            ).show()

                        }
                    }
                } catch (e: Exception) {
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
    private fun cancelProgreeDialog(){
        if(customProgressDialog != null){
            customProgressDialog!!.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this , arrayOf(result) , null){
            path , uri ->
            val shareIntentn = Intent()
            shareIntentn.action = Intent.ACTION_SEND
            shareIntentn.putExtra(Intent.EXTRA_STREAM , uri)
            shareIntentn.type = "image/png"
            startActivity(Intent.createChooser(shareIntentn , "share"))
        }
    }
}