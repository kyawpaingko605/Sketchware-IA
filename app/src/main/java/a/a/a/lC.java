package a.a.a;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.utility.FileUtil;

public class lC {
    public static final String PROJECT_KIND_KEY = "project_kind";
    public static final String PROJECT_KIND_SKETCHWARE = "sketchware";
    public static final String PROJECT_KIND_ANDROID_STUDIO = "android_studio";
    private static final String ANDROID_STUDIO_PROJECTS_COMPAT = ".sketware_ide";
    public static DB a;

    public static ArrayList<HashMap<String, Object>> a() {
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        oB oBVar = new oB();
        Set<String> knownProjectIds = new HashSet<>();
        addProjectsFromRoot(arrayList, new File(wq.n()), oBVar, knownProjectIds, PROJECT_KIND_SKETCHWARE);
        for (File root : getAndroidStudioProjectsRoots()) {
            addProjectsFromRoot(arrayList, root, oBVar, knownProjectIds, PROJECT_KIND_ANDROID_STUDIO);
        }
        return arrayList;
    }

    public static File getAndroidStudioProjectDirectory(String scId) {
        for (File root : getAndroidStudioProjectsRoots()) {
            File projectDirectory = new File(root, scId);
            if (projectDirectory.exists()) {
                return projectDirectory;
            }
        }
        return new File(wq.getAndroidStudioProjectPath(scId));
    }

    private static ArrayList<File> getAndroidStudioProjectsRoots() {
        ArrayList<File> roots = new ArrayList<>();
        roots.add(new File(wq.getAndroidStudioProjectsRoot()));
        File compatRoot = new File(wq.getAbsolutePathOf(ANDROID_STUDIO_PROJECTS_COMPAT));
        if (!sameFile(roots.get(0), compatRoot)) {
            roots.add(compatRoot);
        }
        return roots;
    }

    private static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalPath().equals(second.getCanonicalPath());
        } catch (Exception ignored) {
            return first.getAbsolutePath().equals(second.getAbsolutePath());
        }
    }

    private static void addProjectsFromRoot(ArrayList<HashMap<String, Object>> projects, File root, oB fileUtil,
                                            Set<String> knownProjectIds, String projectKind) {
        File[] listFiles = root.listFiles();
        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            try {
                if (!file.isDirectory()) {
                    continue;
                }
                if (knownProjectIds.contains(file.getName())) {
                    continue;
                }
                if (PROJECT_KIND_ANDROID_STUDIO.equals(projectKind)) {
                    HashMap<String, Object> metadata = readAndroidStudioProjectMetadata(file, true);
                    if (metadata != null) {
                        projects.add(metadata);
                        knownProjectIds.add(file.getName());
                    }
                    continue;
                }
                File projectMetadata = new File(file, "project");
                if (projectMetadata.exists()) {
                    HashMap<String, Object> metadata = vB.a(fileUtil.a(fileUtil.h(projectMetadata.getAbsolutePath())));
                    if (yB.c(metadata, "sc_id").equals(file.getName())) {
                        if (yB.c(metadata, PROJECT_KIND_KEY).isEmpty()) {
                            metadata.put(PROJECT_KIND_KEY, projectKind);
                        }
                        if (PROJECT_KIND_ANDROID_STUDIO.equals(projectKind)) {
                            metadata.put("proj_type", 2);
                            metadata.put("studio_path", file.getAbsolutePath());
                        }
                        projects.add(metadata);
                        knownProjectIds.add(file.getName());
                    }
                }
            } catch (Throwable e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    public static HashMap<String, Object> registerAndroidStudioProject(String scId) {
        return readAndroidStudioProjectMetadata(getAndroidStudioProjectDirectory(scId), true);
    }

    private static HashMap<String, Object> readAndroidStudioProjectMetadata(File projectDirectory, boolean saveMetadata) {
        if (projectDirectory == null || !projectDirectory.exists() || !projectDirectory.isDirectory()) {
            return null;
        }
        HashMap<String, Object> metadata = readProjectMetadataFile(new File(projectDirectory, "project"));
        if (metadata == null && !looksLikeAndroidStudioProject(projectDirectory)) {
            return null;
        }
        if (metadata == null) {
            metadata = createAndroidStudioProjectMetadata(projectDirectory);
        }

        String scId = projectDirectory.getName();
        metadata.put("sc_id", scId);
        metadata.put(PROJECT_KIND_KEY, PROJECT_KIND_ANDROID_STUDIO);
        metadata.put("proj_type", 2);
        metadata.put("studio_path", projectDirectory.getAbsolutePath());

        HashMap<String, Object> detected = createAndroidStudioProjectMetadata(projectDirectory);
        putIfEmpty(metadata, "my_sc_pkg_name", yB.c(detected, "my_sc_pkg_name"));
        putIfEmpty(metadata, "my_ws_name", yB.c(detected, "my_ws_name"));
        putIfEmpty(metadata, "my_app_name", yB.c(detected, "my_app_name"));
        putIfEmpty(metadata, "sc_ver_code", yB.c(detected, "sc_ver_code"));
        putIfEmpty(metadata, "sc_ver_name", yB.c(detected, "sc_ver_name"));
        putIfEmpty(metadata, "sketchware_ver", 61);
        putIfEmpty(metadata, "custom_icon", false);
        putIfEmpty(metadata, "isIconAdaptive", false);

        if (saveMetadata) {
            saveProjectMetadata(projectDirectory.getAbsolutePath(), metadata);
        }
        return metadata;
    }

    private static HashMap<String, Object> readProjectMetadataFile(File projectMetadata) {
        if (projectMetadata == null || !projectMetadata.exists()) {
            return null;
        }
        try {
            oB fileUtil = new oB();
            HashMap<String, Object> metadata = vB.a(fileUtil.a(fileUtil.h(projectMetadata.getAbsolutePath())));
            return metadata == null ? null : metadata;
        } catch (Throwable e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
    }

    private static HashMap<String, Object> createAndroidStudioProjectMetadata(File projectDirectory) {
        String scId = projectDirectory.getName();
        String appBuildGradle = readText(firstExistingFile(projectDirectory,
                "app" + File.separator + "build.gradle",
                "app" + File.separator + "build.gradle.kts"));
        String settingsGradle = readText(firstExistingFile(projectDirectory, "settings.gradle", "settings.gradle.kts"));
        String manifest = readText(new File(projectDirectory, "app" + File.separator + "src" + File.separator + "main" + File.separator + "AndroidManifest.xml"));
        String strings = readText(new File(projectDirectory, "app" + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator + "values" + File.separator + "strings.xml"));

        String projectName = firstMatch(settingsGradle, "rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]", projectDirectory.getName());
        String packageName = firstGradleString(appBuildGradle, "applicationId");
        if (packageName.isEmpty()) {
            packageName = firstMatch(manifest, "\\bpackage\\s*=\\s*\"([^\"]+)\"", fallbackPackageName(scId));
        }
        String versionCode = firstGradleNumber(appBuildGradle, "versionCode", "1");
        String versionName = firstGradleString(appBuildGradle, "versionName");
        if (versionName.isEmpty()) {
            versionName = "1.0";
        }

        String appName = firstStringResource(strings, "app_name");
        if (appName.isEmpty()) {
            String manifestLabel = firstMatch(manifest, "\\bandroid:label\\s*=\\s*\"([^\"]+)\"", "");
            appName = manifestLabel.startsWith("@") || manifestLabel.isEmpty() ? projectName : manifestLabel;
        }

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("sc_id", scId);
        metadata.put("my_sc_pkg_name", packageName);
        metadata.put("my_ws_name", projectName);
        metadata.put("my_app_name", appName);
        metadata.put("sc_ver_code", versionCode);
        metadata.put("sc_ver_name", versionName);
        metadata.put("sketchware_ver", 61);
        metadata.put("custom_icon", false);
        metadata.put("isIconAdaptive", false);
        metadata.put(PROJECT_KIND_KEY, PROJECT_KIND_ANDROID_STUDIO);
        metadata.put("proj_type", 2);
        metadata.put("studio_path", projectDirectory.getAbsolutePath());
        return metadata;
    }

    private static boolean looksLikeAndroidStudioProject(File projectDirectory) {
        return new File(projectDirectory, "app" + File.separator + "build.gradle").exists()
                || new File(projectDirectory, "app" + File.separator + "build.gradle.kts").exists()
                || new File(projectDirectory, "settings.gradle").exists()
                || new File(projectDirectory, "settings.gradle.kts").exists()
                || new File(projectDirectory, "build.gradle").exists()
                || new File(projectDirectory, "build.gradle.kts").exists();
    }

    private static File firstExistingFile(File baseDirectory, String... relativePaths) {
        for (String relativePath : relativePaths) {
            File file = new File(baseDirectory, relativePath);
            if (file.exists()) {
                return file;
            }
        }
        return new File(baseDirectory, relativePaths[0]);
    }

    private static void putIfEmpty(HashMap<String, Object> metadata, String key, Object value) {
        if (metadata == null || key == null || value == null) {
            return;
        }
        String current = yB.c(metadata, key);
        if (current.isEmpty()) {
            metadata.put(key, value);
        }
    }

    private static String readText(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "";
        }
        return FileUtil.readFileIfExist(file.getAbsolutePath());
    }

    private static String firstGradleString(String content, String key) {
        return firstMatch(content, "\\b" + Pattern.quote(key) + "\\b\\s*(?:=\\s*)?['\"]([^'\"]+)['\"]", "");
    }

    private static String firstGradleNumber(String content, String key, String fallback) {
        return firstMatch(content, "\\b" + Pattern.quote(key) + "\\b\\s*(?:=\\s*)?(\\d+)", fallback);
    }

    private static String firstStringResource(String content, String name) {
        String value = firstMatch(content, "(?s)<string\\s+name=\"" + Pattern.quote(name) + "\"\\s*>\\s*(.*?)\\s*</string>", "");
        return unescapeXml(value);
    }

    private static String firstMatch(String content, String regex, String fallback) {
        if (content == null || content.isEmpty()) {
            return fallback;
        }
        Matcher matcher = Pattern.compile(regex).matcher(content);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }

    private static String fallbackPackageName(String scId) {
        String suffix = scId == null ? "project" : scId.replaceAll("[^A-Za-z0-9_]", "");
        if (suffix.isEmpty()) {
            suffix = "project";
        }
        return "com.my.project" + suffix;
    }

    private static String unescapeXml(String value) {
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    public static HashMap<String, Object> a(String str) {
        for (HashMap<String, Object> stringObjectHashMap : a()) {
            if (yB.c(stringObjectHashMap, "my_sc_pkg_name").equals(str) && yB.b(stringObjectHashMap, "proj_type") == 1) {
                return stringObjectHashMap;
            }
        }
        return null;
    }

    public static void a(Context context, String str) {
        File file = new File(wq.c(str));
        if (file.exists()) {
            oB oBVar = new oB();
            oBVar.a(file);
            oBVar.b(wq.d(str));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(wq.g());
            stringBuilder.append(File.separator);
            stringBuilder.append(str);
            oBVar.b(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(wq.t());
            stringBuilder.append(File.separator);
            stringBuilder.append(str);
            oBVar.b(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(wq.d());
            stringBuilder.append(File.separator);
            stringBuilder.append(str);
            oBVar.b(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(wq.e());
            stringBuilder.append(File.separator);
            stringBuilder.append(str);
            oBVar.b(stringBuilder.toString());
            oBVar.b(wq.b(str));
            oBVar.b(wq.a(str));
            stringBuilder = new StringBuilder();
            stringBuilder.append("D01_");
            stringBuilder.append(str);
            new DB(context, stringBuilder.toString()).a();
            stringBuilder = new StringBuilder();
            stringBuilder.append("D02_");
            stringBuilder.append(str);
            new DB(context, stringBuilder.toString()).a();
            stringBuilder = new StringBuilder();
            stringBuilder.append("D03_");
            stringBuilder.append(str);
            new DB(context, stringBuilder.toString()).a();
            stringBuilder = new StringBuilder();
            stringBuilder.append("D04_");
            stringBuilder.append(str);
            new DB(context, stringBuilder.toString()).a();
        }
    }

    public static void deleteAndroidStudioProject(String str) {
        new oB().b(getAndroidStudioProjectDirectory(str).getAbsolutePath());
    }

    public static void a(Context context, boolean z) {
        if (a == null) {
            a = new DB(context, "P15");
        }
    }

    public static void a(String str, HashMap<String, Object> hashMap) {
        hashMap.put(PROJECT_KIND_KEY, PROJECT_KIND_SKETCHWARE);
        saveProjectMetadata(wq.c(str), hashMap);
    }

    public static void saveAndroidStudioProject(String str, HashMap<String, Object> hashMap) {
        hashMap.put(PROJECT_KIND_KEY, PROJECT_KIND_ANDROID_STUDIO);
        hashMap.put("proj_type", 2);
        hashMap.put("studio_path", wq.getAndroidStudioProjectPath(str));
        saveProjectMetadata(wq.getAndroidStudioProjectPath(str), hashMap);
    }

    private static void saveProjectMetadata(String projectDirectory, HashMap<String, Object> hashMap) {
        File file = new File(projectDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }
        String projectMetadataPath = projectDirectory + File.separator + "project";
        String a = vB.a(hashMap);
        oB oBVar = new oB();
        try {
            oBVar.a(projectMetadataPath, oBVar.d(a));
        } catch (Throwable e) {
            Log.e("ERROR", e.getMessage(), e);
        }
    }

    public static String b() {
        int parseInt = Integer.parseInt("600") + 1;
        for (HashMap<String, Object> stringObjectHashMap : a()) {
            try {
                parseInt = Math.max(parseInt, Integer.parseInt(yB.c(stringObjectHashMap, "sc_id")) + 1);
            } catch (Exception ignored) {
            }
        }
        return String.valueOf(parseInt);
    }

    public static HashMap<String, Object> b(String str) {
        HashMap<String, Object> nativeProject = readProjectMetadata(new File(wq.c(str)), str, PROJECT_KIND_SKETCHWARE);
        if (nativeProject != null) {
            return nativeProject;
        }
        for (File root : getAndroidStudioProjectsRoots()) {
            HashMap<String, Object> androidStudioProject = readProjectMetadata(new File(root, str), str, PROJECT_KIND_ANDROID_STUDIO);
            if (androidStudioProject != null) {
                return androidStudioProject;
            }
        }
        return null;
    }

    private static HashMap<String, Object> readProjectMetadata(File projectDirectory, String expectedId, String projectKind) {
        if (PROJECT_KIND_ANDROID_STUDIO.equals(projectKind)) {
            return readAndroidStudioProjectMetadata(projectDirectory, true);
        }
        Throwable e;
        oB oBVar = new oB();
        HashMap<String, Object> hashMap = null;
        try {
            if (!projectDirectory.exists()) {
                return null;
            }
            String path = projectDirectory.getAbsolutePath() + File.separator + "project";
            HashMap<String, Object> a = vB.a(oBVar.a(oBVar.h(path)));
            try {
                if (!yB.c(a, "sc_id").equals(expectedId)) {
                    return null;
                }
                if (yB.c(a, PROJECT_KIND_KEY).isEmpty()) {
                    a.put(PROJECT_KIND_KEY, projectKind);
                }
                if (PROJECT_KIND_ANDROID_STUDIO.equals(projectKind)) {
                    a.put("proj_type", 2);
                    a.put("studio_path", projectDirectory.getAbsolutePath());
                }
                return a;
            } catch (Exception e2) {
                e = e2;
                hashMap = a;
                Log.e("ERROR", e.getMessage(), e);
                return hashMap;
            }
        } catch (Exception e3) {
            e = e3;
            Log.e("ERROR", e.getMessage(), e);
            return hashMap;
        }
    }

    public static void b(String str, HashMap<String, Object> hashMap) {
        File file = getProjectDirectoryForUpdate(str, hashMap);
        if (file.exists()) {
            String path = file + File.separator + "project";
            oB fileUtil = new oB();
            try {
                HashMap<String, Object> a = vB.a(fileUtil.a(fileUtil.h(path)));
                if (yB.c(a, "sc_id").equals(str)) {
                    if (hashMap.containsKey("isIconAdaptive")) {
                        a.put("isIconAdaptive", hashMap.get("isIconAdaptive"));
                    }
                    if (hashMap.containsKey("custom_icon")) {
                        a.put("custom_icon", hashMap.get("custom_icon"));
                    }
                    a.put("my_sc_pkg_name", hashMap.get("my_sc_pkg_name"));
                    a.put("my_ws_name", hashMap.get("my_ws_name"));
                    a.put("my_app_name", hashMap.get("my_app_name"));
                    a.put("sc_ver_code", hashMap.get("sc_ver_code"));
                    a.put("sc_ver_name", hashMap.get("sc_ver_name"));
                    a.put("sketchware_ver", hashMap.get("sketchware_ver"));
                    a.put("color_accent", hashMap.get("color_accent"));
                    a.put("color_primary", hashMap.get("color_primary"));
                    a.put("color_primary_dark", hashMap.get("color_primary_dark"));
                    a.put("color_control_highlight", hashMap.get("color_control_highlight"));
                    a.put("color_control_normal", hashMap.get("color_control_normal"));
                    if (hashMap.containsKey(PROJECT_KIND_KEY)) {
                        a.put(PROJECT_KIND_KEY, hashMap.get(PROJECT_KIND_KEY));
                    }
                    if (hashMap.containsKey("proj_type")) {
                        a.put("proj_type", hashMap.get("proj_type"));
                    }
                    if (hashMap.containsKey("studio_path")) {
                        a.put("studio_path", hashMap.get("studio_path"));
                    }
                    fileUtil.a(path, fileUtil.d(vB.a(a)));
                }
            } catch (Throwable e) {
                Log.e("DEBUG", e.getMessage(), e);
            }
        }
    }

    private static File getProjectDirectoryForUpdate(String scId, HashMap<String, Object> hashMap) {
        File nativeProject = new File(wq.c(scId));
        File androidStudioProject = getAndroidStudioProjectDirectory(scId);
        if (isAndroidStudioProject(hashMap) && androidStudioProject.exists()) {
            return androidStudioProject;
        }
        if (nativeProject.exists()) {
            return nativeProject;
        }
        return androidStudioProject;
    }

    public static boolean isAndroidStudioProject(HashMap<String, Object> projectMap) {
        return PROJECT_KIND_ANDROID_STUDIO.equals(yB.c(projectMap, PROJECT_KIND_KEY)) || yB.b(projectMap, "proj_type") == 2;
    }

    public static String c() {
        ArrayList<HashMap<String, Object>> var0 = a();
        ArrayList<Integer> projectIndices = new ArrayList<>();

        for (HashMap<String, Object> stringObjectHashMap : var0) {
            String workspaceName = yB.c(stringObjectHashMap, "my_ws_name");
            if (workspaceName.equals("NewProject")) {
                projectIndices.add(1);
            } else if (workspaceName.indexOf("NewProject") == 0) {
                try {
                    projectIndices.add(Integer.parseInt(workspaceName.substring(10)));
                } catch (Exception ignored) {
                }
            }
        }

        projectIndices.sort(new IntegerComparator());
        int var3 = 0;

        for (int nextProjectIndex : projectIndices) {
            int var5 = var3 + 1;
            if (nextProjectIndex == var5) {
                var3 = var5;
            } else {
                if (nextProjectIndex == var3) {
                    continue;
                }
                break;
            }
        }

        if (var3 == 0) {
            return "NewProject";
        } else {
            return "NewProject" + (var3 + 1);
        }
    }

    public static void d() {
        for (String str : a.c().keySet()) {
            a(str, a.g(str));
        }
        a.a();
    }

    private static class IntegerComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer first, Integer second) {
            return first.compareTo(second);
        }
    }
}
