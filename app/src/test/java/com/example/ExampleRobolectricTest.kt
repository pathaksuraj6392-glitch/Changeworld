package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClockColorPreset
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutStyle
import com.example.model.DepthClockConfig
import com.example.model.SegmentationMode
import com.example.renderer.DepthClockCanvasRenderer
import com.example.segmentation.SubjectSegmentationEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Depth Clock", appName)
    }

    @Test
    fun `segmentation engine and 3-layer renderer produce valid high-res depth bitmap`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = SubjectSegmentationEngine()

        // Create a synthetic portrait bitmap with a dark sky background and a bright central subject
        val srcBitmap = Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888)
        for (y in 0 until 320) {
            for (x in 0 until 180) {
                val isSubject = x in 55..125 && y in 70..300
                srcBitmap.setPixel(
                    x,
                    y,
                    if (isSubject) Color.rgb(240, 180, 140) else Color.rgb(12, 18, 34)
                )
            }
        }

        val rawData = engine.analyzePhoto(srcBitmap)
        val result = engine.buildSegmentationResult(
            rawData = rawData,
            mode = SegmentationMode.AUTO_HYBRID,
            threshold = 0.40f,
            featherRadius = 2
        )

        assertNotNull(result.foregroundCutout)
        assertTrue(result.activeCoveragePercent > 5)

        val config = DepthClockConfig(
            fontStyle = ClockFontStyle.BEBAS_NEUE,
            layoutStyle = ClockLayoutStyle.IOS_CLASSIC,
            colorPreset = ClockColorPreset.ELECTRIC_CYAN,
            enableDepthEffect = true
        )

        val exported = DepthClockCanvasRenderer.renderToHighResBitmap(
            context = context,
            backgroundBitmap = srcBitmap,
            foregroundCutout = result.foregroundCutout,
            config = config,
            outputWidth = 360,
            outputHeight = 640
        )

        assertEquals(360, exported.width)
        assertEquals(640, exported.height)
    }
}
