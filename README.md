# structured-highlights

![Build](https://github.com/luxalpa/structured-highlights/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/28073-structured-highlights.svg)](https://plugins.jetbrains.com/plugin/28073-structured-highlights)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28073-structured-highlights.svg)](https://plugins.jetbrains.com/plugin/28073-structured-highlights)

<!-- Plugin description -->
Highlight entire blocks of code in different colors!

- The initial version only supports Rust, more languages will come when I can find the time.
- Colors and opacity values are configurable under <kbd>Editor</kbd> → <kbd>Color Scheme</kbd> → <kbd>Structured
  Highlights</kbd> (it's at the very bottom)

Don't hesitate to open an issue on GitHub if there's anything you'd like to see added (including other languages). Note
for dark mode, you need to go into the settings and adjust the colors a bit as it doesn't yet contain a preset for that.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Structured
  Highlights"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28073-structured-highlights) and install it by
  clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/28073-structured-highlights/versions)
  from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/luxalpa/structured-highlights/releases/latest) and install it
  manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

## Development

### Updating the changelog

1. Write the changes into the `Unreleased` section on the changelog.
2. Update the version in `gradle.properties`
3. Run the gradle task `patchChangelog`. It will automatically move the `Unreleased` part into a new section for the
   current version.

### Migrations

Currently, each incompatible storage format gets a separate entry in the settings file (LxApplicationSettings.xml). This
allows people to roll back to an earlier version in case of breakage. However, changing settings on the earlier version
will then discard any of the newer version's properties. This could theoretically be resolved by using different files
instead for different versions, however, this use-case seems too narrow imo to be worth the implementation effort.

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
