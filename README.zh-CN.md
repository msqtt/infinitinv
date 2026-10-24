# 无限背包（`infinitinv`）

[English](README.md) | 中文

一个 Minecraft **Java 版**模组：把原版背包的 27 格主背包换成**容量无上限**、
可**搜索**、可**自动排序**、可**分页浏览**的物品仓库。

装备栏、副手、快捷栏和合成区**完全没动** —— 外观和手感与原版一模一样。

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-c78b25)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860-1e6e2d)
![Java](https://img.shields.io/badge/Java-8-5382a4)
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

1. 装 **Forge 14.23.5.2860** 或更新版本（Minecraft 1.12.2）。
2. 把 `infinitinv-1.12.2.jar` 丢进 `mods/` 文件夹。
3. 启动游戏。快捷栏和装备栏照常使用。

## 从源码构建

项目用 [Gradle](https://gradle.org) 并带了 wrapper，本地不需要装 Gradle：

    ./gradlew :1.12.2:build

产物在 `versions/1.12.2/build/libs/1.12.2.jar`。

需要：一个跑 Gradle 的 JDK（17+），以及作为 1.12.2 编译工具链的 **JDK 8**。
Gradle 通过 `gradle.properties` 里的 `org.gradle.java.installations.paths` 找它；
不在 NixOS 上的话把它指到你自己的 JDK 8：

    ./gradlew :1.12.2:build \
      -Dorg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk17

在 NixOS 上整套工具链由系统配置提供：

```nix
users.users.bob.packages = with pkgs; [ openjdk8 openjdk17 gradle ];
```

## 多版本

项目用 [Stonecutter](https://github.com/kikugie/stonecutter) +
[Unimined](https://github.com/unimined/Unimined)，多个 Minecraft 版本可以共用一套
源码。目前 **1.12.2** 已完整移植并默认构建；新增版本要改什么、各版本 API 的差异
对照表见 [PORTING.md](PORTING.md)。

`gradle.properties` 里的 `readyVersions=1.12.2` 决定哪些版本参与
`./gradlew build`；其余版本目录仍保留在仓库里，方便继续推进移植。

## 目录结构

```
src/main/java/com/infinitinv/
  InfiniteInv.java              @Mod 入口
  storage/ExtendedStorage.java  无界仓库：过滤、排序、分页
  storage/StorageIO.java        NBT 序列化 / 反序列化
  storage/IExtendedStorageHolder.java
  slot/ExtendedSlot.java        挂在仓库上的原版 Slot
  mixin/PlayerMixin.java        挂载 + 持久化仓库
  mixin/ContainerPlayerMixin.java   替换 27 格主背包
  mixin/GuiInventoryMixin.java   页码指示 + 翻页按钮
  network/                      全量快照 + 动作消息
  client/ClientHooks.java       按键绑定
  client/gui/GuiInfiniteInventory.java  仓库界面
versions/                       各版本的 Gradle 属性与依赖
```

## 实现要点

* `ContainerPlayerMixin` 注入在 `ContainerPlayer` **构造函数**末尾（1.12.2 是在
  构造函数里建槽位的），把背包索引落在 `9..35` 的槽位换成 `ExtendedSlot`。
  用索引判断是有意为之：它与原版加槽位的顺序无关，护甲/合成/快捷栏的槽位
  绝不会被误伤。
* `ExtendedSlot` 是标准的原版 `Slot`，只重写了内容读写。所以拖拽、Shift 移动、
  快捷键换位、鼠标指针上的物品堆全都照常工作。
* 服务端持有权威数据。`SyncStorageMessage` 把完整 NBT 快照（物品、过滤条件、
  页码、搜索词、排序方式）发给客户端；`ActionMessage` 把「动作 + 绝对索引」送回服务端。
* Unimined 会在 `remapJar` 阶段自己重混淆 mixin 目标并生成 refmap，所以**故意没有**
  使用 Mixin 自带的注解处理器。

## 许可

MIT —— 见 [LICENSE](LICENSE)。
