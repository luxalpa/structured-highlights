<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Structured Highlights Changelog

## [Unreleased]

## 0.0.3

- Remove platform compatibility upper bound to enable "Open-End" Compatibility. It is not expected for this plugin to
  cause major issues in the rare event of a breaking change. And the annoyance of having to wait for the plugin
  maintainer is going to be the bigger problem.

## 0.0.2

- Removed inner block highlighting as it was causing severe performance issues.

## 0.0.1

Initial release with:

- Rust support
- Configurable colors for blocks and highlights
- Configurable Transparency
- Configuration is currently not stored within the Color profile, and there is only a preset for light mode, so dark
  mode needs to be manually adjusted (very easy though)
