<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="140" alt="Sketchware IA logo">
</p>

<h1 align="center">Sketchware IA</h1>

<p align="center">
  Crie aplicativos Android completos diretamente do celular usando blocos visuais, Java/Kotlin nativo e inteligência artificial integrada.
</p>

<p align="center">
  <a href="https://github.com/FabioSilva11/Sketchware-IA/actions/workflows/android.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/FabioSilva11/Sketchware-IA/android.yml?branch=main&label=Android%20CI" alt="Android CI">
  </a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/commits/main">
    <img src="https://img.shields.io/github/last-commit/FabioSilva11/Sketchware-IA?label=último%20commit" alt="Last commit">
  </a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/pulls">
    <img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs welcome">
  </a>
  <a href="LICENSE.md">
    <img src="https://img.shields.io/badge/license-source--available-lightgrey" alt="License">
  </a>
</p>

---

## O que é o Sketchware IA?

O **Sketchware IA** é um fork avançado do Sketchware Pro focado em transformar o celular em um ambiente completo de desenvolvimento Android.

O projeto combina:

- 🧩 Programação visual com blocos arrastáveis
- ☕ Desenvolvimento Android nativo em Java
- 🔵 Suporte a Kotlin
- 🤖 Inteligência artificial integrada com ferramentas reais
- 🛠️ Automação e terminal integrado
- 📱 Desenvolvimento totalmente mobile — sem precisar de PC

A ideia principal é simples:

> **«Permitir que qualquer pessoa consiga criar aplicativos reais usando apenas o celular.»**

---

## Formas de desenvolver aplicativos

O Sketchware IA foi pensado tanto para iniciantes quanto para desenvolvedores avançados. Você pode criar aplicativos de diferentes maneiras:

---

### 🧩 Desenvolvimento por blocos

A forma mais simples e acessível. Você cria a lógica do aplicativo conectando blocos visuais, sem precisar escrever código manualmente.

**Recursos:**
- Blocos arrastáveis estilo Scratch
- Eventos, variáveis e funções
- Loops, condicionais e lógica avançada
- Navegação entre telas
- APIs e requisições HTTP
- Banco de dados local
- Componentes Android reais
- Código Java gerado automaticamente em segundo plano

**Ideal para:** iniciantes, ensino de programação, prototipagem rápida.

---

### ☕ Desenvolvimento Java nativo

Para usuários avançados que desejam controle total. É possível criar projetos completamente em Java sem usar blocos.

**Recursos:**
- Editor de código integrado com syntax highlight
- Acesso completo ao Android SDK
- Integração com bibliotecas `.jar` e `.aar`
- Código customizado em qualquer parte do projeto
- Controle total da arquitetura

---

### 🔵 Kotlin (em evolução)

O Sketchware IA está evoluindo para oferecer suporte completo ao Kotlin, com projetos nativos, compatibilidade moderna com as APIs Android e código mais seguro e expressivo.

---

### 🤝 Modo híbrido — Blocos + código

A abordagem mais poderosa. Use blocos para criar a estrutura do app e insira código Java ou Kotlin apenas onde precisar.

**Exemplo:**
- Interface e navegação criadas com blocos
- Lógica simples em programação visual
- Funções avançadas escritas em código

Isso permite aprender gradualmente, misturar simplicidade com poder e criar apps extremamente avançados sem abandonar os blocos.

---

## Inteligência Artificial integrada

O Sketchware IA possui um sistema de IA nativo integrado diretamente ao editor. **Não é apenas um chatbot.** A IA possui acesso a ferramentas reais do sistema e atua como um agente de desenvolvimento autônomo.

---

### ✅ AI Fix — correção automática de erros

Quando um projeto falha na compilação, o **AI Fix** entra em ação:

- Lê os logs de compilação
- Identifica a causa raiz do erro (não apenas o primeiro relatado)
- Corrige blocos quebrados e código Java/Kotlin
- Aplica correções automáticas seguras com um toque
- Exibe resumo explicativo antes de qualquer alteração
- Sugere os passos manuais quando a correção é complexa

---

### ✅ Edição de arquivos do projeto

A IA pode criar, editar, refatorar e organizar arquivos diretamente no projeto — com suporte a blocos de busca e substituição precisos.

---

### ✅ Terminal integrado

A IA possui acesso ao terminal do dispositivo e consegue:

- Executar comandos Gradle
- Compilar projetos
- Instalar dependências
- Rodar scripts de automação
- Qualquer comando disponível no ambiente

Timeout de **30 segundos** para operações longas (builds, compilação).

---

### ✅ Navegação pelo projeto

Ferramentas disponíveis para o agente explorar o projeto:

- Ler arquivos e listar pastas
- Analisar estrutura e dependências
- Buscar código em qualquer arquivo

---

### ✅ MCP — Model Context Protocol

O Sketchware IA suporta integração com servidores MCP via HTTP, o que permite adicionar ferramentas ilimitadas para a IA:

- APIs externas
- Sistemas de automação
- Ferramentas de build customizadas
- Banco de dados
- IA especializada
- Qualquer ferramenta que você criar

---

### Modelos de IA suportados

O Sketchware IA se conecta a provedores via **OpenRouter**, com suporte a modelos gratuitos e pagos:

| Modelo | Contexto | Gratuito |
|--------|----------|----------|
| `google/gemini-2.0-flash-exp:free` | 1M tokens | ✅ |
| `anthropic/claude-sonnet-4` | 200K tokens | ❌ |
| `anthropic/claude-opus-4` | 200K tokens | ❌ |
| `deepseek/deepseek-r1` | 128K tokens | ❌ |
| `qwen/qwen3-235b-a22b` | 40K tokens | ❌ |

> **Recomendação gratuita:** use `google/gemini-2.0-flash-exp:free` — 1 milhão de tokens de contexto, tool calls nativas e sem custo.

---

### GitHub MCP integrado

O agente tem acesso nativo à API do GitHub com **13 ferramentas** disponíveis diretamente no chat:

| Ferramenta | O que faz |
|------------|-----------|
| `github_list_repos` | Lista repositórios |
| `github_get_repo` | Detalhes do repositório |
| `github_list_branches` | Lista branches |
| `github_get_file` | Lê conteúdo de arquivo |
| `github_list_files` | Navega por diretório |
| `github_search_code` | Pesquisa código |
| `github_list_issues` | Lista issues |
| `github_create_issue` | Cria issue |
| `github_list_pull_requests` | Lista PRs |
| `github_create_pull_request` | Abre PR |
| `github_create_or_update_file` | Faz commit de arquivo |
| `github_list_commits` | Histórico de commits |
| `github_get_commit` | Detalhes de um commit |

Configure em **Settings → GitHub Settings** com um Personal Access Token.

---

## Integração com GitHub — versionamento de projetos

> 🔜 **Funcionalidade planejada**

Uma das próximas grandes entregas é a **integração direta do GitHub com os projetos do Sketchware IA**, transformando o app em um ambiente profissional de desenvolvimento mobile com versionamento completo — tudo direto do celular.

### Funcionalidades planejadas

**✅ Login com GitHub**
- Conectar conta GitHub
- Vincular repositórios existentes ou criar novos
- Sincronização automática de projetos

**✅ Backup automático na nuvem**
- Projetos enviados automaticamente para o GitHub
- Nunca perca um projeto ao trocar de celular ou formatar o aparelho

**✅ Histórico completo de versões**
- Cada alteração gera commits automáticos
- Restaure qualquer versão anterior com um toque
- Compare mudanças entre versões
- Trabalhe com branches para funcionalidades separadas

**✅ Colaboração em equipe**
- Compartilhe projetos com outras pessoas
- Receba contribuições via pull requests
- Trabalhe em equipe no mesmo app

---

## Visão de futuro — Migração para Flutter

> 🔮 **Roadmap de longo prazo**

A intenção é **migrar o Sketchware IA completamente para Flutter** — tanto a interface quanto a arquitetura interna.

### Por que Flutter?

- Interface consistente e moderna em todas as versões Android
- Performance superior com rendering próprio
- Codebase única para múltiplas plataformas
- Ecossistema rico de widgets e bibliotecas
- Expansão natural para desktop e web

### Compilação multiplataforma com Dart

Com a migração para Flutter e **Dart** como linguagem base, o Sketchware IA ganhará a capacidade de compilar projetos criados no app para múltiplas plataformas:

| Plataforma | Suporte |
|------------|---------|
| Android | ✅ |
| iOS | ✅ |
| Web | ✅ |
| Windows | ✅ |
| Linux | ✅ |
| macOS | ✅ |

Um app criado no Sketchware IA poderá rodar em qualquer dispositivo e sistema operacional, mantendo a mesma experiência de desenvolvimento visual com blocos arrastáveis.

> **Importante:** mesmo com essa visão futura, o desenvolvimento atual em Java/Kotlin continua ativo e continuará recebendo melhorias constantemente.

---

## Contribuindo

O Sketchware IA é um projeto aberto para a comunidade. Contribuições são extremamente bem-vindas.

### O que você pode contribuir

- Novos blocos para o editor visual
- Ferramentas MCP e integrações externas
- Melhorias no agente de IA e nos prompts
- Novos módulos e funcionalidades
- Integração com GitHub (versionamento de projetos)
- Melhorias na interface
- Correções de bugs
- Traduções e documentação
- Testes automatizados

### Como começar

```bash
# 1. Faça um fork do repositório
# 2. Crie uma branch para sua funcionalidade
git checkout -b feat/minha-funcionalidade

# 3. Faça suas alterações
# 4. Abra um Pull Request com descrição clara
```

Issues marcadas com `good first issue` são ótimas entradas para quem está começando.

---

> [!TIP]
> ### 🤖 Use IA para contribuir com o projeto
>
> **Você pode usar inteligência artificial — inclusive o próprio Sketchware IA — para criar novos módulos, ferramentas e melhorias para o projeto.**
>
> A IA integrada pode ajudar a:
> - Criar código e novos módulos
> - Refatorar sistemas existentes
> - Desenvolver ferramentas MCP personalizadas
> - Automatizar partes do desenvolvimento
> - Criar commits e abrir PRs diretamente via GitHub MCP
>
> A ideia do projeto é justamente expandir os limites do desenvolvimento mobile usando IA. Use-a para contribuir.

---

## Build do projeto

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17
- Android SDK 35

### Compilar

```bash
git clone https://github.com/FabioSilva11/Sketchware-IA.git
cd Sketchware-IA
./gradlew assembleDebug
```

### Variáveis de ambiente para CI

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
  Feito com ❤️ pela comunidade Sketchware IA<br>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/issues">Reportar bug</a> · <a href="https://github.com/FabioSilva11/Sketchware-IA/pulls">Contribuir</a>
</p>
