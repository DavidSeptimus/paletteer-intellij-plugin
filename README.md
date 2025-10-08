# <img src="src/main/resources/META-INF/pluginIcon.svg" alt="Paletteer Icon" width="32" height="32" /> Paletteer (IntelliJ Plugin)


[//]: # ()

[//]: # (![Build]&#40;https://github.com/DavidSeptimus/paletteer-intellij-plugin/workflows/Build/badge.svg&#41;)

[//]: # ([![Version]&#40;https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg&#41;]&#40;https://plugins.jetbrains.com/plugin/MARKETPLACE_ID&#41;)

[//]: # ([![Downloads]&#40;https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg&#41;]&#40;https://plugins.jetbrains.com/plugin/MARKETPLACE_ID&#41;)

[//]: # ()
[//]: # (## Template ToDo list)

[//]: # (- [x] Create a new [IntelliJ Platform Plugin Template][template] project.)

[//]: # (- [ ] Get familiar with the [template documentation][template].)

[//]: # (- [ ] Adjust the [pluginGroup]&#40;./gradle.properties&#41; and [pluginName]&#40;./gradle.properties&#41;, as well as the [id]&#40;./src/main/resources/META-INF/plugin.xml&#41; and [sources package]&#40;./src/main/kotlin&#41;.)

[//]: # (- [ ] Adjust the plugin description in `README` &#40;see [Tips][docs:plugin-description]&#41;)

[//]: # (- [ ] Review the [Legal Agreements]&#40;https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate&#41;.)

[//]: # (- [ ] [Publish a plugin manually]&#40;https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate&#41; for the first time.)

[//]: # (- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.)

[//]: # (- [ ] Set the [Plugin Signing]&#40;https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate&#41; related [secrets]&#40;https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables&#41;.)

[//]: # (- [ ] Set the [Deployment Token]&#40;https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate&#41;.)

[//]: # (- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.)

[//]: # (- [ ] Configure the [CODECOV_TOKEN]&#40;https://docs.codecov.com/docs/quick-start&#41; secret for automated test coverage reports on PRs)

<!-- Plugin description -->
Paletteer is an IntelliJ plugin that provides tools for theme developers to rapidly iterate on color themes within the IDE.

### Features

- Color and Text Attribute Lookup: Quickly filter and find colors or text attributes by name, including regex-based filtering.
- 1-click text attribute updates: Easily update text attributes with a single click from the lookup table.
- Instant text attribute lookup on caret position: Automatically see the text attribute at the caret position.
- Bulk color replacement: Replace all instances of a specific color in the current color scheme with a new color.
- Context menu actions:
  - View all highlights for the caret position (click a color preview to copy it to the clipboard).
- Hotkey actions:
  - Show all highlights for the caret position (`alt+shift+c`).
  - Copy color under caret to clipboard (`alt+c`).
- Improved color scheme export:
  - Optional font settings export.
  - Configurable scheme name and file extension, so you don't need to rename the exported file every time.
  - Success notification with a link to the exported file.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Paletteer"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/DavidSeptimus/paletteer-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
