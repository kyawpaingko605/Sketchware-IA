package pro.sketchware.activities.chat.source;

public final class SourceChunks {
    private SourceChunks() {
    }

    public static String join(String[] chunks) {
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            builder.append(chunk);
        }
        return builder.toString();
    }
}
