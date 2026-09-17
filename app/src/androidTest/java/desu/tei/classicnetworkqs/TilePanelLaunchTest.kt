package desu.tei.classicnetworkqs

import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TilePanelLaunchTest {
    @Test
    fun sendInternetPanelBroadcastFromAppUid() {
        InstrumentationRegistry.getInstrumentation().targetContext.sendBroadcast(
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .setPackage("com.android.systemui"),
        )
    }
}
