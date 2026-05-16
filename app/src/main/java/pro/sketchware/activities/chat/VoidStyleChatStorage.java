package pro.sketchware.activities.chat;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pro.sketchware.activities.chat.port.VoidPortChatThreadService;

/**
 * Stores chat threads in one JSON document, mirroring Void's
 * void.chatThreadStorageII application storage shape for Android.
 */
public class VoidStyleChatStorage {
    public static final String STORAGE_KEY = "void.chatThreadStorageII";
    private static final String FILE_NAME = STORAGE_KEY + ".json";

    private final File storageFile;

    public VoidStyleChatStorage(Context context) {
        this.storageFile = new File(context.getFilesDir(), FILE_NAME);
    }

    public boolean exists() {
        return storageFile.exists() && storageFile.length() > 0;
    }

    public String ensureDefaultThread(String scId) {
        String threadId = VoidPortChatThreadService.threadIdForProject(scId);
        JSONObject root = readRoot();
        JSONObject threads = root.optJSONObject("allThreads");
        if (threads == null) {
            threads = new JSONObject();
            put(root, "allThreads", threads);
        }
        if (!threads.has(threadId)) {
            put(threads, threadId, makeThread(threadId, scId, "Principal", "", "", System.currentTimeMillis()));
            if (!root.has("currentThreadId")) {
                put(root, "currentThreadId", threadId);
            }
            writeRoot(root);
        }
        return threadId;
    }

    public String createThread(String scId) {
        JSONObject root = readRoot();
        JSONObject threads = root.optJSONObject("allThreads");
        if (threads == null) {
            threads = new JSONObject();
            put(root, "allThreads", threads);
        }
        String threadId = "sketchware:" + safe(scId, "unknown") + ":" + UUID.randomUUID();
        put(threads, threadId, makeThread(threadId, scId, "Nova conversa", "", "", System.currentTimeMillis()));
        put(root, "currentThreadId", threadId);
        writeRoot(root);
        return threadId;
    }

    public void saveMessage(String scId, String threadId, ChatMessage message) {
        if (message == null) {
            return;
        }
        List<ChatMessage> messages = loadHistory(scId, threadId);
        messages.add(message);
        saveHistory(scId, threadId, messages);
    }

    public void saveHistory(String scId, String threadId, List<ChatMessage> messages) {
        String safeThreadId = safeThreadId(scId, threadId);
        JSONObject root = readRoot();
        JSONObject threads = root.optJSONObject("allThreads");
        if (threads == null) {
            threads = new JSONObject();
            put(root, "allThreads", threads);
        }
        JSONObject thread = threads.optJSONObject(safeThreadId);
        long now = System.currentTimeMillis();
        if (thread == null) {
            thread = makeThread(safeThreadId, scId, "Principal", "", "", now);
        }
        put(thread, "messages", serializeMessages(messages));
        put(thread, "state", buildThreadState(messages));
        put(thread, "lastModified", now);
        put(threads, safeThreadId, thread);
        put(root, "currentThreadId", safeThreadId);
        writeRoot(root);
    }

    public List<ChatMessage> loadHistory(String scId, String threadId) {
        String safeThreadId = safeThreadId(scId, threadId);
        JSONObject root = readRoot();
        JSONObject allThreads = root.optJSONObject("allThreads");
        JSONObject thread = allThreads == null ? null : allThreads.optJSONObject(safeThreadId);
        if (thread == null) {
            return new ArrayList<>();
        }
        return parseMessages(thread.optJSONArray("messages"));
    }

    public void clearHistory(String scId, String threadId) {
        saveHistory(scId, threadId, new ArrayList<>());
    }

    public void deleteProjectHistory(String scId) {
        JSONObject root = readRoot();
        JSONObject threads = root.optJSONObject("allThreads");
        if (threads == null) {
            return;
        }
        JSONArray names = threads.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            String id = names.optString(i, "");
            JSONObject thread = threads.optJSONObject(id);
            if (thread != null && safe(scId, "").equals(thread.optString("scId", ""))) {
                threads.remove(id);
            }
        }
        writeRoot(root);
    }

    public List<ChatThread> getThreads(String scId) {
        ensureDefaultThread(scId);
        List<ChatThread> result = new ArrayList<>();
        JSONObject threads = readRoot().optJSONObject("allThreads");
        JSONArray names = threads == null ? null : threads.names();
        for (int i = 0; names != null && i < names.length(); i++) {
            JSONObject thread = threads.optJSONObject(names.optString(i, ""));
            if (thread == null || !safe(scId, "").equals(thread.optString("scId", ""))) {
                continue;
            }
            result.add(new ChatThread(
                    thread.optString("id", ""),
                    thread.optString("scId", ""),
                    thread.optString("title", ""),
                    thread.optString("summary", ""),
                    thread.optLong("createdAt", 0L),
                    thread.optLong("lastModified", 0L),
                    thread.optString("activeModel", "")
            ));
        }
        result.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return result;
    }

    public void updateThreadSummary(String scId, String threadId, String title, String summary, String activeModel) {
        String safeThreadId = safeThreadId(scId, threadId);
        JSONObject root = readRoot();
        JSONObject threads = root.optJSONObject("allThreads");
        if (threads == null) {
            threads = new JSONObject();
            put(root, "allThreads", threads);
        }
        JSONObject thread = threads.optJSONObject(safeThreadId);
        if (thread == null) {
            thread = makeThread(safeThreadId, scId, title, summary, activeModel, System.currentTimeMillis());
        }
        put(thread, "title", safe(title, ""));
        put(thread, "summary", safe(summary, ""));
        put(thread, "activeModel", safe(activeModel, ""));
        put(thread, "lastModified", System.currentTimeMillis());
        put(threads, safeThreadId, thread);
        writeRoot(root);
    }

    private JSONObject readRoot() {
        String text = readText(storageFile);
        if (text == null || text.trim().isEmpty()) {
            return newRoot();
        }
        try {
            JSONObject root = new JSONObject(text);
            if (root.optJSONObject("allThreads") == null) {
                put(root, "allThreads", new JSONObject());
            }
            return root;
        } catch (Exception ignored) {
            return newRoot();
        }
    }

    private JSONObject newRoot() {
        JSONObject root = new JSONObject();
        put(root, "storageKey", STORAGE_KEY);
        put(root, "allThreads", new JSONObject());
        return root;
    }

    private JSONObject makeThread(String id, String scId, String title, String summary, String activeModel, long now) {
        JSONObject state = new JSONObject();
        put(state, "currCheckpointIdx", JSONObject.NULL);
        put(state, "stagingSelections", new JSONArray());

        JSONObject thread = new JSONObject();
        put(thread, "id", id);
        put(thread, "scId", safe(scId, ""));
        put(thread, "title", safe(title, ""));
        put(thread, "summary", safe(summary, ""));
        put(thread, "activeModel", safe(activeModel, ""));
        put(thread, "createdAt", now);
        put(thread, "lastModified", now);
        put(thread, "state", state);
        put(thread, "messages", new JSONArray());
        return thread;
    }

    private JSONArray serializeMessages(List<ChatMessage> messages) {
        JSONArray array = new JSONArray();
        for (ChatMessage message : messages == null ? new ArrayList<ChatMessage>() : messages) {
            if (message != null && !message.isStreaming()) {
                array.put(serializeMessage(message));
            }
        }
        return array;
    }

    private JSONObject serializeMessage(ChatMessage message) {
        JSONObject object = new JSONObject();
        put(object, "message", safe(message.getMessage(), ""));
        put(object, "type", message.getType());
        put(object, "timestamp", message.getTimestamp());
        put(object, "toolName", message.getToolName());
        put(object, "toolArgs", message.getToolArgs());
        put(object, "toolResult", message.getToolResult());
        put(object, "toolId", message.getToolId());
        put(object, "toolState", message.getToolState());
        put(object, "toolRunning", message.isToolRunning());
        put(object, "toolError", message.isToolError());
        put(object, "isExpanded", message.isExpanded());
        put(object, "reasoning", message.getReasoning());
        put(object, "status", message.getStatus());
        put(object, "requiresApproval", message.getRequiresApproval());
        put(object, "approved", message.isApproved());
        put(object, "rejected", message.isRejected());
        put(object, "checkpointId", message.getCheckpointId());
        put(object, "checkpointType", message.getCheckpointType());
        put(object, "checkpointSnapshots", parseObject(message.getCheckpointSnapshotsJson()));
        put(object, "imageReferences", serializeReferences(message.getImageReferences()));
        JSONObject state = new JSONObject();
        put(state, "stagingSelections", serializeReferences(message.getImageReferences()));
        put(state, "isBeingEdited", false);
        put(object, "state", state);
        return object;
    }

    private JSONObject buildThreadState(List<ChatMessage> messages) {
        JSONObject state = new JSONObject();
        int checkpointIdx = -1;
        for (int i = 0; messages != null && i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message != null && message.isCheckpoint()) {
                checkpointIdx = i;
            }
        }
        put(state, "currCheckpointIdx", checkpointIdx >= 0 ? checkpointIdx : JSONObject.NULL);
        put(state, "stagingSelections", new JSONArray());
        return state;
    }

    private List<ChatMessage> parseMessages(@Nullable JSONArray array) {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                continue;
            }
            int type = object.optInt("type", ChatMessage.TYPE_BOT);
            ChatMessage message;
            if (type == ChatMessage.TYPE_TOOL) {
                message = new ChatMessage(
                        object.optString("toolName", ""),
                        object.optString("toolArgs", ""),
                        object.optLong("timestamp", 0L),
                        object.optString("toolId", "")
                );
                message.setMessage(object.optString("message", ""));
                message.setToolResult(object.optString("toolResult", ""));
                message.setToolRunning(object.optBoolean("toolRunning", false));
                message.setToolError(object.optBoolean("toolError", false));
                message.setToolState(object.optString("toolState", ""));
                message.setRequiresApproval(object.optBoolean("requiresApproval", false));
                message.setApproved(object.optBoolean("approved", false));
                message.setRejected(object.optBoolean("rejected", false));
                message.setExpanded(object.optBoolean("isExpanded", true));
            } else if (type == ChatMessage.TYPE_USER || type == ChatMessage.TYPE_BOT) {
                message = new ChatMessage(object.optString("message", ""), type == ChatMessage.TYPE_USER, object.optLong("timestamp", 0L));
            } else if (type == ChatMessage.TYPE_CHECKPOINT) {
                message = new ChatMessage(object.optString("message", ""), ChatMessage.TYPE_CHECKPOINT, object.optLong("timestamp", 0L), object.optString("status", "Checkpoint"));
            } else {
                message = new ChatMessage(object.optString("message", ""), type, object.optLong("timestamp", 0L), object.optString("status", ""));
            }
            message.setReasoning(object.optString("reasoning", ""));
            message.setStatus(object.optString("status", ""));
            message.setCheckpointId(object.optString("checkpointId", ""));
            message.setCheckpointType(object.optString("checkpointType", ""));
            JSONObject snapshots = object.optJSONObject("checkpointSnapshots");
            message.setCheckpointSnapshotsJson(snapshots == null ? "" : snapshots.toString());
            message.setImageReferences(parseReferences(object.optJSONArray("imageReferences")));
            messages.add(message);
        }
        return messages;
    }

    private JSONArray serializeReferences(List<ChatReference> references) {
        JSONArray array = new JSONArray();
        for (ChatReference reference : references == null ? new ArrayList<ChatReference>() : references) {
            JSONObject object = new JSONObject();
            put(object, "type", reference.getType());
            put(object, "label", reference.getLabel());
            put(object, "path", reference.getPath());
            put(object, "mimeType", reference.getMimeType());
            put(object, "sizeBytes", reference.getSizeBytes());
            array.put(object);
        }
        return array;
    }

    private List<ChatReference> parseReferences(@Nullable JSONArray array) {
        List<ChatReference> references = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                continue;
            }
            if (object.optInt("type", 0) == ChatReference.TYPE_IMAGE) {
                references.add(ChatReference.image(
                        object.optString("label", ""),
                        Uri.parse(object.optString("path", "")),
                        object.optString("mimeType", ""),
                        object.optLong("sizeBytes", 0L)
                ));
            }
        }
        return references;
    }

    private String safeThreadId(String scId, String threadId) {
        return ChatMessage.hasVisibleText(threadId) ? threadId : ensureDefaultThread(scId);
    }

    private JSONObject parseObject(String json) {
        if (!ChatMessage.hasVisibleText(json)) {
            return new JSONObject();
        }
        try {
            return new JSONObject(json);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private void writeRoot(JSONObject root) {
        try (FileWriter writer = new FileWriter(storageFile, false)) {
            writer.write(root == null ? newRoot().toString() : root.toString());
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private String readText(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value == null ? JSONObject.NULL : value);
        } catch (Exception ignored) {
        }
    }

    private String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
