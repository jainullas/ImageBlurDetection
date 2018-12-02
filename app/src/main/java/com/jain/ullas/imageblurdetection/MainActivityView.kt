package com.jain.ullas.imageblurdetection

import android.content.Intent
import android.graphics.Bitmap

interface MainActivityView {
    fun showLoading()
    fun hideLoading()
    fun onClickSelectImage()
    fun onActivityResultForPickImageRequest(data: Intent)
    fun getSharpnessScore(bitmap: Bitmap): Double
    fun showScore(score: Double)
    fun onError()

}
