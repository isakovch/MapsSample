package com.example.mapssample

import android.graphics.*
import java.util.*

object ImageUtils {
    fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Float = 45F): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, pixels, pixels, paint)

        val random = Random()
        val gradientColor =
            Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))

        val shader = LinearGradient(
            0F,
            0F,
            0F,
            bitmap.height.toFloat(),
            gradientColor,
            gradientColor,
            Shader.TileMode.CLAMP
        )

        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawRect(0F, 0F, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}