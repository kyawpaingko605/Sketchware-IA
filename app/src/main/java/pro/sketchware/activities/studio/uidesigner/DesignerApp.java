package pro.sketchware.activities.studio.uidesigner;

import android.app.Application;
import android.content.Context;

import pro.sketchware.SketchApplication;

public class DesignerApp extends Application
{
    public static Context context;

    @Override
    public void onCreate()
    {
        super.onCreate();
        
        context = getApplicationContext();
        
    }
    
    public static Context getContext()
    {
        if (context != null) {
            return context;
        }
        return SketchApplication.getContext();
    }

    public static void install(Context value)
    {
        if (value != null) {
            context = value.getApplicationContext();
        }
    }
}
