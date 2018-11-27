package com.jain.ullas.imageblurdetection

import android.content.Intent
import android.graphics.Bitmap

interface MainActivityView {
    fun showLoading()
    fun hideLoading()
    fun extractImageBitmapFromIntentData(galleryIntentData: Intent)
    fun onScanFailureFromGallery()
    fun checkForImageSharpnessFromOpenCV(image: Bitmap): Double
    fun updateView()
    fun calculateAverage()
}
