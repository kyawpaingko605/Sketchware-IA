package pro.sketchware.activities.chat;

import android.net.Uri;

import androidx.annotation.Nullable;

public class ChatReference {
    public static final int TYPE_FILE = 1;
    public static final int TYPE_FOLDER = 2;
    public static final int TYPE_IMAGE = 3;

    private final int type;
    private final String label;
    private final String path;
    private final @Nullable Uri uri;
    private final @Nullable String mimeType;
    private final long sizeBytes;

    private ChatReference(int type, String label, String path, @Nullable Uri uri,
                          @Nullable String mimeType, long sizeBytes) {
        this.type = type;
        this.label = label == null ? "" : label;
        this.path = path == null ? "" : path;
        this.uri = uri;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public static ChatReference file(String label, String path) {
        return new ChatReference(TYPE_FILE, label, path, null, null, 0L);
    }

    public static ChatReference folder(String label, String path) {
        return new ChatReference(TYPE_FOLDER, label, path, null, null, 0L);
    }

    public static ChatReference image(String label, Uri uri, @Nullable String mimeType, long sizeBytes) {
        return new ChatReference(TYPE_IMAGE, label, uri == null ? "" : uri.toString(), uri, mimeType, sizeBytes);
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }

    @Nullable
    public Uri getUri() {
        return uri;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String mentionText() {
        return "@" + label;
    }

    public String stableKey() {
        return type + ":" + path;
    }
}
