# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [1.2.0] - 2026-10-25

### Added
- Minecraft 1.21.1 support (Forge 52.0.28): the port uses the data
  component item serialisation, `ChannelBuilder` and the
  `CustomPayloadEvent` based networking of the 1.21 payload API.
- The build now produces all three versions at once, and the CI workflow
  builds them in a matrix with a JDK 8, 17 and 21 toolchain each.
- Complete `mods.toml` metadata with the supported Minecraft and Forge
  version ranges.

## [1.1.0] - 2026-10-25

### Added
- Minecraft 1.20.1 support (Forge 47.3.0): the modern API port of the mod
  with `net.minecraft.world.item` classes, `GuiGraphics` based rendering
  and Forge `SimpleChannel` networking.

## [1.0.0] - 2026-10-24

### Added
- Unbounded, NBT-persisted item store that replaces the 27 main inventory
  slots. Items in the vanilla main slots are migrated into the store the first
  time the mod sees a player.
- Search box filtering the store by registry id or item name.
- Automatic sorting by `ID`, `Name` or `Type`.
- Pagination: 27 items per page in the vanilla inventory, 45 per page in the
  dedicated screen.
- An `All items` mode that lists every item registered in the game.
- A dedicated infinite inventory screen (key `K`, rebindable) with a search
  box, sort button, mode toggle, pagination and item tooltips.
- A page indicator and page buttons in the vanilla inventory screen.
- Server-authoritative networking: a full NBT snapshot to the client and
  action messages carrying absolute indices back.

### Infrastructure
- Stonecutter + Unimined build, so several Minecraft versions can share one
  source tree. 1.12.2 is ported and built by default.
- Gradle wrapper included for reproducible builds.
- GitHub Actions workflow that builds the mod on every push.
