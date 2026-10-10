# Shulker Helper — Fabric

Client-side Fabric мод для Minecraft: просмотр содержимого шалкеров и бандлов без их установки, быстрое чтение, метки и сортировка.

Версия: **1.3.0** · Загрузчик: **Fabric Loader >= 0.16.10** · Среда: **client only**

## Возможности (v1.3.0)

- **Preview (просмотр):** наведите на шалкер/бандл в любом контейнере и нажмите `ALT + Shift` — всплывёт тултип с содержимым.
- **Open (чтение):** `Shift + Ctrl` (комбинация клавиши Open) — открывает содержимое в режиме read-only экране `ShulkerContentViewScreen`, не трогая серверный инвентарь.
- **Portable open (настоящее открытие):** `ПКМ` по шалкеру прямо из инвентаря — открывает реальное серверное меню. Работает **только в одиночной игре** (`from_inventory_only`).
- **Метки (Marks):** редактор метки шалкера — название + иконка. Хранится локально на клиенте (`ShulkerMarkStore`), бейдж рисуется поверх слота.
- **Сортировка:** кнопка `Sort` в GUI — сортировка по ценности (`ItemValueComparator`). Только одиночная игра. Сообщения: `sorted / sorting / already_sorted / no_space`.
- **Подсказки в тултипе:** строки `Preview: / Open: / Mark:`, `+N more / ещё N`.
- **Языки:** `en_us`, `ru_ru`.
- **Рендер поверх инвентарей:** миксин `AbstractContainerScreenMixin` + `ShulkerViewerRenderer`, кнопка сортировки `ShulkerSortHook`.

## Поддерживаемые версии Minecraft

Ветка `1.21.x` (обфусцированная, через `fabric-loom-remap` + Mojang Mappings):

- 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11

Ветка `26.x` (необфусцированная, через `fabric-loom`):

- 26.1, 26.1.1, 26.1.2, 26.2, 26.3

Исходники разбиты по source-set'ам:

```
src/main/java            — общий код (util, mixin-интерфейсы, portable, sort, renderer)
src/V1_21_1, V1_21_2, V1_21_5, V1_21_6, V1_21_9, V1_21_11 — варианты под 1.21.x
src/V26, V26_2, V26_3    — варианты под 26.x (+ свои fabric.mod.json и mixin-json)
```

Выбор ветки — через свойство `mc_group` в `gradle.properties`.

## Управление по умолчанию

| Действие | Клавиша (по умолчанию) | Строка локализации |
|---|---|---|
| Preview Shulker Contents | `ALT` + `Shift` | `key.shulker-viewer.preview` |
| Open Shulker Contents | `Shift` + `Ctrl` | `key.shulker-viewer.open` |
| Edit Shulker Mark | настраивается | `key.shulker-viewer.mark` |
| Portable open (реальное) | `ПКМ` | `shulker-viewer.hint.real` |

Все бинды переназначаются в Настройки → Управление → `Shulker Helper`.

## Сборка

Требуется JDK 21 (для 1.21.x) / JDK 25 (для 26.x). Версия выбирается автоматически из `mc_group` в `build.gradle`.

```bash
# Текущая версия по умолчанию (см. gradle.properties): mc 26.3, mod 1.3.0
./gradlew build

# Готовый jar ляжет в Versions/
# Shulker Helper 1.3.0 [26.3]-Fabric.jar
```

Смена версии Minecraft — поменять в `gradle.properties`:

```properties
minecraft_version=1.21.11
fabric_api_version=0.134.0+1.21.11
mc_group=V1_21_11
```

и пересобрать.

## Установка для игроков

1. Установи Fabric Loader + Fabric API под свою версию Minecraft.
2. Скачай `Shulker Helper 1.3.0 [ТВОЯ_ВЕРСИЯ]-Fabric.jar` из [Releases](../../releases).
3. Положи в папку `mods/`.
4. Мод клиентский — на сервер ставить не нужно.

## Структура проекта

- `src/main/java/com/shulkerviewer/` — `client/`, `gui/`, `mark/`, `mixin/`, `portable/`, `renderer/`, `sort/`, `util/`
- `src/main/resources/fabric.mod.json` — `id: shulker-viewer`, `license: MIT`, `environment: client`
- `src/main/resources/shulker-viewer.client.mixins.json` — клиентские миксины
- `src/*/resources/assets/shulker-viewer/lang/` — `en_us.json`, `ru_ru.json`
- `build.gradle` / `settings.gradle` / `gradle.properties` — мульти-версионная сборка через Loom

## Лицензия

MIT — см. [LICENSE](LICENSE).
