package desu.tei.classicnetworkqs

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkStateRepositoryTest {
    @Test
    fun reopenOnWifiWithoutPublishingIncompleteState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork
        assumeTrue(manager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
        val canReadSsid = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            context.getSystemService(LocationManager::class.java).isLocationEnabled
        val states = mutableListOf<InternetState>()
        var published = CountDownLatch(1)
        lateinit var repository: NetworkStateRepository
        instrumentation.runOnMainSync {
            repository = NetworkStateRepository(context) {
                states.add(it)
                published.countDown()
            }
        }
        var ssid: String? = null
        repeat(5) { iteration ->
            try {
                instrumentation.runOnMainSync {
                    states.clear()
                    published = CountDownLatch(1)
                    assertEquals(network, manager.activeNetwork)
                    repository.startObserving()
                    assertTrue("Published before capabilities arrived: $states", states.isEmpty())
                }
                assertTrue("No network state received", published.await(5, TimeUnit.SECONDS))
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    assertTrue("Published a non-Wi-Fi state: $states", states.all { it is InternetState.Wifi })
                    if (iteration == 0) ssid = (states.first() as InternetState.Wifi).ssid
                    states.forEach {
                        assertEquals(ssid, (it as InternetState.Wifi).ssid)
                        if (canReadSsid) assertNotNull("Missing SSID, including when default network is a VPN", it.ssid)
                    }
                }
            } finally {
                instrumentation.runOnMainSync { repository.stopObserving() }
            }
        }
    }
}
