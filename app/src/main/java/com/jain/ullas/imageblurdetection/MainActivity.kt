package com.jain.ullas.imageblurdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.core.Mat
import java.text.DecimalFormat
import android.app.ProgressDialog
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import relinker.ReLinker
import java.io.*
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity(), MainActivityView {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1001
        const val BLUR_THRESHOLD = 500

    }

    private var sourceMatImage: Mat? = null
    private lateinit var presenter: MainPresenter
    private val logcatLogger = ReLinker.Logger { message -> Log.d("ReLinker", message) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadLibrary()
        presenter = MainPresenter(this)
        textCpuArchitecture.text = getString(R.string.cpu_architecture, System.getProperty("os.arch"))
        threshold.text = BLUR_THRESHOLD.toString()
    }

    override fun checkForImageSharpnessFromOpenCV(image: Bitmap): Double {
        sourceMatImage?.let {
            val destination = Mat()
            val matGray = Mat()
            Utils.bitmapToMat(image, it)
            Imgproc.cvtColor(it, matGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.Laplacian(matGray, destination, 3)
            val median = MatOfDouble()
            val std = MatOfDouble()
            Core.meanStdDev(destination, median, std)
            return DecimalFormat("0.00").format(Math.pow(std.get(0, 0)[0], 2.0)).toDouble()
        } ?: run {
            return 0.0
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)
        when (requestCode) {
            PICK_IMAGE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK && null != intentData) {
                presenter.extractImagesFromIntentData(intentData)
                presenter.processBitmapWithOpenCV()
            }
        }
    }

    override fun updateView() {
        hideLoading()
        recycler_view.apply {
            adapter = RecyclerViewAdapter(presenter.getData())
            layoutManager = LinearLayoutManager(this@MainActivity)
            visibility = View.VISIBLE
        }
    }

    override fun calculateAverage() {
        average.apply {
            text = "Average : ".plus(DecimalFormat("0.00").format(presenter.calculateAverage()))
            visibility = View.VISIBLE
        }
    }

    override fun extractImageBitmapFromIntentData(galleryIntentData: Intent) {
        showLoading()
        try {
            if (galleryIntentData.data != null) {
                val imageUri = galleryIntentData.data
                presenter.addData(Data(MediaStore.Images.Media.getBitmap(contentResolver, imageUri)))
            } else {
                if (galleryIntentData.clipData != null) {
                    val clipData = galleryIntentData.clipData
                    (0 until clipData.itemCount).forEach {
                        val clipDataItem = clipData.getItemAt(it)
                        presenter.addData(Data(MediaStore.Images.Media.getBitmap(contentResolver, clipDataItem.uri)))
                    }
                }
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
                    if (sourceMatImage == null) {
                        sourceMatImage = Mat()
                    }
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun loadLibrary() {
        val soFilePath = "/sdcard/data/user/0/jnilibs/arm64-v8a/"
        val sourceSoFile = File(soFilePath, "libopencv_java3.so")
        val libFolderPath = getDir("lib", 0)
        val destinationFile = File(libFolderPath, "libopencv_java3.so")
        if (!destinationFile.exists()) {
            Log.d("Ullas", "Not exists")
            destinationFile.createNewFile()
        } else {
            destinationFile.delete()
            destinationFile.createNewFile()
        }
        try {
            copyFile(sourceSoFile, destinationFile)
            val file = File(destinationFile.absolutePath)
            Toast.makeText(this, file.exists().toString(), Toast.LENGTH_LONG).show()
            ReLinker.log(logcatLogger)
                    .loadLibrary(this, "opencv_java3", object : ReLinker.LoadListener {
                override fun success() {
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
                }

                override fun failure(t: Throwable?) {
                }
            })

        } catch (e: Exception) {
            Log.d("Ullas", "", e)
        }
    }

    @Throws(IOException::class)
    fun copyFile(sourceFile: File, destFile: File) {
        if (!destFile.parentFile.exists())
            destFile.parentFile.mkdirs()

        if (!destFile.exists()) {
            destFile.createNewFile()
        }

        var source: FileChannel? = null
        var destination: FileChannel? = null

        try {
            source = FileInputStream(sourceFile).channel
            destination = FileOutputStream(destFile).channel
            destination!!.transferFrom(source, 0, source!!.size())
        } finally {
            source?.close()
            destination?.close()
        }
    }

}