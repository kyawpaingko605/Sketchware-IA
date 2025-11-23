# Análise Profunda: Como o Sketchware Lê Layouts e Projetos

## 📋 Visão Geral

Este documento explica como o Sketchware lê e gerencia layouts e projetos, baseado na análise do código-fonte.

## 🏗️ Arquitetura Principal

### 1. Sistema de Gerenciamento de Projetos (`jC`)

A classe `jC` é o **gerenciador central** que coordena todos os componentes do projeto:

```12:128:app/src/main/java/a/a/a/jC.java
public class jC {

    public static eC a;
    public static hC b;
    public static kC c;
    public static iC d;
    
    // ... métodos de gerenciamento ...
    
    public static synchronized eC a(String str) {
        return a(str, true);
    }

    public static synchronized eC a(String str, boolean z) {
        if (a != null && !str.equals(a.a)) {
            b();
        }
        if (a == null) {
            a = new eC(str);
            if (!z) {
                a.g();
                a.e();
            } else {
                if (a.d()) {
                    a.h();
                } else {
                    a.g();
                }
                if (a.c()) {
                    a.f();
                } else {
                    a.e();
                }
            }
        }
        return a;
    }
}
```

**Componentes principais:**
- **`eC` (a)**: Gerencia os dados de layout (ViewBeans) e lógica do projeto
- **`hC` (b)**: Gerencia os arquivos do projeto (ProjectFileBean)
- **`kC` (c)**: Gerencia outros aspectos do projeto
- **`iC` (d)**: Componente adicional de gerenciamento

### 2. Carregamento de Projetos

O método `loadProject()` em `DesignActivity` mostra como um projeto é carregado:

```229:242:app/src/main/java/com/besome/sketch/design/DesignActivity.java
private void loadProject(boolean haveSavedState) {
    projectFile = getDefaultProjectFile();
    jC.a(sc_id, haveSavedState);
    jC.b(sc_id, haveSavedState);
    kC var2 = jC.d(sc_id, haveSavedState);
    jC.c(sc_id, haveSavedState);
    cC.c(sc_id);
    bC.d(sc_id);
    if (!haveSavedState) {
        var2.f();
        var2.g();
        var2.e();
    }
}
```

**Fluxo de carregamento:**
1. Obtém o arquivo padrão do projeto (`getDefaultProjectFile()`)
2. Inicializa o gerenciador de dados (`jC.a()` - eC)
3. Inicializa o gerenciador de arquivos (`jC.b()` - hC)
4. Inicializa outros gerenciadores necessários
5. Se não houver estado salvo, executa inicializações adicionais

## 📄 Como os Layouts São Lidos

### 1. Estrutura de Dados: ViewBean

Os layouts são representados internamente como `ViewBean`, que contém:
- Informações de layout (dimensões, margens, padding)
- Propriedades de texto (se aplicável)
- Propriedades de imagem (se aplicável)
- Relacionamento hierárquico (parent/child)
- Atributos customizados

### 2. Conversão: Layout Interno → XML

A classe `Ox` é responsável por converter os dados internos (ViewBeans) para XML:

```42:59:app/src/main/java/a/a/a/Ox.java
public class Ox {

    private final jq buildConfig;
    private final InjectRootLayoutManager rootManager;
    private final AppCompatInjection aci;
    private final ProjectFileBean projectFile;
    private ViewBean fab;
    private ArrayList<ViewBean> views;
    private XmlBuilder rootLayout = null;
    private XmlBuilder collapsingToolbarLayout = null;
    private boolean excludeAppCompat;

    public Ox(jq jq, ProjectFileBean projectFileBean) {
        buildConfig = jq;
        projectFile = projectFileBean;
        rootManager = new InjectRootLayoutManager(jq.sc_id);
        aci = new AppCompatInjection(jq, projectFileBean);
    }
```

**Processo de conversão:**

1. **Inicialização**: Cria uma instância `Ox` com o arquivo do projeto
2. **Carregamento de dados**: Recebe lista de ViewBeans através do método `a()`:

```274:282:app/src/main/java/a/a/a/Ox.java
public void a(ArrayList<ViewBean> arrayList) {
    a(arrayList, null);
}

public void a(ArrayList<ViewBean> arrayList, ViewBean viewBean) {
    fab = viewBean;
    views = arrayList;
    writeRootLayout();
}
```

3. **Geração de XML**: O método `b()` retorna o XML gerado:

```284:286:app/src/main/java/a/a/a/Ox.java
public String b() {
    return rootLayout.toCode();
}
```

### 3. Exemplo de Uso no Código

No `DesignActivity`, vemos como o layout atual é obtido:

```712:722:app/src/main/java/com/besome/sketch/design/DesignActivity.java
// Se solicitado, obter o layout atual
if (includeCurrentLayout) {
    try {
        // Usar q.N que é o jq necessário para Ox (mesmo padrão usado na linha 1021)
        Ox ox = new Ox(q.N, projectFile);
        ox.a(jC.a(sc_id).d(xmlName), jC.a(sc_id).h(xmlName));
        currentLayoutXml = ox.b();
    } catch (Exception e) {
        Log.e("DesignActivity", "Erro ao obter layout atual", e);
        // Continua sem o layout atual se houver erro
    }
}
```

**Explicação:**
- `jC.a(sc_id).d(xmlName)`: Obtém a lista de ViewBeans do layout
- `jC.a(sc_id).h(xmlName)`: Obtém o FAB (FloatingActionButton) se existir
- `ox.b()`: Converte para XML

## 🔄 Conversão Reversa: XML → Layout Interno

### ViewBeanParser

A classe `ViewBeanParser` faz o processo inverso: converte XML para ViewBeans:

```36:56:app/src/main/java/pro/sketchware/tools/ViewBeanParser.java
public class ViewBeanParser {

    private static final int[] viewsCount = new int[49];
    private final XmlPullParser parser;
    private boolean skipRoot;
    private Pair<String, Map<String, String>> rootAttributes;

    public ViewBeanParser(String xml) throws XmlPullParserException {
        this(new StringReader(xml));
    }

    public ViewBeanParser(File path) throws XmlPullParserException, FileNotFoundException {
        this(new FileReader(path));
    }

    public ViewBeanParser(Reader reader) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        parser = factory.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(reader);
    }
```

**Uso no DesignActivity:**

```760:777:app/src/main/java/com/besome/sketch/design/DesignActivity.java
var parser = new ViewBeanParser(preparedXml);
parser.setSkipRoot(true);
var parsedLayout = parser.parse();
var root = parser.getRootAttributes();

var rootLayoutManager = new InjectRootLayoutManager(sc_id);
rootLayoutManager.set(xmlName, InjectRootLayoutManager.toRoot(root));

var bean = new HistoryViewBean();
bean.actionOverride(parsedLayout, jC.a(sc_id).d(xmlName));
var cc = cC.c(sc_id);
if (!cc.c.containsKey(xmlName)) {
    cc.e(xmlName);
}
cc.a(xmlName);
cc.a(xmlName, bean);

jC.a(sc_id).c.put(xmlName, parsedLayout);
```

## 💾 Armazenamento de Dados

### Estrutura de Diretórios

Os projetos são armazenados em:
```
/.sketchware/data/<sc_id>/
```

Onde `sc_id` é o ID único do projeto.

### Arquivos Principais

1. **Dados de Layout**: Armazenados via `eC` (gerenciador de dados)
   - Método `d(xmlName)`: Retorna lista de ViewBeans
   - Método `h(xmlName)`: Retorna FAB se existir

2. **Arquivos do Projeto**: Gerenciados via `hC`
   - Método `b(xmlName)`: Retorna ProjectFileBean pelo nome XML
   - Método `a(javaName)`: Retorna ProjectFileBean pelo nome Java

3. **Root Layout**: Gerenciado por `InjectRootLayoutManager`
   - Arquivo: `/.sketchware/data/<sc_id>/view_root`
   - Formato: JSON com informações do layout raiz

```18:48:app/src/main/java/pro/sketchware/managers/inject/InjectRootLayoutManager.java
public class InjectRootLayoutManager {
    private final String path;

    public InjectRootLayoutManager(String sc_id) {
        path = wq.b(sc_id) + "/view_root";
    }

    public static Root getDefaultRootLayout() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("android:layout_width", "match_parent");
        attrs.put("android:layout_height", "match_parent");
        attrs.put("android:orientation", "vertical");
        return new Root("LinearLayout", attrs);
    }

    public static Root toRoot(Pair<String, Map<String, String>> root) {
        return new Root(root.first, root.second);
    }

    public void set(String name, Root layout) {
        Map<String, Root> data = get();
        if (data == null) {
            data = new LinkedHashMap<>();
        }
        data.put(name, layout);
        save(data);
    }

    private void save(Map<String, Root> data) {
        FileUtil.writeFile(path, new Gson().toJson(data));
    }
```

## 🔑 Pontos-Chave para Edição de Layouts

### 1. Obter Layout Atual

```java
// Obter gerenciador de dados
eC dataManager = jC.a(sc_id);

// Obter lista de ViewBeans do layout
ArrayList<ViewBean> views = dataManager.d(xmlName);

// Obter FAB se existir
ViewBean fab = dataManager.h(xmlName);

// Converter para XML
Ox ox = new Ox(buildConfig, projectFile);
ox.a(views, fab);
String xml = ox.b();
```

### 2. Aplicar Novo Layout

```java
// Parsear XML para ViewBeans
ViewBeanParser parser = new ViewBeanParser(xml);
parser.setSkipRoot(true);
ArrayList<ViewBean> newViews = parser.parse();

// Salvar no gerenciador
jC.a(sc_id).c.put(xmlName, newViews);

// Atualizar root layout se necessário
InjectRootLayoutManager rootManager = new InjectRootLayoutManager(sc_id);
rootManager.set(xmlName, InjectRootLayoutManager.toRoot(parser.getRootAttributes()));
```

### 3. Gerenciar Arquivos do Projeto

```java
// Obter gerenciador de arquivos
hC fileManager = jC.b(sc_id);

// Obter arquivo por nome XML
ProjectFileBean file = fileManager.b("main.xml");

// Obter lista de todos os layouts
ArrayList<ProjectFileBean> layouts = fileManager.b();
```

## 📱 Telas Existentes para Edição

Com base no código analisado, existem várias telas que lidam com edições:

1. **DesignActivity**: Tela principal de design
   - Gerencia visualização e edição de layouts
   - Usa `ViewEditorFragment` para edição visual
   - Integra com sistema de IA para geração de layouts

2. **ViewCodeEditorActivity**: Editor de código XML
   - Permite edição direta do XML
   - Converte XML para ViewBeans automaticamente

3. **ViewEditorFragment**: Fragmento de edição visual
   - Interface visual para editar layouts
   - Gerencia drag-and-drop de componentes

## 🎯 Conclusão

O Sketchware usa uma arquitetura em camadas:

1. **Camada de Armazenamento**: Dados em formato JSON/objetos Java
2. **Camada de Conversão**: `Ox` (para XML) e `ViewBeanParser` (de XML)
3. **Camada de Gerenciamento**: `jC`, `eC`, `hC` para coordenar tudo
4. **Camada de Interface**: `DesignActivity`, `ViewEditorFragment`, etc.

Para trabalhar com layouts, você precisa:
- Usar `jC.a(sc_id).d(xmlName)` para ler layouts
- Usar `Ox` para converter para XML
- Usar `ViewBeanParser` para converter de XML
- Usar `jC.a(sc_id).c.put(xmlName, views)` para salvar alterações

