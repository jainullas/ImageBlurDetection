package com.jain.ullas.imageblurdetection

import android.content.Intent
import android.graphics.Bitmap

interface MainActivityView {
    fun showLoading()
    fun hideLoading()
    fun onClickSelectImage()
    fun onActivityResultForPickImageRequest(data: Intent)
    fun getSharpnessScoreFromOpenCV(bitmap: Bitmap): Double
    fun onError()
    fun showScoreFromOpenCV(score: Double)
    fun showScoreFromRenderScript(status : Pair<Boolean, String>)

}
