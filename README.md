<p align="center">
  <img src="assets/Sketchware-Pro.png" width="140" alt="Sketchware IA logo">
</p>

<h1 align="center">Sketchware IA</h1>

<p align="center">
  A community-maintained, source-available continuation of Sketchware Pro for Android creators.
</p>

<p align="center">
  <a href="https://github.com/FabioSilva11/Sketchware-IA/actions/workflows/android.yml"><img src="https://img.shields.io/github/actions/workflow/status/FabioSilva11/Sketchware-IA/android.yml?branch=main&label=Android%20CI" alt="Android CI"></a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/commits/main"><img src="https://img.shields.io/github/last-commit/FabioSilva11/Sketchware-IA?label=last%20commit" alt="Last commit"></a>
  <a href="https://github.com/FabioSilva11/Sketchware-IA/pulls"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs welcome"></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/license-source--available-lightgrey" alt="Source-available license"></a>
</p>

> [!NOTE]
> This README is written in English to make the repository easier to discover globally. Portuguese-speaking contributors are very welcome.

Sketchware IA keeps the Sketchware experience alive with community-driven fixes, editor improvements, build tooling, and long-term maintenance. This repository is the main home for the Android app source code, contribution workflow, and project automation.

> [!IMPORTANT]
> Sketchware IA is source-available, not a conventional open-source project. Please read [LICENSE.md](LICENSE.md) before reusing code outside this repository.

## Why this project exists

Sketchware changed how many people learned Android development on mobile. Sketchware IA exists to preserve that experience, improve it with community work, and keep the project accessible to new and experienced creators alike.

## Highlights

- Community-maintained continuation of Sketchware Pro.
- Android app source code built with Gradle, Java, Kotlin, and modern AndroidX dependencies.
- Project editing, resource management, preview tooling, import/export flows, and custom component support.
- GitHub Actions workflow for reproducible CI builds.
- Structured issue forms, pull request template, security policy, and contribution guidelines.

## Quick Start

### Requirements

- Android Studio with JDK 17 support.
- Android SDK configured locally.
- Git.

### Build locally

```bash
git clone https://github.com/FabioSilva11/Sketchware-IA.git
cd Sketchware-IA
./gradlew assembleRelease
```

On Windows, use:

```powershell
.\gradlew.bat assembleRelease
```

### Optional environment variables

- `GOOGLE_SERVICES_JSON` or `GOOGLE_SERVICES_JSON_BASE64` for Firebase-related setup.
- `SKETCHUB_API_KEY` for integrations that depend on Sketchub services.

## Repository Map

| Path | Purpose |
| --- | --- |
| `app/` | Main Android application source, resources, manifest, and Gradle module. |
| `.github/` | Workflows, issue forms, templates, and repository automation. |
| `assets/` | Branding assets and shared project images. |
| `ANALISE_SKETCHWARE_LAYOUTS_E_PROJETOS.md` | Internal analysis and architecture notes. |

## Contributing

We welcome bug fixes, UI polish, performance work, documentation improvements, and carefully scoped new features.

Before opening a pull request:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md).
2. Keep your change focused and well described.
3. Test the affected flow locally whenever possible.
4. Use a clear commit message such as `fix: prevent crash on project import`.

If you are new here, documentation, bug reproduction, and cleanup PRs are excellent ways to get started.

## Community

- Telegram: [t.me/sketcware_ia](https://t.me/sketcware_ia)
- Discord: [discord.gg/kq39yhT4rX](http://discord.gg/kq39yhT4rX)
- Security reports: [sketchwarepromod@gmail.com](mailto:sketchwarepromod@gmail.com)
- Support the project: [Patreon](https://www.patreon.com/sketchware)

## Security

Please do not report security issues in public issues. Follow the instructions in [SECURITY.md](SECURITY.md).

## Disclaimer

Sketchware IA is a community effort made to preserve and improve the Sketchware experience. Do not publish this project, or modified versions of it, to app stores as if it were an official release. Read [LICENSE.md](LICENSE.md) for the copyright and reuse context before redistributing code or binaries.

If this project helps you, star the repository, share it with other builders, and consider contributing a pull request.
