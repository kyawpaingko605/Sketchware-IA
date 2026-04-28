package pro.sketchware.activities.chat.source;

public interface SourceAsset {
    String path();
    String sha256();
    int originalByteLength();
    String source();
}
