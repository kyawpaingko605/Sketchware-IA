package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolManager {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private volatile Tool activeTool;

    public ToolManager() {
        registerTool(new ListProjectFilesTool());
        registerTool(new DecryptTool());
        registerTool(new EncryptTool());
        registerTool(new ShellTool());
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public void registerTool(Tool tool) {
        if (tool == null || tool.getName() == null || tool.getName().trim().isEmpty()) {
            return;
        }
        tools.put(tool.getName(), tool);
    }

    public JSONArray getToolsAsMCP() {
        JSONArray array = new JSONArray();

        for (Tool tool : tools.values()) {
            try {
                JSONObject toolObj = new JSONObject();
                JSONObject function = new JSONObject();

                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.put("parameters", tool.getParameters());

                toolObj.put("type", "function");
                toolObj.put("function", function);

                array.put(toolObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return array;
    }

    public String executeTool(String scId, String name, String arguments) {
        Tool tool = null;
        try {
            if (name == null || name.trim().isEmpty()) {
                return "Erro: nome da ferramenta nao informado.";
            }

            tool = tools.get(name);

            if (tool == null) {
                return "Erro: ferramenta '" + name + "' nao encontrada.";
            }

            JSONObject args;

            if (arguments == null || arguments.trim().isEmpty() || "null".equals(arguments.trim())) {
                args = new JSONObject();
            } else {
                args = new JSONObject(arguments);
            }

            activeTool = tool;
            return tool.execute(scId, args);

        } catch (Exception e) {
            return "Erro ao executar ferramenta '" + name + "': " + e.getMessage();
        } finally {
            if (activeTool == tool) {
                activeTool = null;
            }
        }
    }

    public void cancelActiveTool() {
        Tool tool = activeTool;
        if (tool != null) {
            tool.cancelExecution();
        }
    }
}
