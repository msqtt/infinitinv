# Infinite Inventory (`infinitinv`)

[中文](README.zh-CN.md) | English

A Minecraft **Java Edition** mod that replaces the 27 main-inventory slots with
an **unbounded**, **searchable**, **sortable** and **paginated** item store.

The armour slots, the offhand, the hotbar and the crafting area are
**completely untouched** — they keep their exact vanilla look and behaviour.

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-c78b25)
![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-c78b25)
![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-c78b25)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860%20%2F%2047.3.0%20%2F%2052.0.28-1e6e2d)
![Java](https://img.shields.io/badge/Java-8%20%2F%2017%20%2F%2021-5382a4)
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

| Minecraft | Forge | Jar |
|---|---|---|
| 1.12.2 | 14.23.5.2860 | `infinitinv-1.12.2.jar` |
| 1.20.1 | 47.3.0 | `infinitinv-1.20.1.jar` |
| 1.21.1 | 52.0.28 | `infinitinv-1.21.1.jar` |

1. Install the matching Forge version.
2. Drop the matching `infinitinv-<mcversion>.jar` into your `mods/` folder.
3. Launch the game. The hotbar and armour keep working exactly as before.

## Building from source

The project uses [Gradle](https://gradle.org) with a wrapper, so no local
Gradle install is needed:

    ./gradlew :1.12.2:build
    ./gradlew :1.20.1:build
    ./gradlew :1.21.1:build

The result is `versions/<mcversion>/build/libs/<mcversion>.jar`.

Requirements: a JDK to drive Gradle (17+) and a **JDK 8**, **JDK 17** and
**JDK 21** as the compile toolchains for the 1.12.2, 1.20.1 and 1.21.1
targets. Gradle finds it through
`org.gradle.java.installations.paths` in `gradle.properties`; point that at
your own JDK 8 if you are not on NixOS:

    ./gradlew :1.21.1:build \
      -Dorg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk17,/path/to/jdk21

On NixOS the whole toolchain is provided by the system configuration:

```nix
users.users.bob.packages = with pkgs; [ openjdk8 openjdk17 openjdk21 gradle ];
```

## Multi version

The project is built with [Stonecutter](https://github.com/kikugie/stonecutter)
and [Unimined](https://github.com/unimined/Unimined), so several Minecraft
versions can share one source tree. `1.12.2`, `1.20.1` and `1.21.1` are
ported and built by default; see [PORTING.md](PORTING.md) for what another
version needs and for a table of the API differences between version
families.

`gradle.properties` holds `readyVersions=1.12.2,1.20.1,1.21.1` — only the
versions listed there are compiled by `./gradlew build`, the other version
directories are kept in the tree so the porting work stays visible.

The 1.12.2 port lives in `versions/1.12.2/src/main/java`, the 1.20.1 port
in `versions/1.20.1/src/main/java` and the 1.21.1 port in
`versions/1.21.1/src/main/java`. The two modern ports share the same
design but use their own API calls, because Forge's networking layer
changed completely in 1.21.

## Layout

```
versions/1.12.2/src/main/java/com/infinitinv/
  InfiniteInv.java              the @Mod entry point
  storage/ExtendedStorage.java  the unbounded store: filter, sort, paging
  storage/StorageIO.java        NBT (de)serialisation
  slot/ExtendedSlot.java        a vanilla Slot backed by the store
  mixin/PlayerMixin.java        attaches + persists the store
  mixin/ContainerPlayerMixin.java   swaps the 27 main slots
  mixin/GuiInventoryMixin.java   page indicator + page buttons
  network/                      sync snapshot + action messages
  client/ClientHooks.java       key binding
  client/gui/GuiInfiniteInventory.java  the store screen
versions/1.20.1/src/main/java/com/infinitinv/   same design, modern API
versions/1.21.1/src/main/java/com/infinitinv/   same design, 1.21 API
versions/<mcversion>/           per-version Gradle properties and deps
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
* The 1.20.1 port uses `net.minecraft.world.item.ItemStack`,
  `net.minecraft.nbt.CompoundTag` and the `SimpleChannel` networking of
  Forge 47. The 1.21.1 port uses the data-component item serialisation
  (`ItemStack.save(provider, tag)`), `ChannelBuilder` and the
  `CustomPayloadEvent` based networking of Forge 52 — the payload API
  replaced the old `IMessage` handler there.

## License

MIT — see [LICENSE](LICENSE).
