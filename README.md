# Infinite Inventory (infinitinv)

A Minecraft **Java Edition** mod that replaces the 27 main-inventory slots with
an **unbounded**, **searchable**, **sortable** and **paginated** item store.

The armour slots, the offhand, the hotbar and the crafting area are
**completely untouched** — they keep their exact vanilla look and behaviour.

## Features

* **Unlimited capacity.** The store is a plain list with no upper bound. It is
  persisted in the player's NBT, so it survives logout, death and restarts.
  The first time the mod sees a player, whatever is in the 27 main slots is
  moved into the store, so nothing is ever stranded.
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

## Building

Requires JDK 8 for 1.12.2 (the build uses a Gradle toolchain, so a newer JDK
can be used to drive it) and a Gradle 8.x.

    gradle :1.12.2:build

The result is `versions/1.12.2/build/libs/1.12.2.jar`. Drop it into
`mods/` next to Forge 14.23.5.2860 or newer.

On NixOS the toolchain is provided by the system configuration:

    environment.systemPackages = with pkgs; [ openjdk8 openjdk17 gradle ];

## Multi version

The project is built with [Stonecutter](https://github.com/kikugie/stonecutter)
and [Unimined](https://github.com/unimined/Unimined), so several Minecraft
versions can share one source tree. `1.12.2` is ported and built by default;
see [PORTING.md](PORTING.md) for what another version needs.

## Layout

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
