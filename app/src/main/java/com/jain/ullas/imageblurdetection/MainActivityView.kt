package com.jain.ullas.imageblurdetection

import android.graphics.Bitmap

interface MainActivityView {
    fun showLoading()
    fun hideLoading()
    fun onSuccessfulScan(bitmap: Bitmap?)
    fun onScanFailureFromGallery()

}
