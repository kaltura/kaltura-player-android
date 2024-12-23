package com.kaltura.tvplayer.offline.exo

import com.kaltura.androidx.media3.common.Format

internal class DrmRegistrationMetaData @JvmOverloads constructor(val format: Format,
                                            var isRegistered: Boolean = false)
