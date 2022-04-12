package com.kaltura.tvplayer.offline.exo

import com.kaltura.android.exoplayer2.Format

internal class DrmRegistrationMetaData @JvmOverloads constructor(val format: Format,
                                            var isRegistered: Boolean = false)
