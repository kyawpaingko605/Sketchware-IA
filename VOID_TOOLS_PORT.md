# Void Tools Port - Documentação Completa

## Visão Geral

Este documento descreve a portabilidade completa das ferramentas do **Void Editor** para o **Sketchware-IA**, permitindo que o chat use todas as ferramentas builtin do Void para manipulação de arquivos, busca, edição e execução de comandos.

## Arquitetura

### Estrutura de Pastas

```
Sketchware-IA-main/
├── app/src/main/java/pro/sketchware/
│   ├── activities/chat/
│   │   └── port/
│   │       ├── VoidPortToolsService.java    ← Port principal das ferramentas
│   │       └── VoidToolWrapper.java         ← Wrapper para ferramentas Void
│   ├── ia/tools/
│   │   ├── ToolManager.java                 ← Gerenciador de ferramentas
│   │   └── Tool.java                        ← Interface de ferramentas
│   └── util/
│       ├── SemanticFileSearcher.java        ← Busca semântica (atualizado)
│       ├── SketchwareFileEncryptor.java     ← Criptografia (atualizado)
│       └── ProjectPathResolver.java         ← Resolução de paths
└── VOID_TOOLS_PORT.md                       ← Esta documentação
```

## Migração das Ferramentas Legadas

As seguintes ferramentas legadas foram **removidas** e substituídas pelas equivalentes do Void:

| Ferramenta Legada | Substituta Void | Status |
|-------------------|-----------------|--------|
| `ListProjectEntriesTool` | `ls_dir`, `get_dir_tree` | ✅ Migrado |
| `ReadProjectFileTool` | `read_file` | ✅ Migrado |
| `SearchProjectContentTool` | `search_for_files`, `search_pathnames_only` | ✅ Migrado |
| `ListVoidSourceAssetsTool` | (não necessário) | ❌ Removido |
| `ReadVoidSourceAssetTool` | (não necessário) | ❌ Removido |
| `RewriteProjectFileTool` | `rewrite_file` | ✅ Migrado |
| `EditProjectFileTool` | `edit_file` | ✅ Migrado |
| `ListProjectFilesTool` | `ls_dir` | ✅ Migrado |
| `DecryptTool` | (integrado no `read_file`) | ✅ Integrado |
| `EncryptTool` | (integrado no `rewrite_file`, `edit_file`) | ✅ Integrado |
| `ShellTool` | `run_command` | ✅ Migrado |

### Arquivos Removidos

Os seguintes arquivos foram removidos da pasta `ia/tools/`:
- `ListProjectEntriesTool.java`
- `ReadProjectFileTool.java`
- `SearchProjectContentTool.java`
- `ListVoidSourceAssetsTool.java`
- `ReadVoidSourceAssetTool.java`
- `RewriteProjectFileTool.java`
- `EditProjectFileTool.java`
- `ListProjectFilesTool.java`
- `DecryptTool.java`
- `EncryptTool.java`
- `ShellTool.java`
- `VoidToolWrapper.java` (movido para `activities/chat/port/`)

### Estrutura Atual

```
ia/tools/
├── Tool.java                        ← Interface base
└── ToolManager.java                 ← Gerenciador (usa apenas Void tools)

activities/chat/port/
├── VoidPortToolsService.java        ← Implementação de todas as ferramentas
└── VoidToolWrapper.java             ← Adapta Void tools para interface Tool
```

## Ferramentas Portadas

Todas as ferramentas builtin do Void foram portadas para o Sketchware-IA:

### 1. Ferramentas de Arquivo

#### `read_file`
- **Descrição**: Lê o conteúdo de um arquivo com suporte a paginação e seleção de linhas
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo
  - `start_line` (number, opcional): Linha inicial
  - `end_line` (number, opcional): Linha final
  - `page_number` (number): Número da página para paginação
- **Retorno**: Conteúdo do arquivo, tamanho total, número de linhas, indicador de próxima página

#### `ls_dir`
- **Descrição**: Lista arquivos e pastas em um diretório com paginação
- **Parâmetros**:
  - `uri` (string): Caminho do diretório
  - `page_number` (number): Número da página
- **Retorno**: Lista de itens, indicadores de paginação

#### `get_dir_tree`
- **Descrição**: Retorna uma árvore de diretórios em formato de string
- **Parâmetros**:
  - `uri` (string): Caminho do diretório
- **Retorno**: String formatada com a árvore de diretórios

### 2. Ferramentas de Busca

#### `search_pathnames_only`
- **Descrição**: Busca arquivos por nome (somente pathnames)
- **Parâmetros**:
  - `query` (string): Termo de busca
  - `include_pattern` (string, opcional): Padrão de inclusão
  - `page_number` (number): Número da página
- **Retorno**: Lista de URIs encontradas

#### `search_for_files`
- **Descrição**: Busca arquivos por conteúdo com suporte a regex
- **Parâmetros**:
  - `query` (string): Termo ou regex de busca
  - `is_regex` (boolean): Se true, trata query como regex
  - `search_in_folder` (string, opcional): Pasta para limitar busca
  - `page_number` (number): Número da página
- **Retorno**: Lista de URIs encontradas

#### `search_in_file`
- **Descrição**: Busca por string ou regex dentro de um arquivo específico
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo
  - `query` (string): Termo ou regex de busca
  - `is_regex` (boolean): Se true, trata query como regex
- **Retorno**: Lista de números de linha onde ocorre a busca

#### `read_lint_errors`
- **Descrição**: Lê erros de lint/diagnóstico de um arquivo
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo
- **Retorno**: Lista de erros com código, mensagem e linhas

### 3. Ferramentas de Edição

#### `rewrite_file`
- **Descrição**: Reescreve completamente o conteúdo de um arquivo
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo
  - `new_content` (string): Novo conteúdo completo
- **Requer aprovação**: Sim
- **Retorno**: Erros de lint após escrita

#### `edit_file`
- **Descrição**: Aplica edições usando blocos SEARCH/REPLACE
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo
  - `search_replace_blocks` (string): Blocos no formato:
    ```
    <<<<<<< ORIGINAL
    // código original
    =======
    // código novo
    >>>>>>> UPDATED
    ```
- **Requer aprovação**: Sim
- **Retorno**: Erros de lint após edição

#### `create_file_or_folder`
- **Descrição**: Cria um arquivo ou pasta
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo/pasta (terminar com `/` ou `\` para pasta)
- **Requer aprovação**: Sim
- **Retorno**: Objeto vazio `{}`

#### `delete_file_or_folder`
- **Descrição**: Deleta um arquivo ou pasta
- **Parâmetros**:
  - `uri` (string): Caminho do arquivo/pasta
  - `is_recursive` (boolean): Se true, deleta recursivamente
- **Requer aprovação**: Sim
- **Retorno**: Objeto vazio `{}`

### 4. Ferramentas de Terminal

#### `run_command`
- **Descrição**: Executa um comando shell e retorna o resultado
- **Parâmetros**:
  - `command` (string): Comando a executar
  - `cwd` (string, opcional): Diretório de trabalho
- **Requer aprovação**: Sim
- **Retorno**: Resultado da execução, código de saída, motivo de resolução

#### `open_persistent_terminal`
- **Descrição**: Abre um terminal persistente em background
- **Parâmetros**:
  - `cwd` (string, opcional): Diretório de trabalho
- **Requer aprovação**: Sim
- **Retorno**: ID do terminal persistente

#### `run_persistent_command`
- **Descrição**: Executa um comando em um terminal persistente
- **Parâmetros**:
  - `command` (string): Comando a executar
  - `persistent_terminal_id` (string): ID do terminal
- **Requer aprovação**: Sim
- **Retorno**: Resultado da execução

#### `kill_persistent_terminal`
- **Descrição**: Fecha um terminal persistente
- **Parâmetros**:
  - `persistent_terminal_id` (string): ID do terminal
- **Requer aprovação**: Sim
- **Retorno**: Objeto vazio `{}`

## Integração com ToolManager

O `ToolManager` foi atualizado para registrar automaticamente todas as ferramentas do Void através do `VoidToolWrapper`:

```java
public ToolManager() {
    // ... ferramentas existentes ...
    
    // Register all Void builtin tools
    VoidToolWrapper.registerAllVoidTools(this);
}
```

## Segurança

### Aprovação de Ferramentas

As ferramentas são classificadas por tipo de aprovação:

- **Edits** (requerem aprovação): `rewrite_file`, `edit_file`, `create_file_or_folder`, `delete_file_or_folder`
- **Terminal** (requerem aprovação): `run_command`, `open_persistent_terminal`, `run_persistent_command`, `kill_persistent_terminal`
- **Leitura** (não requerem aprovação): `read_file`, `ls_dir`, `get_dir_tree`, `search_*`, `read_lint_errors`

### Proteção de Arquivos Sketchware

As ferramentas de terminal bloqueiam comandos perigosos em arquivos do Sketchware:
- `cat`, `echo`, `sed`, `grep`, `rm`, `mv`, `cp`, `chmod`, `chown`, `dd`
- Redirecionamentos `>` e `>>`

Para manipular arquivos criptografados do Sketchware, use:
- `decrypt_sketchware_file` para ler
- `encrypt_sketchware_file` para salvar

## Dependências Atualizadas

### SemanticFileSearcher
Adicionados métodos para busca por nome e conteúdo:
- `searchByFilename(String query, String scId)`
- `searchByContent(String query, String scId)`
- `searchByContentRegex(String regex, String scId)`

### SketchwareFileEncryptor
Adicionado método para verificação de arquivos Sketchware:
- `isSketchwareFile(String scId, String filePath)`

### VoidPortMarkerCheckService
Adicionada classe interna e método para erros de lint:
- `class LintError` (code, message, startLineNumber, endLineNumber)
- `getLintErrors(String scId, String filePath)`

## Uso no Chat

As ferramentas estão disponíveis em todos os modos de chat exceto "normal":

- **Modo normal**: Nenhuma ferramenta disponível
- **Modo assistente**: Ferramentas de leitura e busca disponíveis
- **Modo agent**: Todas as ferramentas disponíveis (incluindo edição e terminal)

Para obter as ferramentas em formato MCP:

```java
ToolManager manager = new ToolManager();
JSONArray toolsMCP = manager.getToolsAsMCP("agent");
```

## Exemplo de Uso

```java
// Executar uma ferramenta
ToolManager manager = new ToolManager();
String scId = "meu_projeto";

// Ler um arquivo
String result = manager.executeTool(scId, "read_file", 
    "{\"uri\": \"logic/main.java\", \"page_number\": 1}");

// Editar um arquivo
String editResult = manager.executeTool(scId, "edit_file",
    "{\"uri\": \"logic/main.java\", " +
    "\"search_replace_blocks\": \"<<<<<<< ORIGINAL\\nold code\\n=======\\nnew code\\n>>>>>>> UPDATED\"}");

// Buscar arquivos
String searchResult = manager.executeTool(scId, "search_for_files",
    "{\"query\": \"public class\", \"is_regex\": false, \"page_number\": 1}");
```

## Comparação com Void Original

| Recurso | Void (TypeScript) | Sketchware-IA (Java) |
|---------|-------------------|----------------------|
| Leitura de arquivos | ✅ | ✅ |
| Listagem de diretórios | ✅ | ✅ |
| Árvore de diretórios | ✅ | ✅ |
| Busca por nome | ✅ | ✅ |
| Busca por conteúdo | ✅ | ✅ |
| Busca com regex | ✅ | ✅ |
| Busca em arquivo | ✅ | ✅ |
| Erros de lint | ✅ | ✅ (via CompileErrorCapture) |
| Reescrever arquivo | ✅ | ✅ |
| Editar com SEARCH/REPLACE | ✅ | ✅ |
| Criar arquivo/pasta | ✅ | ✅ |
| Deletar arquivo/pasta | ✅ | ✅ |
| Executar comando | ✅ | ✅ |
| Terminal persistente | ✅ | ✅ |
| Criptografia Sketchware | N/A | ✅ (específico do Sketchware) |

## Considerações de Implementação

### Paginação
- `MAX_FILE_CHARS_PAGE = 24000` caracteres por página
- `MAX_CHILDREN_URIS_PAGE = 50` itens por página de diretório

### Timeouts de Terminal
- Comando normal: 15 segundos
- Comando persistente: 30 segundos
- Terminal inativo: 60 segundos

### Limites de Segurança
- Arquivos Sketchware criptografados não podem ser modificados via shell
- Comandos perigosos são bloqueados automaticamente
- Aprovação requerida para operações destrutivas

## Futuras Melhorias

1. **Integração com Lint do Android**: Melhorar `read_lint_errors` para usar o lint oficial do Android
2. **Busca Semântica Avançada**: Implementar embeddings para busca mais inteligente
3. **Histórico de Terminal**: Manter histórico de comandos executados
4. **Cancelamento de Operações**: Melhor suporte para cancelamento de operações longas

## Referências

- Void Editor: https://github.com/voideditor/void
- Sketchware-IA: Projeto base
- Arquivo original: `void/browser/toolsService.ts`
- Tipos originais: `void/common/toolsServiceTypes.ts`
