package pro.sketchware.utility;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class IconImportLog {
    private static final String LOG_TAG = "IconImportLog";
    private static final String LOG_FOLDER = "SketchwareIA";
    private static final String LOG_FILE = "icon_import_log.txt";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private IconImportLog() {
    }

    public static File getLogFile() {
        return new File(
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_FOLDER),
                LOG_FILE
        );
    }

    public static synchronized void clear() {
        write("Icon import log started\nLog file: " + getLogFile().getAbsolutePath() + "\n", false);
    }

    public static synchronized void d(String tag, String message) {
        Log.d(tag, message);
        write(format(tag, message), true);
    }

    public static synchronized void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        StringWriter writer = new StringWriter();
        if (throwable != null) {
            throwable.printStackTrace(new PrintWriter(writer));
        }
        write(format(tag, message + (throwable == null ? "" : "\n" + writer)), true);
    }

    private static String format(String tag, String message) {
        return DATE_FORMAT.format(new Date()) + " [" + tag + "] " + message + "\n";
    }

    private static void write(String text, boolean append) {
        File logFile = getLogFile();
        File parent = logFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.e(LOG_TAG, "Could not create log folder: " + parent.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(logFile, append)) {
            writer.write(text);
            writer.flush();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not write icon import log: " + logFile.getAbsolutePath(), e);
        }
    }
}
