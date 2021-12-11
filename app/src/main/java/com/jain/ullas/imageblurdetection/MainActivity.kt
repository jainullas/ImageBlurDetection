package com.jain.ullas.imageblurdetection

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import java.text.DecimalFormat


class MainActivity : AppCompatActivity(), MainActivityView {

    companion object {
        private const val TAG = "MainActivity"
        const val PICK_IMAGE_REQUEST_CODE = 1001
        private const val BLUR_THRESHOLD = 500
        const val BLURRED_IMAGE = "BLURRED IMAGE"
        const val NOT_BLURRED_IMAGE = "NOT BLURRED IMAGE"

        init {
            System.loadLibrary("opencv_java3")
        }

    }

    private lateinit var sourceMatImage: Mat
    private val presenter: MainPresenter by lazy {
        MainPresenter(this)
    }
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    sourceMatImage = Mat()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textCpuArchitecture.text = getString(R.string.cpu_architecture, System.getProperty("os.arch"))
        statusFromRenderScript.visibility = View.VISIBLE
    }

    override fun onClickSelectImage() {
        Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(this,
                    getString(R.string.gallery_pick_image)), PICK_IMAGE_REQUEST_CODE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pick_gallery_image, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_gallery -> {
                presenter.onClickSelectImage()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        presenter.onActivityResultForPickImageRequest(requestCode, resultCode, data)
    }

    override fun onActivityResultForPickImageRequest(data: Intent) {
        extractImageBitmapFromIntentData(data)
    }

    override fun onError() {

    }

    private fun extractImageBitmapFromIntentData(galleryIntentData: Intent) {
        showLoading()
        try {
            val imageUri = galleryIntentData.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            scannedImage.setImageBitmap(bitmap)
            scannedImage.visibility = VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                showScoreFromRenderScript(BlurrinessDetectionRenderScript.runDetection(this, bitmap))
            }
            presenter.getDataFromImageBitmap(bitmap)
        } catch (e: Exception) {
            hideLoading()
            Log.e(TAG, "Error", e)
        }
    }

    override fun getSharpnessScoreFromOpenCV(bitmap: Bitmap): Double {
        val destination = Mat()
        val matGray = Mat()
        Utils.bitmapToMat(bitmap, sourceMatImage)
        Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Laplacian(matGray, destination, 3)
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(destination, median, std)
        return DecimalFormat("0.00").format(Math.pow(std.get(0, 0)[0], 2.0)).toDouble()
    }

    override fun showScoreFromOpenCV(score: Double) {
        with(statusFromOpenCV) {
            visibility = View.VISIBLE
            when (score < BLUR_THRESHOLD) {
                true -> {
                    text = getString(R.string.result_from_opencv, BLURRED_IMAGE, BLUR_THRESHOLD.toString(), score.toString())
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blurred_image))
                }
                false -> {
                    text = getString(R.string.result_from_opencv, NOT_BLURRED_IMAGE, BLUR_THRESHOLD.toString(), score.toString())
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.not_blurred_image))
                }
            }
        }
    }

    override fun showScoreFromRenderScript(status: Pair<Boolean, String>) {
        with(statusFromRenderScript) {
            visibility = View.VISIBLE
            text = status.second
            when (status.first) {
                true -> {
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blurred_image))
                }
                false -> {
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.not_blurred_image))
                }
            }
        }
    }

    override fun hideLoading() {
        progressBar.visibility = GONE
    }

    override fun showLoading() {
        progressBar.visibility = VISIBLE
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
