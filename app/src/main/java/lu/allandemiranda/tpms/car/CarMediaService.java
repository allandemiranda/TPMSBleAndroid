package lu.allandemiranda.tpms.car;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class CarMediaService extends MediaLibraryService {

    private static final String ROOT_ID = "root";

    private MediaLibrarySession session;
    private ExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();

        session = new MediaLibrarySession.Builder(this, player, new LibraryCallback())
                .setId("tpms_media_session")
                .build();
    }

    @NonNull
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.release();
            session = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private static class LibraryCallback implements MediaLibrarySession.Callback {

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo controller,
                @Nullable LibraryParams params) {

            MediaItem root = new MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle("TPMS")
                            .setIsBrowsable(true)
                            .build())
                    .build();

            return Futures.immediateFuture(LibraryResult.ofItem(root, params));
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull String parentId,
                int page,
                int pageSize,
                @Nullable LibraryParams params) {

            ImmutableList<MediaItem> children =
                    ROOT_ID.equals(parentId) ? buildDummyList() : ImmutableList.of();

            return Futures.immediateFuture(LibraryResult.ofItemList(children, params));
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull String mediaId) {

            MediaItem item = dummy(mediaId, mediaId);
            return Futures.immediateFuture(LibraryResult.ofItem(item, /* params= */ null));
        }

        private static ImmutableList<MediaItem> buildDummyList() {
            return ImmutableList.of(
                    dummy("sensor_front_left",  "Front Left Tire"),
                    dummy("sensor_front_right", "Front Right Tire"),
                    dummy("sensor_rear_left",   "Rear Left Tire"),
                    dummy("sensor_rear_right",  "Rear Right Tire")
            );
        }

        private static MediaItem dummy(String id, String title) {
            return new MediaItem.Builder()
                    .setMediaId(id)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(title)
                            .setIsPlayable(true)
                            .build())
                    .build();
        }
    }
}
