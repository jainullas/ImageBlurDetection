package com.jain.ullas.imageblurdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader






class MainActivity : AppCompatActivity(), MainActivityView {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1001
    }

    private lateinit var presenter: MainPresenter
    private lateinit var matImage: Mat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainPresenter(this)
    }

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
                when(opencvProcess(it)) {
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
                    Log.i("OpenCV", "OpenCV loaded successfully")
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
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun opencvProcess(image : Bitmap) : Boolean{
        val l = CvType.CV_8UC1 //8-bit grey scale image
        Utils.bitmapToMat(image, matImage)
        val matImageGrey = Mat()
        Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY)

        val destImage: Bitmap
        destImage = Bitmap.createBitmap(image)
        val dst2 = Mat()
        Utils.bitmapToMat(destImage, dst2)
        val laplacianImage = Mat()
        dst2.convertTo(laplacianImage, l)
        Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U)
        val laplacianImage8bit = Mat()
        laplacianImage.convertTo(laplacianImage8bit, l)

        val bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(laplacianImage8bit, bmp)
        val pixels = IntArray(bmp.height * bmp.width)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height) // bmp为轮廓图

        var maxLap = -16777216 // 16m
        for (pixel in pixels) {
            if (pixel > maxLap)
                maxLap = pixel
        }

        var soglia = -6118750
        if (maxLap <= soglia) {
            println("is blur image")
        }
        soglia += 6118750
        maxLap += 6118750
//        opencvEnd = true
        return maxLap <= soglia
    }
}
