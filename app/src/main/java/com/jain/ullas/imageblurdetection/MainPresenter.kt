package com.jain.ullas.imageblurdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MainPresenter(val view: MainActivityView) {

    private val compositeSubscription = CompositeSubscription()


    fun getDataFromImageBitmap(galleryImageBitmap: Bitmap) {
        val subscription =
                Observable.just(galleryImageBitmap)
                        .map { resizeBitmap(it, 500, 500) }
                        .map { view.getSharpnessScoreFromOpenCV(it)  }
                        .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                        .subscribe(
                                { score ->
                                    view.hideLoading()
                                    view.showScoreFromOpenCV(score)
                                },
                                {
                                    run {
                                        view.hideLoading()
                                        view.onError()
                                    }
                                })
        compositeSubscription.add(subscription)
    }

    private fun resizeBitmap(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var width = image.width
        var height = image.height

        when {
            width > height -> { //landscape image
                val ratio = (width / maxWidth.toFloat())
                width = maxWidth
                height = (height / ratio).toInt()
            }
            height > width -> { //portrait image
                val ratio = height / maxHeight.toFloat()
                height = maxHeight
                width = (width / ratio).toInt()
            }
            else -> {
                width = maxWidth
                height = maxHeight
            }
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun onDestroy() {
        compositeSubscription.clear()
    }

    fun onClickSelectImage() {
        view.onClickSelectImage()
    }

    fun onActivityResultForPickImageRequest(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MainActivity.PICK_IMAGE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK && null != data){
            view.onActivityResultForPickImageRequest(data)
        }
    }

}