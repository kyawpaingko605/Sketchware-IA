package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import pro.sketchware.activities.chat.PromptConstants;
import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceRegistry;

public class ReadVoidSourceAssetTool implements Tool {
    private static final int DEFAULT_MAX_CHARS = 40_000;
    private static final int MAX_CHARS = 160_000;

    @Override
    public String getName() {
        return "read_void_source_asset";
    }

    @Override
    public String getDescription() {
        return "Reads one embedded Void source asset by exact path from the chat source registry.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Exact Void source asset path, as returned by list_void_source_assets."));
            properties.put("max_chars", new JSONObject()
                    .put("type", "integer")
                    .put("description", "Optional max characters to return. Defaults to 40000, max 160000."));

            params.put("properties", properties);
            params.put("required", new JSONArray().put("path"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) {
        String path = args.optString("path", "").trim();
        if (path.isEmpty()) {
            return "Error: path is required.";
        }

        SourceAsset asset = SourceRegistry.findByPath(path);
        if (asset == null) {
            return "Error: Void source asset not found: " + path;
        }

        int maxChars = args.optInt("max_chars", DEFAULT_MAX_CHARS);
        maxChars = Math.max(1, Math.min(maxChars, MAX_CHARS));

        String source = asset.source();
        boolean truncated = source.length() > maxChars;
        String visibleSource = truncated ? source.substring(0, maxChars) : source;

        StringBuilder builder = new StringBuilder();
        builder.append("Path: ").append(asset.path()).append("\n");
        builder.append("Bytes: ").append(asset.originalByteLength()).append("\n");
        builder.append("SHA-256: ").append(asset.sha256()).append("\n");
        builder.append("Truncated: ").append(truncated).append("\n\n");
        builder.append(PromptConstants.TRIPLE_TICK.get(0)).append("\n");
        builder.append(visibleSource);
        if (truncated) {
            builder.append("\n... asset truncated ...");
        }
        builder.append("\n").append(PromptConstants.TRIPLE_TICK.get(1));
        return builder.toString();
    }
}
