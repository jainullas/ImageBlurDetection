package com.jain.ullas.imageblurdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicColorMatrix
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi

object BlurrinessDetectionRenderScript {

    private val CLASSIC_MATRIX = floatArrayOf(
            -1.0f, -1.0f, -1.0f,
            -1.0f, 8.0f, -1.0f,
            -1.0f, -1.0f, -1.0f
    )

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun runDetection(context: Context, sourceBitmap: Bitmap): Pair<Boolean, String> {
        val rs = RenderScript.create(context)

        val smootherBitmap = Bitmap.createBitmap(sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
        )
        val blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs))
        val source = Allocation.createFromBitmap(rs,
                sourceBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )
        val blurTargetAllocation = Allocation.createFromBitmap(rs,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )
        blurIntrinsic.apply {
            setRadius(1f)
            setInput(source)
            forEach(blurTargetAllocation)
        }
        blurTargetAllocation.copyTo(smootherBitmap)


        val greyscaleBitmap = Bitmap.createBitmap(sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
        )
        val smootherInput = Allocation.createFromBitmap(rs,
                smootherBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )
        val greyscaleTargetAllocation = Allocation.createFromBitmap(rs,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )

        val colorIntrinsic = ScriptIntrinsicColorMatrix.create(rs)
        colorIntrinsic.setGreyscale()
        colorIntrinsic.forEach(smootherInput, greyscaleTargetAllocation)
        greyscaleTargetAllocation.copyTo(greyscaleBitmap)

        val edgesBitmap = Bitmap.createBitmap(sourceBitmap.width,
                sourceBitmap.height,
                sourceBitmap.config
        )
        val greyscaleInput = Allocation.createFromBitmap(rs,
                greyscaleBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )
        val edgesTargetAllocation = Allocation.createFromBitmap(rs,
                edgesBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SHARED
        )

        val convolve = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
        convolve.setInput(greyscaleInput)
        convolve.setCoefficients(CLASSIC_MATRIX) // Or use others
        convolve.forEach(edgesTargetAllocation)
        edgesTargetAllocation.copyTo(edgesBitmap)

        @ColorInt val mostLuminousColor = mostLuminousColorFromBitmap(edgesBitmap)
        val colorHex = "#" + Integer.toHexString(mostLuminousColor)
        val isBlurry = mostLuminousColor > Color.parseColor("#CECECE") // Demo threshold
        // Note - in Android, Color.BLACK is -16777216 and Color.WHITE is -1, so range is somewhere in between. Higher is more luminous
//        Toast.makeText(context, output, Toast.LENGTH_LONG).show()
        return Pair(isBlurry, context.getString(R.string.result_from_renderscript, if (isBlurry) MainActivity.BLURRED_IMAGE else MainActivity.NOT_BLURRED_IMAGE, colorHex))
    }

    /**
     * Resolves the most luminous color pixel in a given bitmap.
     *
     * @param bitmap Source bitmap.
     * @return The most luminous color pixel in the `bitmap`
     */
    @ColorInt
    fun mostLuminousColorFromBitmap(bitmap: Bitmap): Int {
        bitmap.setHasAlpha(false)
        val pixels = IntArray(bitmap.height * bitmap.width)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        @ColorInt var mostLuminousColor = Color.BLACK

        for (pixel in pixels) {
            if (pixel > mostLuminousColor) {
                mostLuminousColor = pixel
            }
        }
        return mostLuminousColor
    }

}