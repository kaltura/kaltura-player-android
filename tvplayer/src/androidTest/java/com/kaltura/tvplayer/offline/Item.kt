package com.kaltura.tvplayer.offline

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.kaltura.playkit.PKMediaEntry
import com.kaltura.playkit.PKMediaSource
import com.kaltura.tvplayer.MediaOptions
import com.kaltura.tvplayer.OTTMediaOptions
import com.kaltura.tvplayer.OVPMediaOptions
import com.kaltura.tvplayer.OfflineManager
import java.util.*

fun String.fmt(vararg args: Any?): String = java.lang.String.format(Locale.ROOT, this, *args)

abstract class Item : Parcelable {
    var entry: PKMediaEntry? = null
    var assetInfo: OfflineManager.AssetInfo? = null
    var percentDownloaded: Float? = null
    var bytesDownloaded: Long? = null
    abstract fun id(): String
    abstract fun title(): String

    private fun sizeMB(): String {
        val sizeBytes = assetInfo?.estimatedSize
        if (sizeBytes == null || sizeBytes <= 0) {
            return "--"
        }

        return "%.3f".fmt(sizeBytes.toFloat() / (1000*1000)) + "mb"
    }

    override fun toString(): String {
        val state = assetInfo?.state ?: OfflineManager.AssetDownloadState.none

        var string = "${title()}, $state\n"
        if (state == OfflineManager.AssetDownloadState.started) {
            string += if (percentDownloaded != null) "%.1f".fmt(percentDownloaded) + "% / " else "--"
        }
        string += sizeMB()

        return string
    }
}

class BasicItem(private val id: String, val url: String): Item() {

    init {
        this.entry = PKMediaEntry().apply {
            id = this@BasicItem.id
            mediaType = PKMediaEntry.MediaEntryType.Vod
            sources = listOf(PKMediaSource().apply {
                id = this@BasicItem.id
                url = this@BasicItem.url
            })
        }

        Log.d("Item", entry.toString())
    }

    override fun id() = id

    override fun title() = id

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BasicItem> {
        override fun createFromParcel(parcel: Parcel): BasicItem {
            return BasicItem(parcel)
        }

        override fun newArray(size: Int): Array<BasicItem?> {
            return arrayOfNulls(size)
        }
    }
}

abstract class KalturaItem(val partnerId: Int, val serverUrl: String): Item() {

    abstract fun mediaOptions(): MediaOptions

    override fun title() = "${id()} @ $partnerId"
}

class OVPItem(partnerId: Int, private val entryId: String, serverUrl: String = "https://cdnapisec.kaltura.com"
) : KalturaItem(partnerId, serverUrl) {

    override fun id() = assetInfo?.assetId ?: entryId

    override fun mediaOptions() = OVPMediaOptions(entryId)

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(partnerId)
        parcel.writeString(entryId)
        parcel.writeString(serverUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OVPItem> {
        override fun createFromParcel(parcel: Parcel): OVPItem {
            return OVPItem(parcel)
        }

        override fun newArray(size: Int): Array<OVPItem?> {
            return arrayOfNulls(size)
        }
    }
}

class OTTItem(partnerId: Int, val ottAssetId: String, serverUrl: String, val format: String) : KalturaItem(partnerId, serverUrl) {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun id() = assetInfo?.assetId ?: ottAssetId

    override fun mediaOptions() = OTTMediaOptions().apply {
        assetId = ottAssetId
        formats = arrayOf(format)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(partnerId)
        parcel.writeString(ottAssetId)
        parcel.writeString(serverUrl)
        parcel.writeString(format)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OTTItem> {
        override fun createFromParcel(parcel: Parcel): OTTItem {
            return OTTItem(parcel)
        }

        override fun newArray(size: Int): Array<OTTItem?> {
            return arrayOfNulls(size)
        }
    }
}
