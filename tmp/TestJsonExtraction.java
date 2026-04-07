
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestJsonExtraction {
    public static void main(String[] args) {
        String test1 = "Here is the JSON:\n```json\n[{\"id\": \"1\", \"params\": [\"a\", \"b\"]}]\n```\nAnd some text.";
        String test2 = "Fix: [{\"id\": \"1\", \"params\": [\"a\"]}] end.";
        String test3 = "Markdown: ```\n{\"key\": \"value\"}\n```";
        String test4 = "Nested: [{\"a\": [1, 2], \"b\": {\"c\": 3}}] text";

        System.out.println("Test 1: " + (extractJsonFromResponse(test1).equals("[{\"id\": \"1\", \"params\": [\"a\", \"b\"]}]")));
        System.out.println("Test 2: " + (extractJsonFromResponse(test2).equals("[{\"id\": \"1\", \"params\": [\"a\"]}]")));
        System.out.println("Test 3: " + (extractJsonFromResponse(test3).equals("{\"key\": \"value\"}")));
        System.out.println("Test 4: " + (extractJsonFromResponse(test4).equals("[{\"a\": [1, 2], \"b\": {\"c\": 3}}]")));
        
        System.out.println("Extracted 4: " + extractJsonFromResponse(test4));
    }

    private static String extractJsonFromResponse(String response) {
        if (response == null) return null;
        
        // Use greedy patterns and check for both arrays and objects
        String[] patterns = {
            "```json\\s*([\\[\\{].*[\\}\\]])\\s*```",
            "```\\s*([\\[\\{].*[\\}\\]])\\s*```",
            "([\\[\\{].*[\\}\\]])"
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher m = p.matcher(response);
            
            if (m.find()) {
                String json = m.group(1 != 0 && m.groupCount() >= 1 ? 1 : 0);
                // Clean up markdown if the greedy match captured backticks
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                return json;
            }
        }
        
        // More robust fallback: find first [ or { and last ] or }
        int arrStart = response.indexOf("[");
        int objStart = response.indexOf("{");
        int start = -1;
        
        if (arrStart != -1 && objStart != -1) start = Math.min(arrStart, objStart);
        else if (arrStart != -1) start = arrStart;
        else if (objStart != -1) start = objStart;
        
        int arrEnd = response.lastIndexOf("]");
        int objEnd = response.lastIndexOf("}");
        int end = -1;
        
        if (arrEnd != -1 && objEnd != -1) end = Math.max(arrEnd, objEnd);
        else if (arrEnd != -1) end = arrEnd;
        else if (objEnd != -1) end = objEnd;
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return null;
    }
}
