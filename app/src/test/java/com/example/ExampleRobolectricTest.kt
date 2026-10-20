package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranClassificationHelper
import com.example.data.SurahClassification
import com.example.ui.security.BiometricHelper
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
    fun `read string from context in default locale`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertTrue(appName == "Society Portal" || appName == "بوابة الجمعية")
    }

    @Test
    @Config(qualifiers = "ar")
    fun `read string from context in arabic locale`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("بوابة الجمعية", appName)
    }

    @Test
    fun `verify quran surah classification helper`() {
        // البقرة مدنية
        assertEquals(SurahClassification.MADANI, QuranClassificationHelper.getClassification("سورة البقرة"))
        // الإخلاص مكية
        assertEquals(SurahClassification.MAKKI, QuranClassificationHelper.getClassification("سورة الإخلاص"))
    }

    @Test
    fun `verify biometric status check does not throw`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = BiometricHelper.checkBiometricStatus(context)
        assertNotNull(status)
    }
}
