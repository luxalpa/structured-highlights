<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Structured Highlights Changelog

## [Unreleased]

- Color `macro_rules` definitions
- Improve coloring for trait functions without bodies.
- Add support for:
    - TOML
    - Kotlin
    - Java
    - PHP
    - C#
    - JavaScript

## [1.0.2] - 2026-08-21

- Fixed compatibility with 2026.2.2 eap (one of the classes was marked as internal)

## [1.0.1] - 2026-08-21

- Fixed an issue where the migration script prevented changes to the color scheme to persist over IDE restarts.
- Change Color Settings Page in preparation for supporting multiple languages.

## [1.0.0]

- Replaced the opacity-based renderer with an opaque one. Massively improves performance during scrolling.
- Added a setting to configure opacity of the Caret Row, since it would now completely paint over the highlights (as
  those are now on the furthest background layer)
- Moved color settings into the color scheme. As a consequence, there's now two settings pages: One for opacity values
  and one for the colors on the color scheme.

## [0.0.3]

- Remove platform compatibility upper bound to enable "Open-End" Compatibility. It is not expected for this plugin to
  cause major issues in the rare event of a breaking change. And the annoyance of having to wait for the plugin
  maintainer is going to be the bigger problem.

## [0.0.2]

- Removed inner block highlighting as it was causing severe performance issues.

## [0.0.1]

Initial release with:

- Rust support
- Configurable colors for blocks and highlights
- Configurable Transparency
- Configuration is currently not stored within the Color profile, and there is only a preset for light mode, so dark
  mode needs to be manually adjusted (very easy though)

[Unreleased]: https://github.com/luxalpa/structured-highlights/compare/v1.0.2...HEAD
[1.0.2]: https://github.com/luxalpa/structured-highlights/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/luxalpa/structured-highlights/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/luxalpa/structured-highlights/compare/v0.0.3...v1.0.0
[0.0.3]: https://github.com/luxalpa/structured-highlights/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/luxalpa/structured-highlights/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/luxalpa/structured-highlights/commits/v0.0.1
