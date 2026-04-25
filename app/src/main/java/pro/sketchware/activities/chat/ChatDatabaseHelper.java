package pro.sketchware.activities.chat;
 
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
 
import java.util.ArrayList;
import java.util.List;
 
public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 3;
 
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
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
 
    private static final int MAX_MESSAGES_PER_PROJECT = 200;

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
                COLUMN_REJECTED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
 
    public void saveMessage(String scId, ChatMessage message) {
        if (scId == null || message == null) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = buildContentValues(scId, message);
        db.insert(TABLE_MESSAGES, null, values);

        trimOldMessages(db, scId);
    }

    private ContentValues buildContentValues(String scId, ChatMessage message) {
        ContentValues values = new ContentValues();
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
        return values;
    }

    private void trimOldMessages(SQLiteDatabase db, String scId) {
        try {
            // Delete messages that are NOT in the top 200 most recent for this project
            String deleteQuery = "DELETE FROM " + TABLE_MESSAGES + 
                    " WHERE " + COLUMN_ID + " IN (SELECT " + COLUMN_ID + 
                    " FROM " + TABLE_MESSAGES + 
                    " WHERE " + COLUMN_SC_ID + " = ?" + 
                    " ORDER BY " + COLUMN_TIMESTAMP + " DESC, " + COLUMN_ID + " DESC" +
                    " LIMIT -1 OFFSET " + MAX_MESSAGES_PER_PROJECT + ")";
            db.execSQL(deleteQuery, new String[]{scId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    public void saveMessages(String scId, List<ChatMessage> messages) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=?", new String[]{scId});

            for (ChatMessage msg : messages) {
                ContentValues values = buildContentValues(scId, msg);
                db.insert(TABLE_MESSAGES, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
 
    public List<ChatMessage> getHistory(String scId) {
        List<ChatMessage> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES,
                new String[]{
                        COLUMN_MESSAGE, COLUMN_TYPE, COLUMN_TIMESTAMP, COLUMN_TOOL_NAME, COLUMN_TOOL_ARGS, COLUMN_TOOL_RESULT,
                        COLUMN_TOOL_ID, COLUMN_TOOL_RUNNING, COLUMN_TOOL_ERROR, COLUMN_REASONING, COLUMN_STATUS,
                        COLUMN_REQUIRES_APPROVAL, COLUMN_APPROVED, COLUMN_REJECTED
                },
                COLUMN_SC_ID + "=?", new String[]{scId},
                null, null, COLUMN_TIMESTAMP + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String message = cursor.getString(0);
                int type = cursor.getInt(1);
                long timestamp = cursor.getLong(2);
                String toolName = cursor.getString(3);
                String toolArgs = cursor.getString(4);
                String toolResult = cursor.getString(5);
                String toolId = cursor.getString(6);
                boolean toolRunning = cursor.getInt(7) == 1;
                boolean toolError = cursor.getInt(8) == 1;
                String reasoning = cursor.getString(9);
                String status = cursor.getString(10);
                boolean requiresApproval = cursor.getInt(11) == 1;
                boolean approved = cursor.getInt(12) == 1;
                boolean rejected = cursor.getInt(13) == 1;

                ChatMessage chatMsg;
                if (type == ChatMessage.TYPE_TOOL) {
                    chatMsg = new ChatMessage(toolName, toolArgs, timestamp, toolId);
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
                history.add(chatMsg);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return history;
    }
 
    public void clearHistory(String scId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=?", new String[]{scId});
    }
}
