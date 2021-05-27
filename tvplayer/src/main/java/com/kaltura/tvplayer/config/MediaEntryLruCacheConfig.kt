package com.kaltura.tvplayer.config

import androidx.annotation.NonNull

const val MAX_MEDIA_ENTRY_CACHE_SIZE = 15
const val MEDIA_ENTRY_CACHE_EXPIRATION_TIME = 86_400_000 // 24H

data class MediaEntryLruCacheConfig @JvmOverloads constructor(@NonNull var allowMediaEntryCaching: Boolean = false,
                                                              @NonNull var maxMediaEntryCacheSize: Int = MAX_MEDIA_ENTRY_CACHE_SIZE,
                                                              @NonNull var timeoutMS: Int = MEDIA_ENTRY_CACHE_EXPIRATION_TIME)
