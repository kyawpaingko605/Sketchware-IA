package pro.sketchware.ia;

import pro.sketchware.network.GroqClient;
import pro.sketchware.network.MorphClient;
import java.io.IOException;
import java.util.*;

public final class GeradorDeLayout {

    private final String texto;
    private final String currentLayout;
    private final List<LayoutHistoryManager.HistoryEntry> history;

    public GeradorDeLayout(String texto) {
        this.texto = texto;
        this.currentLayout = null;
        this.history = new ArrayList<>();
    }

    public GeradorDeLayout(String texto, String currentLayout) {
        this.texto = texto;
        this.currentLayout = currentLayout;
        this.history = new ArrayList<>();
    }

    public GeradorDeLayout(String texto, String currentLayout, List<LayoutHistoryManager.HistoryEntry> history) {
        this.texto = texto;
        this.currentLayout = currentLayout;
        this.history = history != null ? history : new ArrayList<>();
    }

    private String montarPromptBase() {
        StringJoiner sj = new StringJoiner("\n");

        sj.add("You are an assistant that generates valid Android XML layouts for Sketchware.");
        sj.add("Goal: Simple, functional, Material Design-like layouts using only Sketchware components.");
        sj.add("");
        sj.add("== RULES ==");
        sj.add("1. Root layout: <LinearLayout> vertical, match_parent both dimensions.");
        sj.add("2. Use only Sketchware-supported attributes:");
        sj.add("   - Common: id, layout_width/height, layout_margin, padding, background, enabled, clickable, alpha, translationX/Y, scaleX/Y");
        sj.add("   - TextView/EditText/Button: text, textSize, textStyle, textColor, hint, textColorHint");
        sj.add("   - ImageView: src, scaleType, contentDescription");
        sj.add("   - CheckBox/RadioButton/Switch: text, checked");
        sj.add("   - ProgressBar/SeekBar: max, progress, indeterminate");
        sj.add("   - ListView: dividerHeight, choiceMode");
        sj.add("   - Spinner: prompt");
        sj.add("   - CalendarView: firstDayOfWeek");
        sj.add("");
        sj.add("3. Assign android:id to all interactive views (e.g. @+id/button1).");
        sj.add("4. Visual balance:");
        sj.add("   - Margins/paddings: 8dp or 16dp");
        sj.add("   - textSize in sp");
        sj.add("   - Colors: #000000, #FFFFFF, #2196F3");
        sj.add("");
        sj.add("5. Use only these components:");
        Map<String, List<String>> supported = getViewBeanParserSupportedTypes();
        sj.add("   - Layouts: " + String.join(", ", supported.get("layouts")));
        sj.add("   - Widgets: " + String.join(", ", supported.get("widgets")));
        sj.add("");
        sj.add("6. Output strictly XML — no explanations, comments, or code blocks.");
        sj.add("");
        sj.add("== DESIGN GUIDELINES ==");
        sj.add("- Structure vertically (LinearLayout).");
        sj.add("- Nest layouts when needed.");
        sj.add("- Keep hierarchy clear: title → content → actions.");
        sj.add("- Buttons: minimum height 48dp.");
        sj.add("");
        sj.add("== EXAMPLE ==");
        sj.add("<LinearLayout");
        sj.add("    android:layout_width=\"match_parent\"");
        sj.add("    android:layout_height=\"match_parent\"");
        sj.add("    android:orientation=\"vertical\">");
        sj.add("");
        sj.add("    <TextView");
        sj.add("        android:id=\"@+id/title\"");
        sj.add("        android:layout_width=\"wrap_content\"");
        sj.add("        android:layout_height=\"wrap_content\"");
        sj.add("        android:text=\"Hello World\"");
        sj.add("        android:textSize=\"20sp\"");
        sj.add("        android:textColor=\"#000000\"");
        sj.add("        android:layout_margin=\"16dp\" />");
        sj.add("");
        sj.add("    <Button");
        sj.add("        android:id=\"@+id/button1\"");
        sj.add("        android:layout_width=\"match_parent\"");
        sj.add("        android:layout_height=\"wrap_content\"");
        sj.add("        android:text=\"Click Here\"");
        sj.add("        android:layout_margin=\"16dp\"");
        sj.add("        android:background=\"#2196F3\"");
        sj.add("        android:textColor=\"#FFFFFF\" />");
        sj.add("");
        sj.add("</LinearLayout>");

        return sj.toString();
    }

    private Map<String, List<String>> getViewBeanParserSupportedTypes() {
        Map<String, List<String>> types = new HashMap<>();
        List<String> layouts = Arrays.asList(
            "LinearLayout", "RelativeLayout", "HorizontalScrollView", "ScrollView",
            "TabLayout", "BottomNavigationView", "CardView", "CollapsingToolbarLayout",
            "TextInputLayout", "SwipeRefreshLayout", "RadioGroup", "NestedScrollView"
        );

        List<String> widgets = Arrays.asList(
            "Button", "TextView", "EditText", "ImageView", "WebView", "ProgressBar", "ListView",
            "Spinner", "CheckBox", "Switch", "SeekBar", "CalendarView", "AdView", "MapView",
            "RadioButton", "RatingBar", "VideoView", "SearchView", "AutoCompleteTextView",
            "MultiAutoCompleteTextView", "GridView", "AnalogClock", "DatePicker", "TimePicker",
            "DigitalClock", "ViewPager", "BadgeView", "PatternLockView", "WaveSideBar",
            "MaterialButton", "SignInButton", "CircleImageView", "LottieAnimationView",
            "YoutubePlayerView", "OTPView", "CodeView", "RecyclerView", "ImageButton",
            "MaterialSwitch", "TextInputEditText"
        );

        types.put("layouts", layouts);
        types.put("widgets", widgets);
        return types;
    }

    public String getTexto() {
        return texto;
    }

    public String gerarLayout() throws IOException {
        // Passo 1: Groq gera o layout inicial
        String prompt = montarPromptBase();
        
        // Adicionar histórico de conversas anteriores se houver (limitado a 5000 caracteres)
        if (!history.isEmpty()) {
            prompt += "\n\n== CONVERSATION HISTORY ==\n";
            prompt += "Previous interactions for context:\n\n";
            
            StringBuilder historyBuilder = new StringBuilder();
            int totalLength = 0;
            final int MAX_HISTORY_LENGTH = 5000;
            
            // Adicionar entradas do histórico mais recente primeiro (últimas entradas)
            for (int i = history.size() - 1; i >= 0; i--) {
                LayoutHistoryManager.HistoryEntry entry = history.get(i);
                String entryText = "--- Previous Request " + (history.size() - i) + " ---\n"
                    + "User: " + entry.userPrompt + "\n"
                    + "Generated Layout:\n" + entry.generatedLayout + "\n\n";
                
                // Se adicionar esta entrada ultrapassar o limite, parar
                if (totalLength + entryText.length() > MAX_HISTORY_LENGTH) {
                    break;
                }
                
                historyBuilder.insert(0, entryText);
                totalLength += entryText.length();
            }
            
            prompt += historyBuilder.toString();
            prompt += "== CURRENT REQUEST ==\n";
        }
        
        // Se houver layout atual, incluir como contexto
        if (currentLayout != null && !currentLayout.trim().isEmpty()) {
            prompt += "\n\n== CURRENT LAYOUT ==\n";
            prompt += "The current layout XML is:\n";
            prompt += currentLayout;
            prompt += "\n\n== USER REQUEST ==\n";
            prompt += "Based on the current layout above, " + texto;
        } else {
            if (history.isEmpty()) {
                prompt += "\n\nUser Request:\n" + texto;
            } else {
                prompt += "User Request:\n" + texto;
            }
        }
        
        String initialLayout = GroqClient.getInstance().sendMessage(prompt);
        
        // Limpar o layout gerado (remover markdown code blocks se houver)
        initialLayout = cleanXmlLayout(initialLayout);
        
        // Passo 2: Morph aplica refinamentos e otimizações
        try {
            String instructions = "Refine and optimize this Android XML layout for Sketchware. Ensure all attributes are valid, the structure follows best practices, proper indentation, and the layout follows Material Design guidelines. Keep the same functionality but improve code quality and formatting.";
            
            // Para o Morph, passamos o layout inicial como código base
            // O codeEdit é o mesmo código, mas o Morph vai refiná-lo baseado nas instruções
            // O formato espera que codeEdit mostre a versão editada, então passamos o mesmo código
            // e o Morph vai criar uma versão refinada
            String codeEdit = initialLayout;
            
            String refinedLayout = MorphClient.getInstance().applyCodeEdit(
                initialLayout,
                codeEdit,
                instructions
            );
            
            // Limpar o resultado final
            return cleanXmlLayout(refinedLayout);
        } catch (IOException e) {
            // Se Morph falhar, retornar o layout do Groq
            return initialLayout;
        }
    }
    
    /**
     * Remove markdown code blocks e limpa o XML gerado
     */
    private String cleanXmlLayout(String layout) {
        if (layout == null) return "";
        
        // Remover blocos de código markdown
        layout = layout.replaceAll("```xml\\s*", "");
        layout = layout.replaceAll("```\\s*", "");
        layout = layout.trim();
        
        // Se ainda contém markdown, tentar extrair apenas o XML
        int xmlStart = layout.indexOf("<");
        int xmlEnd = layout.lastIndexOf(">");
        
        if (xmlStart != -1 && xmlEnd != -1 && xmlEnd > xmlStart) {
            layout = layout.substring(xmlStart, xmlEnd + 1);
        }
        
        return layout.trim();
    }
}
