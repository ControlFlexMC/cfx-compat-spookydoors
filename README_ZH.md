# cfx-compat-spookydoors

[ControlFlex](https://www.curseforge.com/minecraft/mc-mods/control-flex) 的 Spooky Doors 手柄桥：
用左摇杆抓住门，推或拉，而不是左右晃鼠标。

[English](README.md)

Minecraft **1.20.1**，同时支持 **Forge** 与 **Fabric**。

## 为什么要单独做一个桥

Spooky Doors 没有开放"操作这扇门"的 API。它自己监听 `MouseHandler.onPress` 与
`MouseHandler.onMove` 并对水平位移做积分，所以不碰它代码的唯一入口，就是重放真实鼠标会发出的
调用：按下使用键、按住期间累加位移、松手。本桥做的正是这件事，门的状态、音效与服务端同步仍由
门模组自己负责。

把这段逻辑留在兼容模组而不是塞进 ControlFlex 核心，核心才能不依赖任何第三方模组；这里出问题也
只影响本模组，不会波及所有玩家。

## 工作方式

* `MouseHandlerAccessor`（对 `MouseHandler` 的 mixin accessor/invoker）直达 Spooky Doors 注入的
  私有 `onPress` / `onMove`。`onMove` 收的是**绝对**光标坐标，因此桥自己维护一个虚拟光标，抓门
  那一刻用真实坐标做种子，第一次移动才不会跳。
* 注入按下之前，`DoorTargeting` 会把门模组自己的抓取条件重新核一遍——`spookydoors:doors` /
  `excluded_doors` 标签、门类型、`haunts_doors` / `exorcises_doors` 物品，以及每扇门的 `spooky`
  标记（反射读取）。被拒的按下会落到原版使用逻辑上，变成放方块，所以这种按下绝不能发。
* `DragRate` 把摇杆行程换算成鼠标位移：模拟输入（摇杆方向、扳机）按速度积分按下量；数字输入
  （面板键、键盘）每按一次固定走八分之一行程，因为它没有行程可积分。
* 全部逻辑挂在 ControlFlex 的 SPI 插件（`SpookyDoorsPlugin`）上，ControlFlex 没就绪就什么都不做。

## 动作

注册在 Spooky Doors 的适配卡下，均属游戏内层：

| 动作 | Id | 默认绑定 |
|---|---|---|
| 抓住门 | `cfx_compat_spookydoors:grab` | 左摇杆按下，按住 |
| 推门 | `cfx_compat_spookydoors:push` | 左摇杆上，按住 |
| 拉门 | `cfx_compat_spookydoors:pull` | 左摇杆下，按住 |

推/拉的幅度取自绑定轴本身而不是动作，所以把推门改绑到扳机，它自然就有扳机行程。

## 依赖

| | |
|---|---|
| Minecraft | 1.20.1 |
| 加载器 | Forge 47+ / Fabric Loader 0.15.11+（需 Fabric API） |
| ControlFlex | 0.9.0+——低于 0.9.3.0 时桥能启动但自动禁用（读不到模拟量） |
| Spooky Doors | 1.20+——**可选**，没有它动作只是永远瞄不到门 |

## 工程结构

```
common/     共享桥：门的逻辑、ControlFlex 插件、mixin——两个加载器完全一致
forge/      @Mod 入口、mods.toml、pack.mcmeta
fabric/     ClientModInitializer 入口、fabric.mod.json
tools/      各加载器的构建与部署脚本
```

每个加载器只有入口和模组元数据不同，驱动门的代码在两边是同一份。

## 构建

```bash
./gradlew :forge:build     # 产物在 forge/build/libs/
./gradlew :fabric:build    # 产物在 fabric/build/libs/
./gradlew test             # 共享的 DragRate 测试，两个加载器各跑一遍

tools/build-forge.sh       # 构建并把 Forge 包部署到 $MC_1_20_1_FORGE_PATH
tools/build-fabric.sh      # 构建并把 Fabric 包部署到 $MC_1_20_1_FABRIC_PATH
```

`--release` 会额外复制一份到 `$RELEASE_DEPLOY_PATH`；`--clean` 先执行 `clean`。
