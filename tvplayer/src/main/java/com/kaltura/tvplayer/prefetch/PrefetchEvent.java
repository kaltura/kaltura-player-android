package com.kaltura.tvplayer.prefetch;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PlayerEvent;

public class PrefetchEvent implements PKEvent  {

    public static final Class<PrefetchEvent.PrefetchAssetAdded> prefetchAssetAdded = PrefetchEvent.PrefetchAssetAdded.class;
    public static final Class<PrefetchEvent.PrefetchAssetRemoved> prefetchAssetRemoved = PrefetchEvent.PrefetchAssetRemoved.class;
    public static final Class<PrefetchEvent.PrefetchPrepareError> prefetchPrepareError = PrefetchEvent.PrefetchPrepareError.class;
    public static final Class<PrefetchEvent.PrefetchRegistrationError> prefetchRegistrationError = PrefetchEvent.PrefetchRegistrationError.class;
    public static final Class<PrefetchEvent.PrefetchAssetRemoveError> prefetchAssetRemoveError = PrefetchEvent.PrefetchAssetRemoveError.class;

    public static final PrefetchEvent.Type prefetchCacheFull = Type.PREFETCH_CACHE_FULL;


    public final PrefetchEvent.Type type;

    public PrefetchEvent(PrefetchEvent.Type type) {
        this.type = type;
    }

    public static class PrefetchAssetAdded extends PrefetchEvent {

        public final String assetId;

        public PrefetchAssetAdded(String assetId) {
            super(Type.PREFETCH_ASSET_ADDED);
            this.assetId = assetId;
        }
    }

    public static class PrefetchAssetRemoved extends PrefetchEvent {

        public final String assetId;

        public PrefetchAssetRemoved(String assetId) {
            super(Type.PREFETCH_ASSET_REMOVED);
            this.assetId = assetId;
        }
    }
    public static class PrefetchPrepareError extends PrefetchEvent {

        public final String assetId;
        public final ErrorElement error;

        public PrefetchPrepareError(String assetId, ErrorElement error) {
            super(Type.PREFETCH_PREPARE_ERROR);
            this.assetId = assetId;
            this.error = error;
        }
    }

    public static class PrefetchRegistrationError extends PrefetchEvent {

        public final String assetId;
        public final ErrorElement error;

        public PrefetchRegistrationError(String assetId, ErrorElement error) {
            super(Type.PREFETCH_REGISTRATION_ERROR);
            this.assetId = assetId;
            this.error = error;
        }
    }

    public static class PrefetchAssetRemoveError extends PrefetchEvent {

        public final String assetId;

        public PrefetchAssetRemoveError(String assetId) {
            super(Type.PREFETCH_ASSET_REMOVE_ERROR);
            this.assetId = assetId;
        }
    }

    public enum Type {
        PREFETCH_ASSET_ADDED,
        PREFETCH_ASSET_REMOVED,
        PREFETCH_CACHE_FULL,
        PREFETCH_PREPARE_ERROR,
        PREFETCH_REGISTRATION_ERROR,
        PREFETCH_ASSET_REMOVE_ERROR
    }

    @Override
    public Enum eventType() {
        return this.type;
    }
}
