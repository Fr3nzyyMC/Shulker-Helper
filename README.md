# Shulker Helper — Fabric

A client-side Fabric mod for Minecraft that lets you preview shulker box and bundle contents without placing them, quickly inspect items, add labels, and sort contents.

Version: **1.3.0** · Loader: **Fabric Loader >= 0.16.10** · Environment: **client only**

## Features (v1.3.0)

- **Preview:** Hover over a shulker box or bundle in any container and press `ALT + Shift` to display a tooltip with its contents.
- **Open (read-only):** Press `Shift + Ctrl` (the Open keybind) to view the contents in the read-only `ShulkerContentViewScreen` without modifying the server-side inventory.
- **Portable Open (actual opening):** Right-click a shulker box directly from your inventory to open its actual server-side menu. Works **only in singleplayer** (`from_inventory_only`).
- **Marks:** Edit a shulker box label, including its name and icon. Labels are stored locally on the client using `ShulkerMarkStore`, and a badge is rendered over the inventory slot.
- **Sorting:** The `Sort` button in the GUI sorts items by value using `ItemValueComparator`. Singleplayer only. Messages: `sorted / sorting / already_sorted / no_space`.
- **Tooltip hints:** Displays the `Preview: / Open: / Mark:` lines and `+N more` when additional items are present.
- **Languages:** `en_us`, `ru_ru`.
- **Inventory overlay rendering:** Uses the `AbstractContainerScreenMixin` mixin and `ShulkerViewerRenderer`, with the sorting button handled by `ShulkerSortHook`.

## Supported Minecraft Versions

The `1.21.x` branch (obfuscated, using `fabric-loom-remap` + Mojang Mappings):

- 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11

The `26.x` branch (unobfuscated, using `fabric-loom`):

- 26.1, 26.1.1, 26.1.2, 26.2, 26.3

Source code is organized into separate source sets:

```text
src/main/java            — shared code (util, mixin interfaces, portable, sort, renderer)
src/V1_21_1, V1_21_2, V1_21_5, V1_21_6, V1_21_9, V1_21_11 — variants for 1.21.x
src/V26, V26_2, V26_3    — variants for 26.x (with their own fabric.mod.json and mixin JSON files)
```

Select the branch using the `mc_group` property in `gradle.properties`.

## Default Controls

| Action | Default Keybind | Localization Key |
|---|---|---|
| Preview Shulker Contents | `ALT` + `Shift` | `key.shulker-viewer.preview` |
| Open Shulker Contents | `Shift` + `Ctrl` | `key.shulker-viewer.open` |
| Edit Shulker Mark | Configurable | `key.shulker-viewer.mark` |
| Portable Open (actual opening) | `Right-click` | `shulker-viewer.hint.real` |

All keybinds can be changed under **Options → Controls → Shulker Helper**.

## Building

Requires JDK 21 for 1.21.x or JDK 25 for 26.x. The version is selected automatically based on `mc_group` in `build.gradle`.

```bash
# Default version (see gradle.properties): Minecraft 26.3, mod 1.3.0
./gradlew build

# The compiled JAR will be placed in Versions/
# Shulker Helper 1.3.0 [26.3]-Fabric.jar
```

To change the Minecraft version, update `gradle.properties`:

```properties
minecraft_version=1.21.11
fabric_api_version=0.134.0+1.21.11
mc_group=V1_21_11
```

Then rebuild the project.

## Installation for Players

1. Install Fabric Loader and Fabric API for your Minecraft version.
2. Download `Shulker Helper 1.3.0 [YOUR_VERSION]-Fabric.jar` from [Releases](../../releases).
3. Place the JAR file in your `mods/` folder.
4. This is a client-side mod — **it does not need to be installed on the server**.

## Project Structure

- `src/main/java/com/shulkerviewer/` — `client/`, `gui/`, `mark/`, `mixin/`, `portable/`, `renderer/`, `sort/`, `util/`
- `src/main/resources/fabric.mod.json` — `id: shulker-viewer`, `license: MIT`, `environment: client`
- `src/main/resources/shulker-viewer.client.mixins.json` — client-side mixins
- `src/*/resources/assets/shulker-viewer/lang/` — `en_us.json`, `ru_ru.json`
- `build.gradle` / `settings.gradle` / `gradle.properties` — multi-version build configuration using Loom

## License

MIT — see [LICENSE](LICENSE).
