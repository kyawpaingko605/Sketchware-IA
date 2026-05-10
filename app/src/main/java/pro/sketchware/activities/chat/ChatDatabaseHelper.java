package pro.sketchware.activities.chat;
 
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
 
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 4;
 
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_THREADS = "threads";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_THREAD_ID = "thread_id";
    private static final String COLUMN_SC_ID = "sc_id";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TYPE = "type"; // Added
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TOOL_NAME = "tool_name"; // Added
    private static final String COLUMN_TOOL_ARGS = "tool_args"; // Added
    private static final String COLUMN_TOOL_RESULT = "tool_result"; // Added
    private static final String COLUMN_TOOL_ID = "tool_id";
    private static final String COLUMN_TOOL_RUNNING = "tool_running";
    private static final String COLUMN_TOOL_ERROR = "tool_error";
    private static final String COLUMN_REASONING = "reasoning";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_REQUIRES_APPROVAL = "requires_approval";
    private static final String COLUMN_APPROVED = "approved";
    private static final String COLUMN_REJECTED = "rejected";
    private static final String COLUMN_IMAGE_REFERENCES = "image_references";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_SUMMARY = "summary";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_ACTIVE_MODEL = "active_model";
 
    private static final int MAX_MESSAGES_PER_PROJECT = 200;

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        String createMessagesTable = "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_THREAD_ID + " TEXT, " +
                COLUMN_SC_ID + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TYPE + " INTEGER, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_TOOL_NAME + " TEXT, " +
                COLUMN_TOOL_ARGS + " TEXT, " +
                COLUMN_TOOL_RESULT + " TEXT, " +
                COLUMN_TOOL_ID + " TEXT, " +
                COLUMN_TOOL_RUNNING + " INTEGER DEFAULT 0, " +
                COLUMN_TOOL_ERROR + " INTEGER DEFAULT 0, " +
                COLUMN_REASONING + " TEXT, " +
                COLUMN_STATUS + " TEXT, " +
                COLUMN_REQUIRES_APPROVAL + " INTEGER DEFAULT 0, " +
                COLUMN_APPROVED + " INTEGER DEFAULT 0, " +
                COLUMN_REJECTED + " INTEGER DEFAULT 0, " +
                COLUMN_IMAGE_REFERENCES + " TEXT)";
        db.execSQL(createMessagesTable);

        String createThreadsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_THREADS + " (" +
                COLUMN_THREAD_ID + " TEXT PRIMARY KEY, " +
                COLUMN_SC_ID + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_SUMMARY + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER, " +
                COLUMN_UPDATED_AT + " INTEGER, " +
                COLUMN_ACTIVE_MODEL + " TEXT)";
        db.execSQL(createThreadsTable);
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
        if (oldVersion < 2) {
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TYPE, "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_NAME, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_ARGS, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_RESULT, "TEXT");
        }
        if (oldVersion < 3) {
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_ID, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_RUNNING, "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_TOOL_ERROR, "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_REASONING, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_STATUS, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_REQUIRES_APPROVAL, "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_APPROVED, "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_REJECTED, "INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_THREAD_ID, "TEXT");
            addColumnIfNotExists(db, TABLE_MESSAGES, COLUMN_IMAGE_REFERENCES, "TEXT");
            migrateDefaultThreads(db);
        }
    }

    private void addColumnIfNotExists(SQLiteDatabase db, String table, String column, String type) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            boolean exists = false;
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    private void migrateDefaultThreads(SQLiteDatabase db) {
        try {
            db.execSQL("UPDATE " + TABLE_MESSAGES + " SET " + COLUMN_THREAD_ID + " = " +
                    COLUMN_SC_ID + " || ':default' WHERE " + COLUMN_THREAD_ID + " IS NULL OR " +
                    COLUMN_THREAD_ID + " = ''");
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_THREADS + " (" +
                    COLUMN_THREAD_ID + ", " + COLUMN_SC_ID + ", " + COLUMN_TITLE + ", " +
                    COLUMN_SUMMARY + ", " + COLUMN_CREATED_AT + ", " + COLUMN_UPDATED_AT + ", " +
                    COLUMN_ACTIVE_MODEL + ") SELECT " + COLUMN_SC_ID + " || ':default', " +
                    COLUMN_SC_ID + ", 'Principal', '', MIN(" + COLUMN_TIMESTAMP + "), MAX(" +
                    COLUMN_TIMESTAMP + "), '' FROM " + TABLE_MESSAGES + " GROUP BY " + COLUMN_SC_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveMessage(String scId, ChatMessage message) {
        saveMessage(scId, ensureDefaultThread(scId), message);
    }

    public void saveMessage(String scId, String threadId, ChatMessage message) {
        if (scId == null || message == null) return;

        SQLiteDatabase db = this.getWritableDatabase();
        String safeThreadId = safeThreadId(scId, threadId);
        ensureThreadRow(db, scId, safeThreadId);
        ContentValues values = buildContentValues(scId, safeThreadId, message);
        db.insert(TABLE_MESSAGES, null, values);

        trimOldMessages(db, scId, safeThreadId);
    }

    private ContentValues buildContentValues(String scId, String threadId, ChatMessage message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_THREAD_ID, threadId);
        values.put(COLUMN_SC_ID, scId);
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TYPE, message.getType());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_TOOL_NAME, message.getToolName());
        values.put(COLUMN_TOOL_ARGS, message.getToolArgs());
        values.put(COLUMN_TOOL_RESULT, message.getToolResult());
        values.put(COLUMN_TOOL_ID, message.getToolId());
        values.put(COLUMN_TOOL_RUNNING, message.isToolRunning() ? 1 : 0);
        values.put(COLUMN_TOOL_ERROR, message.isToolError() ? 1 : 0);
        values.put(COLUMN_REASONING, message.getReasoning());
        values.put(COLUMN_STATUS, message.getStatus());
        values.put(COLUMN_REQUIRES_APPROVAL, message.getRequiresApproval() ? 1 : 0);
        values.put(COLUMN_APPROVED, message.isApproved() ? 1 : 0);
        values.put(COLUMN_REJECTED, message.isRejected() ? 1 : 0);
        values.put(COLUMN_IMAGE_REFERENCES, serializeImageReferences(message.getImageReferences()));
        return values;
    }

    private void trimOldMessages(SQLiteDatabase db, String scId, String threadId) {
        try {
            // Delete messages that are NOT in the top 200 most recent for this project
            String deleteQuery = "DELETE FROM " + TABLE_MESSAGES + 
                    " WHERE " + COLUMN_ID + " IN (SELECT " + COLUMN_ID + 
                    " FROM " + TABLE_MESSAGES + 
                    " WHERE " + COLUMN_SC_ID + " = ? AND " + COLUMN_THREAD_ID + " = ?" +
                    " ORDER BY " + COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC" +
                    " LIMIT -1 OFFSET " + MAX_MESSAGES_PER_PROJECT + ")";
            db.execSQL(deleteQuery, new String[]{scId, threadId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    public void saveMessages(String scId, List<ChatMessage> messages) {
        saveMessages(scId, ensureDefaultThread(scId), messages);
    }

    public void saveMessages(String scId, String threadId, List<ChatMessage> messages) {
        SQLiteDatabase db = this.getWritableDatabase();
        String safeThreadId = safeThreadId(scId, threadId);
        ensureThreadRow(db, scId, safeThreadId);
        db.beginTransaction();
        try {
            db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=? AND " + COLUMN_THREAD_ID + "=?",
                    new String[]{scId, safeThreadId});

            for (ChatMessage msg : messages) {
                ContentValues values = buildContentValues(scId, safeThreadId, msg);
                db.insert(TABLE_MESSAGES, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
 
    public List<ChatMessage> getHistory(String scId) {
        return getHistory(scId, ensureDefaultThread(scId));
    }

    public List<ChatMessage> getHistory(String scId, String threadId) {
        List<ChatMessage> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String safeThreadId = safeThreadId(scId, threadId);
        Cursor cursor = db.query(TABLE_MESSAGES,
                new String[]{
                        COLUMN_MESSAGE, COLUMN_TYPE, COLUMN_TIMESTAMP, COLUMN_TOOL_NAME, COLUMN_TOOL_ARGS, COLUMN_TOOL_RESULT,
                        COLUMN_TOOL_ID, COLUMN_TOOL_RUNNING, COLUMN_TOOL_ERROR, COLUMN_REASONING, COLUMN_STATUS,
                        COLUMN_REQUIRES_APPROVAL, COLUMN_APPROVED, COLUMN_REJECTED, COLUMN_IMAGE_REFERENCES
                },
                COLUMN_SC_ID + "=? AND " + COLUMN_THREAD_ID + "=?", new String[]{scId, safeThreadId},
                null, null, COLUMN_TIMESTAMP + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String message = normalizeStoredText(cursor.getString(0));
                int type = cursor.getInt(1);
                long timestamp = cursor.getLong(2);
                String toolName = normalizeStoredText(cursor.getString(3));
                String toolArgs = normalizeStoredText(cursor.getString(4));
                String toolResult = normalizeStoredText(cursor.getString(5));
                String toolId = cursor.getString(6);
                boolean toolRunning = cursor.getInt(7) == 1;
                boolean toolError = cursor.getInt(8) == 1;
                String reasoning = normalizeStoredText(cursor.getString(9));
                String status = normalizeStoredText(cursor.getString(10));
                boolean requiresApproval = cursor.getInt(11) == 1;
                boolean approved = cursor.getInt(12) == 1;
                boolean rejected = cursor.getInt(13) == 1;
                String imageReferencesJson = cursor.getString(14);

                if (type == ChatMessage.TYPE_AWAITING_USER) {
                    continue;
                }
                if (type == ChatMessage.TYPE_BOT
                        && !ChatMessage.hasVisibleText(message)
                        && !ChatMessage.hasVisibleText(reasoning)
                        && !ChatMessage.hasVisibleText(status)) {
                    continue;
                }

                ChatMessage chatMsg;
                if (type == ChatMessage.TYPE_TOOL) {
                    chatMsg = new ChatMessage(toolName, toolArgs, timestamp, toolId);
                    chatMsg.setMessage(message);
                    chatMsg.setToolRunning(toolRunning);
                    chatMsg.setToolResult(toolResult);
                    chatMsg.setToolError(toolError);
                    chatMsg.setRequiresApproval(requiresApproval);
                    chatMsg.setApproved(approved);
                    chatMsg.setRejected(rejected);
                } else {
                    if (type == ChatMessage.TYPE_USER || type == ChatMessage.TYPE_BOT) {
                        chatMsg = new ChatMessage(message, type == ChatMessage.TYPE_USER, timestamp);
                    } else {
                        chatMsg = new ChatMessage(message, type, timestamp, status);
                    }
                }
                chatMsg.setStatus(status);
                chatMsg.setReasoning(reasoning);
                chatMsg.setImageReferences(parseImageReferences(imageReferencesJson));
                history.add(chatMsg);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return history;
    }

    private String normalizeStoredText(String value) {
        if (!ChatMessage.hasVisibleText(value)) {
            return "";
        }
        return value;
    }
 
    public void clearHistory(String scId) {
        clearHistory(scId, ensureDefaultThread(scId));
    }

    public void clearHistory(String scId, String threadId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=? AND " + COLUMN_THREAD_ID + "=?",
                new String[]{scId, safeThreadId(scId, threadId)});
    }

    public void deleteProjectHistory(String scId) {
        if (scId == null || scId.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=?", new String[]{scId});
            db.delete(TABLE_THREADS, COLUMN_SC_ID + "=?", new String[]{scId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String ensureDefaultThread(String scId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String threadId = defaultThreadId(scId);
        ensureThreadRow(db, scId, threadId);
        return threadId;
    }

    public String createThread(String scId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String threadId = scId + ":" + UUID.randomUUID();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COLUMN_THREAD_ID, threadId);
        values.put(COLUMN_SC_ID, scId);
        values.put(COLUMN_TITLE, "Nova conversa");
        values.put(COLUMN_SUMMARY, "");
        values.put(COLUMN_CREATED_AT, now);
        values.put(COLUMN_UPDATED_AT, now);
        values.put(COLUMN_ACTIVE_MODEL, "");
        db.insert(TABLE_THREADS, null, values);
        return threadId;
    }

    public List<ChatThread> getThreads(String scId) {
        ensureDefaultThread(scId);
        List<ChatThread> threads = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_THREADS,
                new String[]{COLUMN_THREAD_ID, COLUMN_SC_ID, COLUMN_TITLE, COLUMN_SUMMARY,
                        COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_ACTIVE_MODEL},
                COLUMN_SC_ID + "=?", new String[]{scId},
                null, null, COLUMN_UPDATED_AT + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                threads.add(new ChatThread(
                        cursor.getString(0),
                        cursor.getString(1),
                        normalizeStoredText(cursor.getString(2)),
                        normalizeStoredText(cursor.getString(3)),
                        cursor.getLong(4),
                        cursor.getLong(5),
                        normalizeStoredText(cursor.getString(6))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return threads;
    }

    public void updateThreadSummary(String scId, String threadId, String title, String summary, String activeModel) {
        if (scId == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        String safeThreadId = safeThreadId(scId, threadId);
        ensureThreadRow(db, scId, safeThreadId);
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_SUMMARY, summary);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        values.put(COLUMN_ACTIVE_MODEL, activeModel);
        db.update(TABLE_THREADS, values, COLUMN_SC_ID + "=? AND " + COLUMN_THREAD_ID + "=?",
                new String[]{scId, safeThreadId});
    }

    private void ensureThreadRow(SQLiteDatabase db, String scId, String threadId) {
        if (scId == null || threadId == null) {
            return;
        }
        try (Cursor cursor = db.query(TABLE_THREADS, new String[]{COLUMN_THREAD_ID},
                COLUMN_THREAD_ID + "=?", new String[]{threadId}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COLUMN_THREAD_ID, threadId);
        values.put(COLUMN_SC_ID, scId);
        values.put(COLUMN_TITLE, "Principal");
        values.put(COLUMN_SUMMARY, "");
        values.put(COLUMN_CREATED_AT, now);
        values.put(COLUMN_UPDATED_AT, now);
        values.put(COLUMN_ACTIVE_MODEL, "");
        db.insertWithOnConflict(TABLE_THREADS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private String safeThreadId(String scId, String threadId) {
        if (threadId != null && !threadId.trim().isEmpty()) {
            return threadId;
        }
        return defaultThreadId(scId);
    }

    private String defaultThreadId(String scId) {
        return (scId == null ? "unknown" : scId) + ":default";
    }

    private String serializeImageReferences(List<ChatReference> references) {
        JSONArray array = new JSONArray();
        if (references == null) {
            return array.toString();
        }
        for (ChatReference reference : references) {
            if (reference == null || reference.getType() != ChatReference.TYPE_IMAGE) {
                continue;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("label", reference.getLabel());
                obj.put("uri", reference.getPath());
                obj.put("mimeType", reference.getMimeType());
                obj.put("sizeBytes", reference.getSizeBytes());
                array.put(obj);
            } catch (Exception ignored) {
            }
        }
        return array.toString();
    }

    private List<ChatReference> parseImageReferences(String raw) {
        List<ChatReference> references = new ArrayList<>();
        if (!ChatMessage.hasVisibleText(raw)) {
            return references;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String uriValue = obj.optString("uri", "");
                if (uriValue.isEmpty()) {
                    continue;
                }
                references.add(ChatReference.image(
                        obj.optString("label", "reference-image"),
                        Uri.parse(uriValue),
                        obj.optString("mimeType", "image/*"),
                        obj.optLong("sizeBytes", 0L)
                ));
            }
        } catch (Exception ignored) {
        }
        return references;
    }
}
