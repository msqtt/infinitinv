# Infinite Inventory (`infinitinv`)

[中文](README.zh-CN.md) | English

A Minecraft **Java Edition** mod that replaces the 27 main-inventory slots with
an **unbounded**, **searchable**, **sortable** and **paginated** item store.

The armour slots, the offhand, the hotbar and the crafting area are
**completely untouched** — they keep their exact vanilla look and behaviour.

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-c78b25)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860-1e6e2d)
![Java](https://img.shields.io/badge/Java-8-5382a4)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Features

* **Unlimited capacity.** The store is a plain list with no upper bound. It is
  persisted in the player's NBT, so it survives logout, death and server
  restarts. The first time the mod sees a player, whatever is in the 27 main
  slots is moved into the store, so nothing is ever stranded.
* **Search.** Type into the search box to filter the store by registry id
  (`minecraft:diamond_sword`) or by item name.
* **Automatic sorting.** `ID`, `Name` and `Type` (item class) — one button
  cycles through them.
* **Pagination.** The vanilla inventory shows one page of 27 items; the
  dedicated screen shows 45 at a time. Both can be paged through.
* **Browse every item in the game.** The `All items` mode lists every
  registered item, not just the ones you own.
* **Server authoritative.** The server owns the data, the client only holds a
  mirror. Every mutation travels as an *absolute* index, so the server never
  needs to know which page you are looking at.

## Controls

| Action | How |
|---|---|
| Open the infinite inventory screen | `K` (rebindable in the key bindings) |
| Browse the store in the normal inventory | press `E` — the 27 slots are the store |
| Page in the normal inventory | the `<` / `>` buttons next to the armour slots |
| Take an item out of the store | click it in the infinite inventory screen |
| Put an item into the store | `All items` mode, then click the item |

## Installation

1. Install **Forge 14.23.5.2860** or newer for Minecraft 1.12.2.
2. Drop `infinitinv-1.12.2.jar` into your `mods/` folder.
3. Launch the game. The hotbar and armour keep working exactly as before.

## Building from source

The project uses [Gradle](https://gradle.org) with a wrapper, so no local
Gradle install is needed:

    ./gradlew :1.12.2:build

The result is `versions/1.12.2/build/libs/1.12.2.jar`.

Requirements: a JDK to drive Gradle (17+) and a **JDK 8** as the compile
toolchain for the 1.12.2 target. Gradle finds it through
`org.gradle.java.installations.paths` in `gradle.properties`; point that at
your own JDK 8 if you are not on NixOS:

    ./gradlew :1.12.2:build \
      -Dorg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk17

On NixOS the whole toolchain is provided by the system configuration:

```nix
users.users.bob.packages = with pkgs; [ openjdk8 openjdk17 gradle ];
```

## Multi version

The project is built with [Stonecutter](https://github.com/kikugie/stonecutter)
and [Unimined](https://github.com/unimined/Unimined), so several Minecraft
versions can share one source tree. `1.12.2` is ported and built by default;
see [PORTING.md](PORTING.md) for what another version needs and for a table
of the API differences between version families.

`gradle.properties` holds `readyVersions=1.12.2` — only the versions listed
there are compiled by `./gradlew build`, the other version directories are
kept in the tree so the porting work stays visible.

## Layout

```
src/main/java/com/infinitinv/
  InfiniteInv.java              the @Mod entry point
  storage/ExtendedStorage.java  the unbounded store: filter, sort, paging
  storage/StorageIO.java        NBT (de)serialisation
  storage/IExtendedStorageHolder.java
  slot/ExtendedSlot.java        a vanilla Slot backed by the store
  mixin/PlayerMixin.java        attaches + persists the store
  mixin/ContainerPlayerMixin.java   swaps the 27 main slots
  mixin/GuiInventoryMixin.java   page indicator + page buttons
  network/                      sync snapshot + action messages
  client/ClientHooks.java       key binding
  client/gui/GuiInfiniteInventory.java  the store screen
versions/                       per-version Gradle properties and deps
```

## How it works

* `ContainerPlayerMixin` injects at the end of the `ContainerPlayer`
  **constructor** (1.12.2 creates its slots there) and replaces every slot
  whose inventory index is in `9..35` with an `ExtendedSlot`. The index test
  is used on purpose: it is independent of the order the vanilla slots are
  added in, so the armour, crafting and hotbar slots can never be hit by
  accident.
* `ExtendedSlot` is a normal vanilla `Slot` — it only overrides the content
  accessors. Drag and drop, shift clicking, hotbar swaps and the cursor
  stack therefore keep working unchanged.
* The server owns the data. `SyncStorageMessage` ships a full NBT snapshot
  (items, filter, page, query, sort order) to the client, and
  `ActionMessage` carries a mutation plus an **absolute** index back.
* Unimined remaps the mixin targets and builds the refmap itself during
  `remapJar`, so the stock Mixin annotation processor is deliberately not
  used.

## License

MIT — see [LICENSE](LICENSE).
