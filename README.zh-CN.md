# 无限背包（`infinitinv`）

[English](README.md) | 中文

一个 Minecraft **Java 版**模组：把原版背包的 27 格主背包换成**容量无上限**、
可**搜索**、可**自动排序**、可**分页浏览**的物品仓库。

装备栏、副手、快捷栏和合成区**完全没动** —— 外观和手感与原版一模一样。

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-c78b25)
![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-c78b25)
![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-c78b25)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860%20%2F%2047.3.0%20%2F%2052.0.28-1e6e2d)
![Java](https://img.shields.io/badge/Java-8%20%2F%2017%20%2F%2021-5382a4)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 功能

* **容量无上限。** 仓库就是一个没有上限的列表，保存在玩家 NBT 里，登出、死亡、
  重启都不丢。模组第一次见到某个玩家时，会把原本 27 格里的东西自动搬进仓库，
  不会有物品被落下。
* **搜索。** 在搜索框里输入，可按注册名（`minecraft:diamond_sword`）或物品名过滤。
* **自动排序。** `ID` / `Name` / `Type`（物品类别）三种模式，一个按钮循环切换。
* **分页浏览。** 原版背包一页 27 格；独立界面一页 45 格，两边都能翻页。
* **浏览游戏里的所有物品。** `All items` 模式会列出游戏注册的**每一个**物品，
  不只是你手里有的。
* **服务端权威。** 数据在服务端，客户端只保留镜像。所有修改都带**绝对索引**
  上行，服务端不需要知道你正在看第几页。

## 操作

| 动作 | 怎么做 |
|---|---|
| 打开无限背包界面 | `K`（可在按键设置里改） |
| 在原版背包里浏览仓库 | 按 `E` —— 那 27 格就是仓库 |
| 原版背包里翻页 | 护甲栏旁边的 `<` / `>` 按钮 |
| 从仓库里取出物品 | 在无限背包界面里点它 |
| 把物品放进仓库 | 切到 `All items` 模式，然后点它 |

## 安装


| Minecraft | Forge | 文件 |
|---|---|---|
| 1.12.2 | 14.23.5.2860 | `infinitinv-1.12.2.jar` |
| 1.20.1 | 47.3.0 | `infinitinv-1.20.1.jar` |
| 1.21.1 | 52.0.28 | `infinitinv-1.21.1.jar` |

1. 装对应版本的 Forge。
2. 把对应的 `infinitinv-<mc版本>.jar` 丢进 `mods/` 文件夹。
3. 启动游戏。快捷栏和装备栏照常使用。

## 从源码构建


项目用 [Gradle](https://gradle.org) 并带了 wrapper，本地不需要装 Gradle：

    ./gradlew :1.12.2:build
    ./gradlew :1.20.1:build
    ./gradlew :1.21.1:build

产物在 `versions/<mc版本>/build/libs/<mc版本>.jar`。

需要：一个跑 Gradle 的 JDK（17+），以及分别作为 1.12.2 / 1.20.1 / 1.21.1
编译工具链的 **JDK 8**、**JDK 17**、**JDK 21**。Gradle 通过
`gradle.properties` 里的 `org.gradle.java.installations.paths` 找它们；
不在 NixOS 上就自己指路径：

    ./gradlew :1.21.1:build \
      -Dorg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk17,/path/to/jdk21

NixOS 上整套工具链由系统配置提供：

```nix
users.users.bob.packages = with pkgs; [ openjdk8 openjdk17 openjdk21 gradle ];
```

## 多版本


项目用 [Stonecutter](https://github.com/kikugie/stonecutter) 和
[Unimined](https://github.com/unimined/Unimined)，一套源码树支持多个
Minecraft 版本。`1.12.2`、`1.20.1`、`1.21.1` 均已移植并默认构建；新增版本
需要改什么、以及各版本族之间的 API 差异，见 [PORTING.md](PORTING.md)。

`gradle.properties` 里 `readyVersions=1.12.2,1.20.1,1.21.1` ——
`./gradlew build` 只编译列在里面的版本，其它版本目录仍保留在仓库里，方便
继续做移植。

1.12.2 的实现放在 `versions/1.12.2/src/main/java`，1.20.1 在
`versions/1.20.1/src/main/java`，1.21.1 在 `versions/1.21.1/src/main/java`。
两个现代版本设计完全一致，但 API 调用各自独立 —— Forge 的网络层在 1.21
被整体换掉了。

## 目录结构


```
versions/1.12.2/src/main/java/com/infinitinv/
  InfiniteInv.java              @Mod 入口
  storage/ExtendedStorage.java  无上限存储：过滤、排序、分页
  storage/StorageIO.java        NBT 序列化
  slot/ExtendedSlot.java        挂在存储上的原版 Slot
  mixin/PlayerMixin.java        挂载与持久化
  mixin/ContainerPlayerMixin.java   替换 27 格主背包
  mixin/GuiInventoryMixin.java   页码指示与翻页按钮
  network/                      快照同步 + 动作消息
  client/ClientHooks.java       按键绑定
  client/gui/GuiInfiniteInventory.java  独立背包界面
versions/1.20.1/src/main/java/com/infinitinv/   同样设计，现代 API
versions/1.21.1/src/main/java/com/infinitinv/   同样设计，1.21 API
versions/<mc版本>/              各版本的 Gradle 属性与依赖
```

## 实现要点


* `ContainerPlayerMixin` 在 `ContainerPlayer` **构造函数**末尾注入（1.12.2
  在那里创建槽位），把 inventory index 落在 `9..35` 的槽位换成
  `ExtendedSlot`。用 index 判断是有意为之：它与原版添加槽位的顺序无关，
  装备栏、合成区和快捷栏不可能被误伤。
* `ExtendedSlot` 就是普通的原版 `Slot`，只重写内容读写。拖拽、Shift 点击、
  快捷栏互换、光标堆栈的行为因此完全不变。
* 服务端掌握数据。`SyncStorageMessage` 把完整 NBT 快照（物品、过滤、页码、
  搜索词、排序）发给客户端，`ActionMessage` 带回一次修改和**绝对**索引。
* Unimined 在 `remapJar` 阶段自己重混淆 mixin 目标并生成 refmap，所以
  刻意不用原版 Mixin 注解处理器。
* 1.20.1 用 `net.minecraft.world.item.ItemStack`、
  `net.minecraft.nbt.CompoundTag` 和 Forge 47 的 `SimpleChannel`；1.21.1 改用
  数据组件序列化（`ItemStack.save(provider, tag)`）、`ChannelBuilder` 和
  基于 `CustomPayloadEvent` 的网络层 —— 1.21 里旧的 `IMessage` 处理器已被
  payload API 取代。

## 许可

MIT —— 见 [LICENSE](LICENSE)。
