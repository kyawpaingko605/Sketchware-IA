# Void Tools Port - Pasta `port`

## Visão Geral

Esta pasta contém todas as ferramentas do **Void Editor** portadas para o Sketchware-IA. As ferramentas estão localizadas aqui para manter a organização e separação clara entre:

- **`port/`**: Código portado do Void (TypeScript → Java)
- **`ia/tools/`**: Interface e gerenciador de ferramentas do Sketchware-IA

## Arquivos Principais

### `VoidPortToolsService.java`
Implementação principal de **todas** as ferramentas builtin do Void:
- Leitura de arquivos (`read_file`)
- Listagem de diretórios (`ls_dir`, `get_dir_tree`)
- Busca (`search_pathnames_only`, `search_for_files`, `search_in_file`)
- Edição (`rewrite_file`, `edit_file`, `create_file_or_folder`, `delete_file_or_folder`)
- Terminal (`run_command`, `open_persistent_terminal`, `run_persistent_command`, `kill_persistent_terminal`)
- Diagnóstico (`read_lint_errors`)

### `VoidToolWrapper.java`
Wrapper que adapta as ferramentas do `VoidPortToolsService` para a interface `Tool` do Sketchware-IA.

**Responsabilidades:**
- Implementar a interface `Tool` para cada ferramenta Void
- Registrar todas as ferramentas no `ToolManager`
- Definir quais ferramentas requerem aprovação
- Definir quais ferramentas são destrutivas

## Como Usar

### No ToolManager

```java
import pro.sketchware.ia.tools.ToolManager;

ToolManager manager = new ToolManager();
// Todas as ferramentas Void já estão registradas automaticamente
```

### Executar uma Ferramenta

```java
// Ler arquivo
String result = manager.executeTool(
    scId, 
    "read_file", 
    "{\"uri\": \"logic/main.java\", \"page_number\": 1}"
);

// Editar arquivo
String editResult = manager.executeTool(
    scId, 
    "edit_file",
    "{\"uri\": \"logic/main.java\", \"search_replace_blocks\": \"...\"}"
);

// Buscar arquivos
String searchResult = manager.executeTool(
    scId,
    "search_for_files",
    "{\"query\": \"public class\", \"is_regex\": false}"
);
```

### Obter Ferramentas em Formato MCP

```java
JSONArray toolsMCP = manager.getToolsAsMCP("agent");
```

## Lista Completa de Ferramentas

### Leitura (sem aprovação)
| Ferramenta | Descrição |
|------------|-----------|
| `read_file` | Lê conteúdo de arquivo com paginação |
| `ls_dir` | Lista arquivos em diretório |
| `get_dir_tree` | Retorna árvore de diretórios |
| `search_pathnames_only` | Busca por nome de arquivo |
| `search_for_files` | Busca por conteúdo |
| `search_in_file` | Busca dentro de arquivo específico |
| `read_lint_errors` | Lê erros de lint |

### Edição (requer aprovação)
| Ferramenta | Descrição | Destrutiva |
|------------|-----------|------------|
| `rewrite_file` | Reescreve arquivo completo | ✅ |
| `edit_file` | Aplica edições SEARCH/REPLACE | ✅ |
| `create_file_or_folder` | Cria arquivo ou pasta | ❌ |
| `delete_file_or_folder` | Deleta arquivo ou pasta | ✅ |

### Terminal (requer aprovação)
| Ferramenta | Descrição |
|------------|-----------|
| `run_command` | Executa comando shell |
| `open_persistent_terminal` | Abre terminal persistente |
| `run_persistent_command` | Executa em terminal persistente |
| `kill_persistent_terminal` | Fecha terminal persistente |

## Dependências

As ferramentas dependem dos seguintes utilitários:

- `SketchwareFileDecryptor` - Descriptografar arquivos Sketchware
- `SketchwareFileEncryptor` - Criptografar arquivos Sketchware
- `ProjectPathResolver` - Resolver paths de arquivos
- `SemanticFileSearcher` - Busca semântica em arquivos
- `DirectoryTreeService` - Gerar árvore de diretórios
- `VoidPortMarkerCheckService` - Obter erros de lint
- `CompileErrorCapture` - Capturar erros de compilação

## Segurança

### Aprovação de Ferramentas

As ferramentas são classificadas por modo de chat:

- **Modo normal**: Nenhuma ferramenta
- **Modo assistente**: Apenas leitura
- **Modo agent**: Todas as ferramentas (incluindo edição e terminal)

### Proteção de Arquivos

- Comandos shell perigosos são bloqueados automaticamente
- Arquivos criptografados do Sketchware só podem ser lidos/editados via ferramentas apropriadas
- Operações destrutivas requerem aprovação explícita

## Migração

Se você estava usando as ferramentas legadas, atualize para as equivalentes Void:

```
ListProjectEntriesTool  →  ls_dir, get_dir_tree
ReadProjectFileTool     →  read_file
SearchProjectContentTool →  search_for_files, search_pathnames_only
RewriteProjectFileTool  →  rewrite_file
EditProjectFileTool     →  edit_file
ListProjectFilesTool    →  ls_dir
DecryptTool             →  (integrado no read_file)
EncryptTool             →  (integrado no rewrite_file, edit_file)
ShellTool               →  run_command
```

## Referências

- Void original: `void/browser/toolsService.ts`
- Tipos: `void/common/toolsServiceTypes.ts`
- Documentação completa: `VOID_TOOLS_PORT.md` (raiz do projeto)
