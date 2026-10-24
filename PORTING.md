# Porting the mod to another Minecraft version

The project uses **Stonecutter** (one source tree, many versions) on top of
**Unimined** (one build for Forge, Fabric, NeoForge, ... ).

    versions/
      1.12.2/      <- ported and built by default
      1.20.1/      <- scaffolded, not ported yet

`gradle.properties` holds `readyVersions=1.12.2`. Everything listed there is
compiled by `gradle build`; the other version directories are skipped so the
default build stays green. To work on a version, add it to `readyVersions`.

## Adding a new version

1. Create `versions/<mcVersion>/` with a `gradle.properties`
   (`mcVersion`, `forgeVersion`, and `mcpMappingVersion` for 1.12.x) and a
   `deps.gradle` with the version specific dependencies.
2. Add the version to `stonecutter { create(rootProject) { versions(...) } }`
   in `settings.gradle.kts`.
3. Port the code. The parts that differ between versions:

| Concern | 1.12.2 | 1.13 - 1.16 | 1.17+ |
|---|---|---|---|
| Item stack | `net.minecraft.item.ItemStack` | same | `net.minecraft.world.item.ItemStack` |
| NBT | `net.minecraft.nbt.NBTTagCompound` | same | `net.minecraft.nbt.CompoundTag` |
| Slot | `net.minecraft.inventory.Slot` | same | `net.minecraft.world.inventory.Slot` |
| Player container | `net.minecraft.inventory.ContainerPlayer` | `net.minecraft.inventory.container.InventoryMenu` (1.16+) | same as 1.16 |
| Network | `net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper` | same | `net.minecraftforge.network.simple.SimpleChannel` |
| Loader side | `net.minecraftforge.fml.relauncher.Side` | same | `net.minecraftforge.api.distmarker.Dist` |
| Screen | `net.minecraft.client.gui.inventory.GuiContainer` | `net.minecraft.client.gui.screen.inventory.AbstractContainerScreen` (1.14+) | same as 1.14 |
| Item rendering | `net.minecraft.client.renderer.RenderItem` | `net.minecraft.client.renderer.item.ItemRenderer` (1.15+) | `net.minecraft.client.renderer.entity.ItemRenderer` |
| Key binding | `net.minecraft.client.settings.KeyBinding` | same | `net.minecraft.client.KeyMapping` |
| Item registry | `Item.REGISTRY` | same | `ForgeRegistries.ITEMS` |
| Player NBT | `writeEntityToNBT` / `readEntityFromNBT` | same | same |

4. The easiest way to deal with the differences is Stonecutter's comment
   syntax inside the source files:

       //? if=1.12.2 {
       stack.writeToNBT(new NBTTagCompound());
       //? } else {
       // stack.save(new CompoundTag());
       //? }

   The block that does not apply is commented out, so only one version of the
   code is ever compiled.

## Why 1.20.1 is not built yet

1.20.1 needs the whole `net.minecraft.item` -> `net.minecraft.world.item`
package move, the new `SimpleChannel` network API, `Dist` instead of `Side`
and a completely different screen API (`AbstractContainerScreen` with
`AbstractWidget` buttons instead of `GuiContainer` with `GuiButton`).
The storage, slot and network logic port over almost unchanged, but the GUI
has to be written a second time. The scaffold is in place
(`versions/1.20.1/`), so the remaining work is the code port.
