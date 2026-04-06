package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ToolManager {
    private final Map<String, Tool> tools = new HashMap<>();

    public ToolManager() {
        // Registrar as ferramentas padrão
        registerTool(new ShellTool());
        registerTool(new DecryptTool());
        registerTool(new EncryptTool());
    }

    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * Retorna a definição de todas as ferramentas no formato MCP (JSON Schema)
     * esperado pelo Groq/OpenAI.
     */
    public JSONArray getToolsAsMCP() {
        JSONArray array = new JSONArray();
        for (Tool tool : tools.values()) {
            try {
                JSONObject toolObj = new JSONObject();
                toolObj.put("type", "function");
                JSONObject function = new JSONObject();
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.put("parameters", tool.getParameters());
                toolObj.put("function", function);
                array.put(toolObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    /**
     * Executa uma ferramenta pelo nome e argumentos JSON.
     */
    public String executeTool(String scId, String name, String arguments) throws Exception {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new Exception("Tool '" + name + "' not found.");
        }
        
        JSONObject args;
        if (arguments == null || arguments.isEmpty() || arguments.equals("null")) {
            args = new JSONObject();
        } else {
            args = new JSONObject(arguments);
        }
        
        return tool.execute(scId, args);
    }
}
