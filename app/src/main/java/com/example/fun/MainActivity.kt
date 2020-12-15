package com.example.`fun`

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity(){
   var mImageCurrentPaint : ImageButton? =  null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawing_view.setSizeForBrush(20.toFloat())

        mImageCurrentPaint = ll_paint_pallet[1] as ImageButton
        mImageCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        iv_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        iv_background_image_selector.setOnClickListener {
            if(isReadStorageAllowed()){
                val selectImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(selectImageIntent, GALLERY) //GALLERY IS A COMPANION OBJECT
            }else{
                requestStoragePermission()
            }
        }

        iv_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }

        iv_redo.setOnClickListener {
            drawing_view.onClickRedo()
        }

        iv_save.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(drawing_view)).execute()
            }else{
                requestStoragePermission()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK)
            if(requestCode == GALLERY)
            {
                try {
                    if(data!!.data != null){
                        Toast.makeText(this, "onActivity result", Toast.LENGTH_SHORT).show()
                        val imageUri = data.data
                        val bMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri!!))
                        } else {
                            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        }
                        val bMapScaled = Bitmap.createScaledBitmap(bMap, iv_background.width, iv_background.height, true)
                        BitmapScaler.scaleToFitHeight(bMapScaled, iv_background.height)
                        BitmapScaler.scaleToFitWidth(bMapScaled, iv_background.width)
                        iv_background.setImageBitmap(bMapScaled)

                    }else{
                        Toast.makeText(this, "Error in parsing the image or its corrupted", Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener{
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener{
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if(mImageCurrentPaint !== view){

            val colorTag = view as ImageButton
            val newColor = colorTag.tag.toString()
            drawing_view.setColorForPaint(newColor)
            mImageCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageCurrentPaint = colorTag
            mImageCurrentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )


        }
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>(){

        override fun doInBackground(vararg params: Any?): String {

            var result = ""

            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.isNotEmpty()){
                Toast.makeText(this@MainActivity, "File saved Successfully : $result", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this@MainActivity, "Something got wrong while saving the file ", Toast.LENGTH_SHORT).show()
            }
        }

    }
    //TODO(Step 4 - For the first time you need to ask for the permission
    // for selecting the image from your phone or when it is not allowed it is when you are about to select an image from phone storage.)
    //START
    /**
     * Requesting permission
     */
    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).toString()
                )
        ) {
            Toast.makeText(this, "Please provide storage permission if you want to change background", Toast.LENGTH_LONG).show()
        }

        /**
         * Requests permissions to be granted to this application. These permissions
         * must be requested in your manifest, otherwise they will not be granted to your app.
         */

        //And finally ask for the permission
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
                STORAGE_PERMISSION_CODE
        )
    }
    //END

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty() && grantResults[0] == STORAGE_PERMISSION_CODE){
            Toast.makeText(this@MainActivity, "Permission Granted...Now you can add background of your choice", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "on request permission result", Toast.LENGTH_LONG).show()
        }
    }

    //TODO(Step 7 - After giving an permission in Manifest file check that is it allowed or not for selecting the image from your phone)
    //START
    /**
     * We are calling this method to check the permission status
     */
    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
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
    companion object{


        /**
         * Permission code that will be checked in the method onRequestPermissionsResult
         *
         * For more Detail visit : https://developer.android.com/training/permissions/requesting#kotlin
         */
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
        //END
    }
}