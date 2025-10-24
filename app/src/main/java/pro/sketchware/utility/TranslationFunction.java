package pro.sketchware.utility;

import android.content.Context;
import android.content.res.Resources;

import java.util.Map;
import pro.sketchware.utility.TranslationFunction;

public class TranslationFunction {
    private static TranslationFunction instance;
    private final Context context;
    private final Map<String, String> externalStrings;
    private final Resources originalResources;

    private TranslationFunction(Context context, Map<String, String> externalStrings) {
        this.context = context.getApplicationContext();
        this.externalStrings = externalStrings;
        this.originalResources = context.getResources();
    }

    public static void initialize(Context context) {
        Map<String, String> strings = GlobalStrings.loadExternalStrings(context);
        instance = new TranslationFunction(context, strings);
    }

    public static TranslationFunction get() {
        if (instance == null) {
            throw new IllegalStateException("TranslationFunction não inicializado! Chame initialize() primeiro.");
        }
        return instance;
    }

    /**
     * 🔥 MAGIC! Substitui getString(R.string.key) com fallback automático
     */
    public String getString(int resId) {
        try {
            String key = originalResources.getResourceEntryName(resId);
            String externalValue = externalStrings.get(key);
            if (externalValue != null) {
                return externalValue;
            }
            return originalResources.getString(resId);
        } catch (Resources.NotFoundException e) {
            return "";
        }
    }

    /**
     * Para compatibilidade com Context.getString()
     */
    public static String getString(Context context, int resId) {
        return get().getString(resId);
    }
}
