package pro.sketchware.ia.tools;

import org.json.JSONObject;

import java.util.Locale;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceRegistry;

public class ListVoidSourceAssetsTool implements Tool {
    private static final int DEFAULT_LIMIT = 120;
    private static final int MAX_LIMIT = 500;

    @Override
    public String getName() {
        return "list_void_source_assets";
    }

    @Override
    public String getDescription() {
        return "Lists embedded Void source assets available to the chat, optionally filtered by path text.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("query", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional case-insensitive text filter for asset paths."));
            properties.put("limit", new JSONObject()
                    .put("type", "integer")
                    .put("description", "Optional max number of assets to list. Defaults to 120, max 500."));

            params.put("properties", properties);
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) {
        String query = args.optString("query", "").trim().toLowerCase(Locale.ROOT);
        int limit = args.optInt("limit", DEFAULT_LIMIT);
        limit = Math.max(1, Math.min(limit, MAX_LIMIT));

        StringBuilder builder = new StringBuilder();
        builder.append("Embedded Void source assets: ").append(SourceRegistry.ALL.size()).append("\n");
        if (!query.isEmpty()) {
            builder.append("Filter: ").append(query).append("\n");
        }
        builder.append("\n");

        int matched = 0;
        int printed = 0;
        for (SourceAsset asset : SourceRegistry.ALL) {
            if (asset == null || asset.path() == null) {
                continue;
            }
            String path = asset.path();
            if (!query.isEmpty() && !path.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            matched++;
            if (printed >= limit) {
                continue;
            }
            builder.append("- ")
                    .append(path)
                    .append(" (")
                    .append(asset.originalByteLength())
                    .append(" bytes, sha256=")
                    .append(asset.sha256())
                    .append(")\n");
            printed++;
        }

        if (matched == 0) {
            return "No embedded Void source assets matched.";
        }
        if (matched > printed) {
            builder.append("...truncated after ").append(printed).append(" of ").append(matched).append(" matches.");
        }
        return builder.toString().trim();
    }
}
