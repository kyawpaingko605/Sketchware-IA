package pro.sketchware.activities.chat;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import pro.sketchware.util.ProjectPathResolver;
import pro.sketchware.util.SketchwareFileEncryptor;

public class ChatCheckpointManager {
    private static final String ROOT_DIR = "chat_checkpoints";

    private final Context context;

    public static class CheckpointEntry {
        public final String id;
        public final String scId;
        public final String toolId;
        public final String toolName;
        public final String filePath;
        public final String beforeContent;
        public final boolean existedBefore;
        public final long createdAt;

        public CheckpointEntry(String id, String scId, String toolId, String toolName, String filePath,
                               String beforeContent, boolean existedBefore, long createdAt) {
            this.id = id;
            this.scId = scId;
            this.toolId = toolId;
            this.toolName = toolName;
            this.filePath = filePath;
            this.beforeContent = beforeContent;
            this.existedBefore = existedBefore;
            this.createdAt = createdAt;
        }

        public JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("scId", scId);
                object.put("toolId", toolId);
                object.put("toolName", toolName);
                object.put("filePath", filePath);
                object.put("beforeContent", beforeContent);
                object.put("existedBefore", existedBefore);
                object.put("createdAt", createdAt);
            } catch (Exception ignored) {
            }
            return object;
        }

        @Nullable
        public static CheckpointEntry fromJson(String json) {
            try {
                JSONObject object = new JSONObject(json);
                return new CheckpointEntry(
                        object.optString("id"),
                        object.optString("scId"),
                        object.optString("toolId"),
                        object.optString("toolName"),
                        object.optString("filePath"),
                        object.optString("beforeContent"),
                        object.optBoolean("existedBefore", true),
                        object.optLong("createdAt")
                );
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static class RollbackResult {
        public final boolean success;
        public final String message;
        @Nullable
        public final CheckpointEntry entry;

        public RollbackResult(boolean success, String message, @Nullable CheckpointEntry entry) {
            this.success = success;
            this.message = message;
            this.entry = entry;
        }
    }

    public ChatCheckpointManager(Context context) {
        this.context = context.getApplicationContext();
    }

    @Nullable
    public CheckpointEntry createCheckpoint(String scId, String toolId, String toolName, String filePath,
                                            String beforeContent, boolean existedBefore) {
        if (scId == null || scId.trim().isEmpty() || filePath == null || filePath.trim().isEmpty()) {
            return null;
        }

        long createdAt = System.currentTimeMillis();
        String id = "cp_" + createdAt + "_" + UUID.randomUUID().toString().replace("-", "");
        CheckpointEntry entry = new CheckpointEntry(
                id,
                scId,
                toolId,
                toolName,
                filePath,
                beforeContent == null ? "" : beforeContent,
                existedBefore,
                createdAt
        );

        File projectDir = getProjectDir(scId);
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            return null;
        }

        File checkpointFile = new File(projectDir, entry.id + ".json");
        if (!writeText(checkpointFile, entry.toJson().toString())) {
            return null;
        }
        return entry;
    }

    public boolean hasCheckpoint(String scId) {
        return getLatestCheckpoint(scId) != null;
    }

    @Nullable
    public CheckpointEntry getLatestCheckpoint(String scId) {
        File projectDir = getProjectDir(scId);
        File[] files = projectDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return null;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            CheckpointEntry entry = readEntry(file);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    public RollbackResult rollbackLatestCheckpoint(String scId) {
        CheckpointEntry entry = getLatestCheckpoint(scId);
        if (entry == null) {
            return new RollbackResult(false, "Nenhum checkpoint disponivel para rollback.", null);
        }

        try {
            ProjectPathResolver.ResolvedPath resolvedPath = ProjectPathResolver.resolveForWrite(scId, entry.filePath);
            if (resolvedPath == null) {
                return new RollbackResult(false, "Nao foi possivel resolver o arquivo do checkpoint.", entry);
            }

            File file = resolvedPath.getFile();
            boolean restored;
            if (!entry.existedBefore) {
                restored = !file.exists() || file.delete();
            } else {
                restored = SketchwareFileEncryptor.encryptAndSaveFile(scId, entry.filePath, entry.beforeContent);
            }

            if (!restored) {
                return new RollbackResult(false, "Falha ao restaurar o arquivo do checkpoint.", entry);
            }

            deleteEntryFile(entry);
            return new RollbackResult(true, "Rollback aplicado em " + entry.filePath + ".", entry);
        } catch (Exception e) {
            return new RollbackResult(false, "Erro no rollback: " + e.getMessage(), entry);
        }
    }

    private void deleteEntryFile(CheckpointEntry entry) {
        File file = new File(getProjectDir(entry.scId), entry.id + ".json");
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private File getProjectDir(String scId) {
        return new File(new File(context.getFilesDir(), ROOT_DIR), scId);
    }

    @Nullable
    private CheckpointEntry readEntry(File file) {
        String text = readText(file);
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return CheckpointEntry.fromJson(text);
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

    private boolean writeText(File file, String content) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content == null ? "" : content);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
