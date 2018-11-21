package com.jain.ullas.imageblurdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import android.view.SurfaceHolder
import org.opencv.core.Core
import org.opencv.core.MatOfDouble


class MainActivity : AppCompatActivity(), MainActivityView
//        , SurfaceHolder.Callback
{

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1001
    }

    private var camera : Camera? = null
    private lateinit var presenter: MainPresenter
    private lateinit var matImage: Mat
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var jpegCallback : Camera.PictureCallback
    private lateinit var rawCallback : Camera.PictureCallback
    private lateinit var shutterCallback: Camera.ShutterCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainPresenter(this)
//        surfaceHolder = surfaceView.holder
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
//        surfaceHolder.addCallback(this)
//
//        jpegCallback = Camera.PictureCallback { data, camera ->
//            var outStream : FileOutputStream? = null
//            try {
//                outStream = FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()))
//                outStream.write(data)
//                outStream.close()
//                Log.d("Log", "onPictureTaken - wrote bytes: " + data.size)
//            }catch (e : Exception) {
//                e.printStackTrace()
//            }
//            showToast("Picture Saved")
//            refreshCamera()
//        }
    }
//
//    @Throws(Exception::class)
//    fun captureImage(v: View) {
//        //take the picture
//        camera?.takePicture(null, null, jpegCallback)
//    }
//
//    @Throws(Exception::class)
//    private fun refreshCamera() {
//        if (surfaceHolder.surface == null) {
//            // preview surface does not exist
//            return
//        }
//        try {
//            camera?.stopPreview()
//            camera?.setPreviewDisplay(surfaceHolder)
//            camera?.startPreview()
//        }catch (e : Exception) {
//            e.printStackTrace()
//        }
//
//    }
//
//    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
//        refreshCamera()
//    }
//
//    override fun surfaceDestroyed(holder: SurfaceHolder?) {
//        camera?.stopPreview();
//        camera?.release();
//        camera = null;
//    }
//
//    override fun surfaceCreated(holder: SurfaceHolder?) {
//        try {
//            camera = Camera.open();
//        } catch (e : RuntimeException) {
//            System.err.println(e);
//            return;
//        }
//        val param :  Camera.Parameters? = camera?.parameters
//        param?.setPreviewSize(352, 288);
//        camera?.setParameters(param);
//        try {
//            camera?.setPreviewDisplay(surfaceHolder);
//            camera?.startPreview();
//        } catch (e : Exception) {
//            e.printStackTrace()
//            return;
//        }
//    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pick_gallery_image, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_gallery -> {
                launchGalleryToPickPhoto()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK && null != data) {
                extractImageBitmapFromIntentData(data)
            }
        }
    }

    private fun extractImageBitmapFromIntentData(galleryIntentData: Intent) {
        showLoading()
        try {
            val imageUri = galleryIntentData.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            presenter.getDataFromImageBitmap(bitmap)
        } catch (e: Exception) {
            hideLoading()
            Log.e(MainActivity::class.java.name, "Error",e)
        }
    }

    override fun onSuccessfulScan(bitmap: Bitmap?) {
        try {
            bitmap?.let {
                when(isImageBlurred(it)) {
                    true -> showToast("BLURRED IMAGE")
                    false -> showToast("NOT blurred IMAGE")
                }
            }
        }catch (e : Exception){
            Log.e(MainActivity::class.java.name, "Error",e)
        }

    }

    override fun onScanFailureFromGallery() {
        showToast("onScanFailureFromGallery")
    }

    private fun showToast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    override fun hideLoading() {
        progressBar.visibility = GONE
    }

    override fun showLoading() {
        progressBar.visibility = VISIBLE
    }

    private fun launchGalleryToPickPhoto() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, getString(R.string.gallery_pick_image)), PICK_IMAGE_REQUEST_CODE)
    }

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    matImage = Mat()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun isImageBlurred(image : Bitmap) : Boolean{
        val destination = Mat()
        val matGray = Mat()
        Utils.bitmapToMat(image, matImage)
        Imgproc.cvtColor(matImage, matGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Laplacian(matGray, destination, 3)
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(destination, median, std)
        val sharpness = Math.pow(std.get(0,0)[0], 2.0)

        showToast("Sharpness : $sharpness")

        return sharpness < 1000
    }
}
