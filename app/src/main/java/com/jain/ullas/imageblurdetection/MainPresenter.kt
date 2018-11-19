package com.jain.ullas.imageblurdetection

import android.graphics.Bitmap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MainPresenter(val view: MainActivityView) {

    private val compositeSubscription = CompositeSubscription()


    fun getDataFromImageBitmap(galleryImageBitmap: Bitmap) {
        val subscription =
                Observable.fromCallable { resizeBitmap(galleryImageBitmap, 500, 500) }
                        .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                        .subscribe(
                                { bitmap ->
                                    view.hideLoading()
                                    when {
                                        bitmap != null -> {
                                            view.onSuccessfulScan(bitmap)
                                        }
                                        else -> view.onScanFailureFromGallery()
                                    }
                                },
                                {
                                    run {
                                        view.hideLoading()
                                        view.onScanFailureFromGallery()
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

}