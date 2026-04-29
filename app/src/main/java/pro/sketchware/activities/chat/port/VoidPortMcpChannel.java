package pro.sketchware.activities.chat.port;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Android-safe port of electron-main/mcpChannel.ts.
 *
 * The mobile app cannot spawn desktop stdio/SSE MCP clients reliably, so this
 * class preserves config parsing, status reporting and tool naming semantics.
 */
public final class VoidPortMcpChannel {
    private VoidPortMcpChannel() {
    }

    public static final class ServerStatus {
        public final String name;
        public final String command;
        public final boolean enabled;
        public final String status;

        ServerStatus(String name, String command, boolean enabled, String status) {
            this.name = name == null ? "" : name;
            this.command = command == null ? "" : command;
            this.enabled = enabled;
            this.status = status == null ? "offline" : status;
        }
    }

    public static List<ServerStatus> readServerStatuses(SharedPreferences prefs) {
        List<ServerStatus> result = new ArrayList<>();
        JSONObject servers = VoidPortSettings.readMcpConfigObject(prefs).optJSONObject("mcpServers");
        JSONArray names = servers == null ? null : servers.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String name = names.optString(i, "");
            JSONObject server = servers.optJSONObject(name);
            if (server == null) {
                continue;
            }
            boolean enabled = server.optBoolean("enabled", true);
            String command = server.optString("command", "");
            if (command.isEmpty()) {
                command = server.optString("url", "");
            }
            String status = enabled ? "configured" : "offline";
            result.add(new ServerStatus(name, command, enabled, status));
        }
        return result;
    }

    public static String buildPromptSummary(SharedPreferences prefs) {
        List<ServerStatus> servers = readServerStatuses(prefs);
        if (servers.isEmpty()) {
            return "MCP: no configured servers";
        }
        StringBuilder builder = new StringBuilder("MCP servers:\n");
        for (ServerStatus server : servers) {
            builder.append("- ")
                    .append(server.name)
                    .append(" [")
                    .append(server.status)
                    .append("] ");
            if (!server.command.isEmpty()) {
                builder.append(server.command);
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    public static String addUniquePrefix(String serverName, String toolName) {
        String safeServer = slug(serverName);
        String safeTool = slug(toolName);
        if (safeServer.isEmpty()) {
            return safeTool;
        }
        return "mcp_" + safeServer + "_" + safeTool;
    }

    public static String removeMcpToolNamePrefix(String prefixedToolName) {
        if (prefixedToolName == null) {
            return "";
        }
        String value = prefixedToolName;
        if (value.startsWith("mcp_")) {
            value = value.substring(4);
        }
        int first = value.indexOf('_');
        return first >= 0 && first < value.length() - 1 ? value.substring(first + 1) : value;
    }

    private static String slug(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
