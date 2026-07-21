package com.example.familieconnect

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable

object MarkerIconFactory {

    fun create(
        resources: Resources,
        baseDrawableId: Int,
        label: String,
        sos: Boolean,
        follow: Boolean
    ): BitmapDrawable {

        val base = BitmapFactory.decodeResource(resources, baseDrawableId)

        val scaled =
            Bitmap.createScaledBitmap(base, 90, 130, true)

        val bitmap =
            scaled.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = bitmap.width * 0.30f
        paint.typeface = Typeface.DEFAULT_BOLD

        val x = bitmap.width / 2f
        val textY = bitmap.height * 0.38f
        val sosY = bitmap.height * 0.48f

        if (sos) {

            val pulse =
                if ((System.currentTimeMillis() / 500) % 2 == 0L)
                    1.0f
                else
                    1.25f

            val radius = bitmap.width * 0.30f * pulse

            paint.color = Color.RED
            canvas.drawCircle(x, sosY - 20f, radius, paint)

            paint.color = Color.WHITE

        } else if (follow) {

            val radius = bitmap.width * 0.30f

            paint.color = Color.rgb(100, 200, 255)
            canvas.drawCircle(x, sosY - 20f, radius, paint)

            paint.color = Color.BLACK

        } else {

            paint.color = Color.BLACK
        }

        val shortLabel =
            if (label.length > 6)
                label.take(6)
            else
                label

        canvas.drawText(shortLabel, x, textY, paint)

        return BitmapDrawable(resources, bitmap)
    }
}