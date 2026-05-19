package mod.hey.studios.project;

import static com.besome.sketch.Config.VAR_DEFAULT_MIN_SDK_VERSION;
import static com.besome.sketch.Config.VAR_DEFAULT_TARGET_SDK_VERSION;

import android.app.Activity;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a.a.a.lC;
import pro.sketchware.databinding.DialogProjectSettingsBinding;
import pro.sketchware.utility.FileUtil;

public class ProjectSettingsDialog {

    private final Activity activity;
    private final ProjectSettings settings;
    private final boolean androidStudioProject;

    public ProjectSettingsDialog(Activity activity, String sc_id) {
        this(activity, sc_id, false);
    }

    public ProjectSettingsDialog(Activity activity, String sc_id, boolean androidStudioProject) {
        this.activity = activity;
        settings = new ProjectSettings(sc_id);
        this.androidStudioProject = androidStudioProject;
    }

    public void show() {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        DialogProjectSettingsBinding binding = DialogProjectSettingsBinding.inflate(activity.getLayoutInflater());

        dialog.setOnShowListener(bsd -> {
            var b = (BottomSheetDialog) bsd;
            var parent = b.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (parent != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        String defaultMinSdk = String.valueOf(VAR_DEFAULT_MIN_SDK_VERSION);
        String defaultTargetSdk = String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION);
        if (androidStudioProject) {
            defaultMinSdk = readAndroidStudioGradleNumber(defaultMinSdk, "minSdkVersion", "minSdk");
            defaultTargetSdk = readAndroidStudioGradleNumber(defaultTargetSdk, "targetSdkVersion", "targetSdk");
        }

        binding.etMinimumSdkVersion.setText(settings.getValue(ProjectSettings.SETTING_MINIMUM_SDK_VERSION, defaultMinSdk));
        binding.etTargetSdkVersion.setText(settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, defaultTargetSdk));
        binding.etApplicationClassName.setText(settings.getValue(ProjectSettings.SETTING_APPLICATION_CLASS, ".SketchApplication"));

        binding.cbEnableViewbinding.setChecked(
                settings.getValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, "false").equals("true"));
        binding.cbRemoveOldMethods.setChecked(
                settings.getValue(ProjectSettings.SETTING_DISABLE_OLD_METHODS, "true").equals("true"));
        binding.cbUseNewMaterialComponentsAppTheme.setChecked(
                settings.getValue(ProjectSettings.SETTING_ENABLE_BRIDGELESS_THEMES, "false").equals("true"));

        binding.enableViewbinding.setOnClickListener(v -> binding.cbEnableViewbinding.performClick());
        binding.removeOldMethods.setOnClickListener(v -> binding.cbRemoveOldMethods.performClick());
        binding.useNewMaterialComponentsAppTheme.setOnClickListener(v -> binding.cbUseNewMaterialComponentsAppTheme.performClick());

        binding.etMinimumSdkVersion.setTag(ProjectSettings.SETTING_MINIMUM_SDK_VERSION);
        binding.etTargetSdkVersion.setTag(ProjectSettings.SETTING_TARGET_SDK_VERSION);
        binding.etApplicationClassName.setTag(ProjectSettings.SETTING_APPLICATION_CLASS);
        binding.cbEnableViewbinding.setTag(ProjectSettings.SETTING_ENABLE_VIEWBINDING);
        binding.cbRemoveOldMethods.setTag(ProjectSettings.SETTING_DISABLE_OLD_METHODS);
        binding.cbUseNewMaterialComponentsAppTheme.setTag(ProjectSettings.SETTING_ENABLE_BRIDGELESS_THEMES);

        dialog.setContentView(binding.getRoot());

        View[] preferences = {
                binding.etMinimumSdkVersion,
                binding.etTargetSdkVersion,
                binding.etApplicationClassName,
                binding.cbEnableViewbinding,
                binding.cbRemoveOldMethods,
                binding.cbUseNewMaterialComponentsAppTheme
        };

        binding.save.setOnClickListener(v -> {
            settings.setValues(preferences);
            if (androidStudioProject) {
                applyAndroidStudioProjectConfiguration(binding);
            }
            dialog.dismiss();
        });
        binding.cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String readAndroidStudioGradleNumber(String defaultValue, String... keys) {
        File buildFile = getAndroidStudioBuildFile();
        if (!buildFile.exists()) {
            return defaultValue;
        }
        String content = FileUtil.readFileIfExist(buildFile.getAbsolutePath());
        for (String key : keys) {
            Matcher matcher = gradleNumberPattern(key).matcher(content);
            if (matcher.find()) {
                return matcher.group(2);
            }
        }
        return defaultValue;
    }

    private void applyAndroidStudioProjectConfiguration(DialogProjectSettingsBinding binding) {
        File buildFile = getAndroidStudioBuildFile();
        if (!buildFile.exists()) {
            return;
        }

        String content = FileUtil.readFileIfExist(buildFile.getAbsolutePath());
        String minSdk = binding.etMinimumSdkVersion.getText() == null ? "" : binding.etMinimumSdkVersion.getText().toString().trim();
        String targetSdk = binding.etTargetSdkVersion.getText() == null ? "" : binding.etTargetSdkVersion.getText().toString().trim();
        if (!minSdk.isEmpty()) {
            content = replaceGradleNumber(content, minSdk, "minSdkVersion", "minSdk");
        }
        if (!targetSdk.isEmpty()) {
            content = replaceGradleNumber(content, targetSdk, "targetSdkVersion", "targetSdk");
            content = raiseCompileSdkIfNeeded(content, targetSdk);
        }
        FileUtil.writeFile(buildFile.getAbsolutePath(), content);
    }

    private File getAndroidStudioBuildFile() {
        File projectDirectory = lC.getAndroidStudioProjectDirectory(settings.sc_id);
        File groovyBuildFile = new File(projectDirectory,
                "app" + File.separator + "build.gradle");
        if (groovyBuildFile.exists()) {
            return groovyBuildFile;
        }
        return new File(projectDirectory, "app" + File.separator + "build.gradle.kts");
    }

    private Pattern gradleNumberPattern(String key) {
        return Pattern.compile("(\\b" + Pattern.quote(key) + "\\b\\s*(?:=\\s*)?)(\\d+)");
    }

    private String replaceGradleNumber(String content, String value, String... keys) {
        for (String key : keys) {
            Matcher matcher = gradleNumberPattern(key).matcher(content);
            if (matcher.find()) {
                return matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + value));
            }
        }
        return content;
    }

    private String raiseCompileSdkIfNeeded(String content, String targetSdk) {
        int target = parseInt(targetSdk, -1);
        if (target < 0) {
            return content;
        }
        int compile = parseFirstGradleNumber(content, "compileSdkVersion", "compileSdk");
        if (compile >= target) {
            return content;
        }
        return replaceGradleNumber(content, targetSdk, "compileSdkVersion", "compileSdk");
    }

    private int parseFirstGradleNumber(String content, String... keys) {
        for (String key : keys) {
            Matcher matcher = gradleNumberPattern(key).matcher(content);
            if (matcher.find()) {
                return parseInt(matcher.group(2), -1);
            }
        }
        return -1;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
