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
    private static final int DATABASE_VERSION = 2; // Incremented version
 
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SC_ID = "sc_id";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TYPE = "type"; // Added
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TOOL_NAME = "tool_name"; // Added
    private static final String COLUMN_TOOL_ARGS = "tool_args"; // Added
    private static final String COLUMN_TOOL_RESULT = "tool_result"; // Added
 
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
                COLUMN_TOOL_RESULT + " TEXT)";
        db.execSQL(createTable);
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_TYPE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_TOOL_NAME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_TOOL_ARGS + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_TOOL_RESULT + " TEXT");
            
            // Migrate is_user to type if necessary, but here we can just reset if simple
            // db.execSQL("UPDATE " + TABLE_MESSAGES + " SET " + COLUMN_TYPE + " = 1 WHERE is_user = 0");
        }
    }
 
    public void saveMessage(String scId, ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SC_ID, scId);
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TYPE, message.getType());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_TOOL_NAME, message.getToolName());
        values.put(COLUMN_TOOL_ARGS, message.getToolArgs());
        values.put(COLUMN_TOOL_RESULT, message.getToolResult());
        db.insert(TABLE_MESSAGES, null, values);
    }
 
    public void saveMessages(String scId, List<ChatMessage> messages) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_MESSAGES, COLUMN_SC_ID + "=?", new String[]{scId});
            
            for (ChatMessage msg : messages) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_SC_ID, scId);
                values.put(COLUMN_MESSAGE, msg.getMessage());
                values.put(COLUMN_TYPE, msg.getType());
                values.put(COLUMN_TIMESTAMP, msg.getTimestamp());
                values.put(COLUMN_TOOL_NAME, msg.getToolName());
                values.put(COLUMN_TOOL_ARGS, msg.getToolArgs());
                values.put(COLUMN_TOOL_RESULT, msg.getToolResult());
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
                new String[]{COLUMN_MESSAGE, COLUMN_TYPE, COLUMN_TIMESTAMP, COLUMN_TOOL_NAME, COLUMN_TOOL_ARGS, COLUMN_TOOL_RESULT},
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
                
                ChatMessage chatMsg;
                if (type == ChatMessage.TYPE_TOOL) {
                    chatMsg = new ChatMessage(toolName, toolArgs, timestamp);
                    chatMsg.setToolRunning(false);
                    chatMsg.setToolResult(toolResult);
                } else {
                    chatMsg = new ChatMessage(message, type == ChatMessage.TYPE_USER, timestamp);
                }
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
