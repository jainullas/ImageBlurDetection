package com.jain.ullas.imageblurdetection

import android.content.Intent
import android.graphics.Bitmap
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MainPresenter(val view: MainActivityView) {

    private val compositeSubscription = CompositeSubscription()
    private val data = arrayListOf<Data>()


    fun processBitmapWithOpenCV() {
        val resizeSubscription =
                Observable.from(data)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map { data -> data.copy(bitmap = resizeBitmap(data.bitmap)) }
                        .map { data -> data.copy(score = view.checkForImageSharpnessFromOpenCV(data.bitmap)) }
                        .toList()
                        .subscribe {
                            data.clear()
                            data.addAll(it)
                            view.updateView()
                            view.calculateAverage()
                        }
        compositeSubscription.add(resizeSubscription)
    }

    fun calculateAverage() : Double {
        var sum = 0.0
        if(data.isNotEmpty()){
            (0 until data.size).forEach {
                sum += data[it].score
            }
            return sum / data.size
        }
        return sum
    }

    fun extractImagesFromIntentData(galleryIntentData: Intent) {
        data.clear()
        view.extractImageBitmapFromIntentData(galleryIntentData)
    }

    private fun resizeBitmap(image: Bitmap): Bitmap {
        val maxWidth = 500
        val maxHeight = 500
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

    fun getData() = data

    fun addData(item: Data) {
        data.add(item)
    }

}