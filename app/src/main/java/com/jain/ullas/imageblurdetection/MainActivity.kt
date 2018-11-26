package com.jain.ullas.imageblurdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.DecimalFormat
import java.util.*


class MainActivity : AppCompatActivity(), MainActivityView {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1001
        const val BLUR_THRESHOLD = 500
        private const val BLURRED_IMAGE = "BLURRED IMAGE"
        private const val NOT_BLURRED_IMAGE = "NOT BLURRED IMAGE"
    }

    private lateinit var sourceMatImage: Mat
    private lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainPresenter(this)
        textCpuArchitecture.text = getString(R.string.cpu_architecture, System.getProperty("os.arch"))
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
                Observable.just(true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(object : Subscriber<Boolean>(){
                            override fun onNext(t: Boolean?) {
                                val bitmapImages = extractImageBitmapFromIntentData(data)
                                (0 until bitmapImages.size).forEach {
                                    presenter.getDataFromImageBitmap(bitmapImages[it])
                                }
                            }

                            override fun onCompleted() {

                            }

                            override fun onError(e: Throwable?) {

                            }

                        })
            }
        }
    }


    private fun extractImageBitmapFromIntentData(galleryIntentData: Intent): ArrayList<Bitmap> {
        showLoading()
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val images = ArrayList<Bitmap>()
        if (galleryIntentData.data != null) {
            val imageUri = galleryIntentData.data
            images.add(MediaStore.Images.Media.getBitmap(contentResolver, imageUri))
//            val cursor = contentResolver.query(imageUri, filePathColumn, null, null, null)
//            cursor.moveToFirst()
//            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
//            images.add(cursor.getString(columnIndex))
//            cursor.close()
        } else {
            if (galleryIntentData.clipData != null) {
                val clipData = galleryIntentData.clipData
                (0 until clipData.itemCount).forEach {
                    val clipDataItem = clipData.getItemAt(it)
                    images.add(MediaStore.Images.Media.getBitmap(contentResolver, clipDataItem.uri))

//                    val cursor = contentResolver.query(clipDataItem.uri, filePathColumn, null, null, null)
//                    cursor.moveToFirst()
//                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
//                    imagesEncodedList.add(cursor.getString(columnIndex))
//                    cursor.close()
                }
            }
        }
//        try {
//            val imageUri = galleryIntentData.data
//            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
//            scannedImage.setImageBitmap(bitmap)
//            scannedImage.visibility = VISIBLE
//            presenter.getDataFromImageBitmap(bitmap)
//        } catch (e: Exception) {
//            hideLoading()
//            Log.e(MainActivity::class.java.name, "Error", e)
//        }
        return images
    }

    override fun onSuccessfulScan(bitmap: Bitmap?) {
        clearExistingResult()
        try {
            bitmap?.let {
                updateStatus(checkForImageSharpness(it))
            }
        } catch (e: Exception) {
            Log.e(MainActivity::class.java.name, "Error", e)
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.gallery_pick_image)), PICK_IMAGE_REQUEST_CODE)
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

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun checkForImageSharpness(image: Bitmap): Double {
        val destination = Mat()
        val matGray = Mat()
        Utils.bitmapToMat(image, sourceMatImage)
        Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Laplacian(matGray, destination, 3)
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(destination, median, std)
        return DecimalFormat("0.00").format(Math.pow(std.get(0, 0)[0], 2.0)).toDouble()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
