package com.kaltura.tvplayer.offline

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.widget.Toast
import androidx.test.platform.app.InstrumentationRegistry
import com.kaltura.playkit.PKLog
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.OfflineManager
import com.kaltura.tvplayer.OfflineManager.AssetDownloadState.completed
import com.kaltura.tvplayer.OfflineManager.SelectionPrefs
import com.kaltura.tvplayer.offline.dtg.DTGOfflineManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch


private val log = PKLog.get("DTGOfflineManagerTest")

class OfflineManagerTest {

    val mainHandler = Handler(Looper.getMainLooper())

    private fun toast(msg: String) = mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    private fun toastLong(msg: String) = mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }

    private lateinit var context: Context
    private lateinit var instrumentation: Instrumentation


    val handler: Handler = {

        val thread = HandlerThread("test runner")
        thread.start()

        Handler(thread.looper)
    }()

    var downloadLatch: CountDownLatch? = null
    var downloadError: Exception? = null

    val listener = object : OfflineManager.AssetStateListener {
        override fun onStateChanged(assetId: String, downloadType: OfflineManager.DownloadType, assetInfo: OfflineManager.AssetInfo) {
            toast("State changed for asset $assetId")
        }

        override fun onAssetRemoved(assetId: String, downloadType: OfflineManager.DownloadType) {
            toast("Asset $assetId removed")
        }

        override fun onAssetRemoveError(
            assetId: String,
            downloadType: OfflineManager.DownloadType,
            error: java.lang.Exception
        ) {
            toast("Error Asset $assetId WAS NOT removed")
        }

        override fun onAssetDownloadPending(assetId: String,  downloadType: OfflineManager.DownloadType) {
            toast("asset $assetId is waiting for download")
        }

        override fun onAssetDownloadPaused(assetId: String,  downloadType: OfflineManager.DownloadType) {
            toast("asset $assetId download finished")
        }

        override fun onRegistered(assetId: String, drmStatus: OfflineManager.DrmStatus) {
            toast("drm register of asset $assetId is successful")
        }

        override fun onRegisterError(assetId: String, downloadType: OfflineManager.DownloadType, error: Exception) {
            toast("drm register of asset $assetId, ${downloadType.name} has failed: $error")
        }

        override fun onUnRegisterError(assetId: String, downloadType: OfflineManager.DownloadType, error: Exception) {
            toast("drm unregister of asset $assetId, ${downloadType.name} has failed: $error")
        }

        override fun onAssetDownloadFailed(assetId: String, downloadType: OfflineManager.DownloadType, error: Exception) {
            toast("download of $assetId, ${downloadType.name} has failed: $error")
            downloadError = error
            downloadLatch?.countDown()
        }
        
        override fun onAssetDownloadComplete(assetId: String, downloadType : OfflineManager.DownloadType) {
            toast("download of $assetId has finished")
            downloadError = null
            downloadLatch?.countDown()
        }

        override fun onAssetPrefetchComplete(
            assetId: String,
            downloadType: OfflineManager.DownloadType
        ) {
            toast("prefetch of $assetId has finished") 
        }
    }

    @Before
    fun setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        context = instrumentation.targetContext

        DTGOfflineManager.getInstance(context).setAssetStateListener(listener)
    }

    private fun startPlayer(assetId: String, expectedDuration: Long) {
        val intent = Intent(context, PlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse(assetId)
            putExtra("expectedDuration", expectedDuration)
        }

//        instrumentation.startActivitySync(intent)



        context.startActivity(intent)
    }

    fun test(block: (om: OfflineManager) -> Unit) {

        val manager = DTGOfflineManager.getInstance(context)

        val latch = CountDownLatch(1)

        handler.post {

            val startLatch = CountDownLatch(1)
            manager.start{ startLatch.countDown() }
            startLatch.await()

//            manager.waitUntilStarted()

            block(manager)

            latch.countDown()
        }

        latch.await()
    }

    fun downloadComplete(error: Exception) {
        downloadError = error
        downloadLatch?.countDown()
    }

    private fun download(
        om: OfflineManager,
        item: Item,
        prefs: SelectionPrefs,
        expectedEstSize: Long,
        expectedDownloadedBytes: Long
    ) {
        om.removeAsset(item.id())
        if (item is KalturaItem) {
            om.setKalturaParams(KalturaPlayer.Type.ovp, item.partnerId)
            om.setKalturaServerUrl(item.serverUrl)
        }

        downloadLatch = CountDownLatch(1)

        val callback = object : OfflineManager.PrepareCallback {
            override fun onPrepared(
                assetId: String,
                assetInfo: OfflineManager.AssetInfo,
                selected: MutableMap<OfflineManager.TrackType, MutableList<OfflineManager.Track>>?
            ) {
                assertEquals(expectedEstSize, assetInfo.estimatedSize)
                om.startAssetDownload(assetInfo)
            }

            override fun onPrepareError(assetId: String, downloadType: OfflineManager.DownloadType, error: Exception) {
                fail("Prepare failed with $error")
                downloadComplete(error)
            }
        }

        if (item is KalturaItem) {
            om.prepareAsset(item.mediaOptions(), prefs, callback)
        } else {
            item.entry?.let {
                om.prepareAsset(it, prefs, callback)
            } ?: downloadComplete(Exception())
        }

        downloadLatch?.await()
        assertNull("Download has failed: $downloadError", downloadError)

        val assetInfo = om.getAssetInfo(item.id())
        assertNotNull(assetInfo)
        assetInfo?.let {
            assertEquals(completed, it.state)
            assertEquals(expectedDownloadedBytes, it.bytesDownloaded)
        }
    }


    @Test
    fun prepare1() {
        val item = OVPItem(2215841, "0_axrfacp3")
        val expectedEstSize = 36837968L
        val expectedDownloadedBytes = 37671264L
        val expectedDuration = 634253L

        log.d("prepare1.thread=${Thread.currentThread()}")

        val block: (OfflineManager) -> Unit = { om ->
            download(
                om,
                item,
                SelectionPrefs(),
                expectedEstSize = expectedEstSize,
                expectedDownloadedBytes = expectedDownloadedBytes
            )

            log.d("prepare1.block.thread=${Thread.currentThread()}")
//            startPlayer(item.id(), expectedDuration)

//            Looper.getMainLooper().thread.join()
//            Thread.sleep(10000)
        }
        test(block)



        log.d("PREPARE1 ended")
    }
}
