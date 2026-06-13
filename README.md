<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="140" alt="Sketchware IA logo">
</p>

<h1 align="center">Sketchware IA</h1>

<p align="center">
  Crie aplicativos Android completos no seu celular — com blocos visuais, código real e inteligência artificial integrada.
</p>

<p align="center">
  <a href="https://github.com/FabioSilva11/Sketchware-IA/actions/workflows/android.yml"><img src="https://img.shields.io/github/actions/workflow/status/FabioSilva11/Sketchware-IA/android.yml?branch=main&label=Android%20CI" alt="Android CI"></a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/commits/main"><img src="https://img.shields.io/github/last-commit/FabioSilva11/Sketchware-IA?label=último%20commit" alt="Last commit"></a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/pulls"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs welcome"></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/license-source--available-lightgrey" alt="Source-available license"></a>
</p>

---

## O que é o Sketchware IA?

O **Sketchware IA** é um fork do Sketchware Pro com inteligência artificial nativa integrada. Ele permite que qualquer pessoa — com ou sem experiência em programação — crie aplicativos Android completos diretamente do celular, sem precisar de um computador.

A proposta central é simples: **democratizar o desenvolvimento mobile**. Quem não tem acesso a um PC ainda pode criar, aprender e publicar apps reais.

---

## Formas de criar um aplicativo

O Sketchware IA oferece **três abordagens** de desenvolvimento, que podem ser usadas juntas ou separadamente:

### 🧩 Blocos arrastáveis (sem código)

A forma mais acessível. A lógica do aplicativo é montada arrastando e conectando blocos visuais, semelhante ao Scratch. Nenhuma linha de código é necessária.

- Ideal para iniciantes e para ensino de programação
- Blocos representam ações reais: condicionais, loops, chamadas de API, navegação entre telas, banco de dados, e muito mais
- O código Java é gerado automaticamente em segundo plano
- Você vê o resultado final sem precisar entender a implementação

### ☕ Java nativo

Para quem quer controle total. É possível criar um projeto inteiramente em Java, sem blocos, usando o editor de código integrado ao app.

- Suporte a Java com syntax highlight e autocompletar
- Acesso direto ao código gerado pelos blocos
- Possibilidade de adicionar código customizado em qualquer evento ou componente
- Integração com bibliotecas `.jar` e `.aar` locais

### 🤝 Blocos + código customizado (modo híbrido)

A abordagem mais flexível. Você usa blocos para a estrutura geral e insere blocos de código Java nos pontos que precisam de lógica avançada.

- Misture blocos visuais com snippets de código em qualquer evento
- Útil para quem está aprendendo: começa com blocos e vai migrando para código gradualmente
- Blocos de código customizado suportam chamadas a bibliotecas externas, reflection e qualquer API Android

### 🔵 Kotlin (em desenvolvimento)

Suporte a Kotlin como linguagem alternativa para projetos em código nativo.

---

## Inteligência Artificial integrada

O Sketchware IA vai além do editor visual. Ele tem um **agente de IA nativo** que funciona como um assistente de desenvolvimento completo, diretamente no chat do app.

### Agente de desenvolvimento

O agente opera em modo autônomo e tem acesso a ferramentas reais — não é apenas um chatbot que responde perguntas. Ele pode:

- **Editar arquivos** do projeto diretamente (`edit_file`) com suporte a blocos de busca e substituição
- **Executar comandos no terminal** integrado (`run_command`, `run_persistent_command`) com timeout de 30 segundos
- **Navegar pelo sistema de arquivos** (`list_files`, `read_file`) para entender a estrutura do projeto
- **Corrigir erros de compilação automaticamente** sem intervenção manual
- **Integrar com servidores MCP** (Model Context Protocol) via HTTP para ampliar as capacidades com ferramentas externas

### AI Fix — correção automática de erros

Quando um projeto falha na compilação, o **AI Fix** analisa o log de erro, identifica o bloco ou código responsável e aplica a correção automaticamente.

- Detecta a causa raiz além do primeiro erro reportado pelo compilador
- Sugere operações precisas: atualizar parâmetro de bloco, adicionar variável, corrigir tipo
- Exibe um resumo explicativo da correção antes de aplicá-la
- Operações seguras são aplicadas com um toque; as complexas ficam como sugestão manual

### Modelos de IA suportados

O Sketchware IA se conecta a provedores via **OpenRouter**, incluindo modelos gratuitos:

| Modelo | Contexto | Tool calls | Gratuito |
|--------|----------|------------|----------|
| `google/gemini-2.0-flash-exp:free` | 1M tokens | ✅ Nativas | ✅ |
| `anthropic/claude-sonnet-4` | 200K tokens | ✅ Nativas | ❌ |
| `anthropic/claude-opus-4` | 200K tokens | ✅ Nativas | ❌ |
| `deepseek/deepseek-r1` | 128K tokens | ✅ | ❌ |
| `qwen/qwen3-235b-a22b` | 40K tokens | ✅ | ❌ |

> **Recomendação:** Para uso gratuito, use `google/gemini-2.0-flash-exp:free`. É o único modelo gratuito disponível que gera tool calls limpas e não apresenta vazamento de tokens internos.

### GitHub MCP integrado

O agente tem acesso nativo à API do GitHub com **13 ferramentas** disponíveis no chat:

| Ferramenta | O que faz |
|------------|-----------|
| `github_list_repos` | Lista repositórios |
| `github_get_file` | Lê conteúdo de arquivo |
| `github_list_files` | Navega por diretório |
| `github_search_code` | Pesquisa código |
| `github_create_issue` | Cria issues |
| `github_create_pull_request` | Abre PRs |
| `github_create_or_update_file` | Faz commit de arquivo |
| `github_list_commits` | Histórico de commits |
| + 5 outras | — |

Configure em **Settings → GitHub Settings** com um Personal Access Token.

---

## Integração com GitHub — versionamento de projetos

> 🔜 **Recurso planejado**

Uma das próximas grandes funcionalidades é a **integração direta do GitHub com os projetos do Sketchware IA**, permitindo que os usuários façam versionamento completo dos seus apps sem sair do celular.

O objetivo é que cada projeto Sketchware tenha:

- **Repositório GitHub vinculado** — criação automática ou vinculação a um repositório existente
- **Commits a cada salvamento** — histórico completo de todas as versões do projeto
- **Branches por funcionalidade** — crie uma branch para testar algo sem quebrar o projeto principal
- **Diff visual** — compare versões anteriores dos blocos e do código
- **Restauração de versão** — volte para qualquer estado anterior com um toque
- **Colaboração** — compartilhe o repositório com outras pessoas para contribuírem no projeto
- **Backup automático na nuvem** — seus projetos nunca mais serão perdidos por formatação ou troca de celular

Se você quer ajudar a construir essa funcionalidade, veja a seção [Contribuindo](#contribuindo).

---

## Visão de futuro — Migração para Flutter

> 🔮 **Roadmap de longo prazo**

A intenção é **migrar o Sketchware IA para Flutter**, tanto na interface quanto na arquitetura interna. Essa mudança trará benefícios significativos:

### Por que Flutter?

- **Interface consistente** em todas as versões do Android, sem depender de APIs depreciadas
- **Performance superior** com rendering próprio (Skia/Impeller)
- **Codebase único** para múltiplas plataformas
- **Ecossistema rico** de widgets e bibliotecas

### O que muda para os usuários

Com a migração para Flutter e Dart como linguagem base, o Sketchware IA ganhará a capacidade de **compilar projetos para múltiplas plataformas** a partir do mesmo código:

| Linguagem | Compila para |
|-----------|-------------|
| **Dart / Flutter** | Android, iOS, Web, Windows, Linux, macOS |

Isso significa que um app criado no Sketchware IA poderá rodar em qualquer dispositivo e sistema operacional, mantendo a mesma experiência de desenvolvimento visual com blocos arrastáveis.

> Esta é uma visão de longo prazo. A versão atual continua sendo desenvolvida ativamente em Java/Kotlin para Android.

---

## Contribuindo

O Sketchware IA é um projeto open source e **contribuições são muito bem-vindas**.

### O que você pode contribuir

- **Novos blocos** para o editor visual
- **Melhorias no agente de IA** — prompts, ferramentas MCP, modelos
- **Novas ferramentas para o agente** — o sistema de tools é extensível, e você pode usar a própria IA para desenvolver novos módulos
- **Interface** — o design atual é funcional, mas pode evoluir muito
- **Testes automatizados** — o projeto ainda não tem cobertura de testes
- **Documentação** em português e inglês
- **Integração com GitHub** — a funcionalidade de versionamento de projetos descrita acima

> [!TIP]
> **Você pode usar o próprio Sketchware IA e sua IA integrada para desenvolver novos módulos e ferramentas para o projeto.** O agente consegue ler o código-fonte, sugerir implementações e até criar arquivos diretamente no repositório via GitHub MCP. O projeto se desenvolve com suas próprias ferramentas.

### Como começar

1. Faça um fork do repositório
2. Crie uma branch: `git checkout -b feat/minha-funcionalidade`
3. Faça suas alterações e adicione testes se possível
4. Abra um Pull Request com uma descrição clara do que foi feito

Issues marcadas com `good first issue` são boas entradas para quem está começando.

---

## Configuração do projeto

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17
- Android SDK 35

### Build

```bash
git clone https://github.com/FabioSilva11/Sketchware-IA.git
cd Sketchware-IA
./gradlew assembleDebug
```

### Variáveis de ambiente para o CI

| Variável | Descrição |
|----------|-----------|
| `SKETCHUB_API_KEY` | Chave da API do Sketchub (opcional) |
| `KEYSTORE_FILE` | Keystore em base64 para assinar o APK |
| `KEY_ALIAS` | Alias da chave no keystore |
| `KEY_PASSWORD` | Senha da chave |
| `KEYSTORE_PASSWORD` | Senha do keystore |

---

## Licença

O Sketchware IA é **source-available**, não um projeto open source convencional. Leia [LICENSE.md](LICENSE.md) antes de reutilizar código fora deste repositório.

---

<p align="center">
  Feito com ❤️ pela comunidade · <a href="https://github.com/FabioSilva11/Sketchware-IA/issues">Reportar bug</a> · <a href="https://github.com/FabioSilva11/Sketchware-IA/pulls">Contribuir</a>
</p>
