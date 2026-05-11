package pro.sketchware.activities.studio;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.HashMap;

import a.a.a.lC;
import a.a.a.wq;
import pro.sketchware.utility.SketchwareUtil;

public class AndroidStudioProjectActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SC_ID = "sc_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String scId = getIntent().getStringExtra(EXTRA_SC_ID);
        HashMap<String, Object> project = lC.b(scId);
        String projectName = valueOf(project, "my_ws_name", "Android Studio Project");
        String appName = valueOf(project, "my_app_name", projectName);

        setTitle(projectName);
        setContentView(createContent(scId, projectName, appName));
        SketchwareUtil.toast("Projeto Android Studio selecionado");
    }

    private LinearLayout createContent(String scId, String projectName, String appName) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.WHITE);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(this);
        title.setText("Projeto Android Studio");
        title.setTextColor(Color.rgb(32, 33, 36));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView message = new TextView(this);
        message.setText(appName + "\n" + projectName + "\n\n" + wq.getAndroidStudioProjectPath(scId));
        message.setTextColor(Color.rgb(95, 99, 104));
        message.setTextSize(14);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(0, 1.15f);
        root.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return root;
    }

    private String valueOf(HashMap<String, Object> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isEmpty() ? fallback : text;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
