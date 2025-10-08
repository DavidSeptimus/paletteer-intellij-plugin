# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ Platform plugin project based on the IntelliJ Platform Plugin Template. The plugin is currently in initial template state with placeholder components that demonstrate the plugin architecture.

**Plugin ID**: `com.github.davidseptimus.paletteer`
**Target Platform**: IntelliJ IDEA Community Edition (IC) 2024.3.6
**Minimum Build**: 243
**Language**: Kotlin with JVM toolchain 21

## Build Commands

### Core Development Tasks
- **Build the plugin**: `./gradlew buildPlugin`
- **Run the plugin in IDE**: `./gradlew runIde`
- **Run tests**: `./gradlew test`
- **Run all checks (tests + verification)**: `./gradlew check`
- **Verify plugin structure**: `./gradlew verifyPlugin`

### Code Quality
- **Run Qodana inspections**: `./gradlew qodana`
- **Generate test coverage report**: `./gradlew koverReport`
  - Coverage XML report is auto-generated on `check` task at `build/reports/kover/report.xml`

### Plugin Verification
- **Run IntelliJ Plugin Verifier**: `./gradlew runPluginVerifier`
  - Verifies plugin compatibility with recommended IDE versions
  - Results available at `build/reports/pluginVerifier`

### UI Testing
- **Run IDE for UI tests**: `./gradlew runIdeForUiTests`
  - Launches IDE with Robot Server Plugin on port 8082

### Release Management
- **Get changelog**: `./gradlew getChangelog`
- **Patch changelog**: `./gradlew patchChangelog`
- **Publish plugin**: `./gradlew publishPlugin`
  - Requires `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` environment variables

## Project Architecture

### Plugin Configuration
Plugin metadata and configuration are centralized in multiple locations:
- **`gradle.properties`**: Version, platform target, build numbers, dependencies
- **`src/main/resources/META-INF/plugin.xml`**: Plugin extensions, dependencies, and component registration
- **`build.gradle.kts`**: Build configuration, dependency management, tasks
- **`README.md`**: Plugin description (extracted between `<!-- Plugin description -->` markers and injected into plugin.xml during build)

### Core Components

**Services** (`src/main/kotlin/.../services/`)
- Project-level services annotated with `@Service(Service.Level.PROJECT)`
- Accessed via `project.service<ServiceClass>()`
- Current example: `MyProjectService` - demonstrates service initialization and project-scoped functionality

**Tool Windows** (`src/main/kotlin/.../toolWindow/`)
- Registered in `plugin.xml` under `<extensions>` with `<toolWindow>` tag
- Factory classes implement `ToolWindowFactory`
- Current example: `MyToolWindowFactory` - creates custom tool window with Swing UI components

**Startup Activities** (`src/main/kotlin/.../startup/`)
- Registered in `plugin.xml` as `<postStartupActivity>`
- Implement `ProjectActivity` with suspending `execute()` function
- Run after project is opened and indexed
- Current example: `MyProjectActivity` - executes code on project startup

**Localization** (`src/main/kotlin/.../MyBundle.kt` + `src/main/resources/messages/`)
- Uses `DynamicBundle` for i18n
- Message keys defined in `messages/MyBundle.properties`
- Accessed via `MyBundle.message(key, params)`

### Key Files to Modify
When adapting this template for actual plugin functionality:
1. Update `pluginGroup`, `pluginName`, and `pluginVersion` in `gradle.properties`
2. Update plugin `<id>` and `<name>` in `src/main/resources/META-INF/plugin.xml`
3. Update plugin description in `README.md` between the special comment markers
4. Rename package structure from `com.github.davidseptimus.paletteer`
5. Remove/replace template components (all contain warning logs about removing sample code)

### Extension Points
The plugin extends the IntelliJ Platform through:
- **Tool Windows**: Custom UI panels in the IDE window (`com.intellij.toolWindow`)
- **Post Startup Activities**: Code executed after project initialization (`postStartupActivity`)
- Additional extension points can be registered in `plugin.xml` under `<extensions defaultExtensionNs="com.intellij">`

### Dependencies
- **Platform Modules**: `com.intellij.modules.platform` (required base dependency)
- **Test Framework**: IntelliJ Platform Test Framework
- **Test Libraries**: JUnit, OpenTest4J
- Additional bundled plugins and external plugins can be added via `platformBundledPlugins` and `platformPlugins` in `gradle.properties`

## CI/CD Pipeline

The project uses GitHub Actions with three main workflows:

**Build Workflow** (`.github/workflows/build.yml`)
- Triggered on push to `main` and pull requests
- Jobs: build, test, inspectCode, verify, releaseDraft
- Creates draft releases automatically on main branch

**Release Workflow** (triggered on publishing a release)
- Publishes plugin to JetBrains Marketplace

**UI Tests Workflow** (`.github/workflows/run-ui-tests.yml`)
- Runs UI automation tests using Robot Server Plugin

## Development Notes

### Gradle Configuration
- Uses Gradle 9.0.0 with configuration cache and build cache enabled
- IntelliJ Platform Gradle Plugin handles IDE dependencies and build tasks
- Version catalog manages dependency versions (`libs.versions.toml`)

### Plugin Signing
Required for marketplace publication:
- Set `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` environment variables
- Configured in `build.gradle.kts` signing block

### Release Channels
Plugin version determines release channel (defined in `build.gradle.kts`):
- `x.y.z` → default channel
- `x.y.z-alpha.n` → alpha channel
- `x.y.z-beta.n` → beta channel
- `x.y.z-eap.n` → eap channel
- Paletteer is plugin for IntelliJ color scheme developers to help quickly analyze a theme's colors and perform quick color replacements.
- Externalize all string rendered in the ui to src/main/resources/messages/paletteerBundle.properties