# cfx-compat-spookydoors

Spooky Doors controller bridge for [ControlFlex](https://www.curseforge.com/minecraft/mc-mods/control-flex) —
grab a door with the left stick and push or pull it, instead of wiggling the mouse.

[中文文档](README_ZH.md)

Supports **Forge** and **Fabric** on Minecraft **1.20.1**.

## Why a separate mod

Spooky Doors exposes no API for "operate this door". It listens on `MouseHandler.onPress` and
`MouseHandler.onMove` and integrates the horizontal displacement itself, so the only way in without
touching its code is to replay what a real mouse would send: press use, accumulate displacement while
held, release. That is exactly what this bridge does — the door mod keeps ownership of its own state,
sound and server sync.

Keeping that in a compat mod instead of ControlFlex core means the core stays free of third-party
dependencies, and a failure here is contained instead of affecting every player.

## How it works

* `MouseHandlerAccessor` (a mixin accessor/invoker on `MouseHandler`) reaches the same private
  `onPress` / `onMove` calls Spooky Doors mixes into. `onMove` takes **absolute** cursor coordinates,
  so the bridge keeps its own virtual cursor, seeded from the real one at grab time.
* Before injecting a press, `DoorTargeting` re-checks every condition of the door mod's own grab
  check — the `spookydoors:doors` / `excluded_doors` tags, the door type, the `haunts_doors` /
  `exorcises_doors` items, and the per-door `spooky` flag (read by reflection). A press the door mod
  would decline falls through to vanilla use and would place a block, so it must never be sent.
* `DragRate` converts stick travel into mouse displacement. An analog binding (stick direction,
  trigger) integrates its deflection as speed; a digital one (face button, key) moves the door a
  fixed eighth of its travel per press, because a pressed button has no travel to read.
* Everything runs off the ControlFlex SPI plugin (`SpookyDoorsPlugin`), so nothing happens until
  ControlFlex reports it is ready.

## Actions

Registered under the Spooky Doors adaptation card, all in the in-game layer:

| Action | Id | Default |
|---|---|---|
| Grab Door | `cfx_compat_spookydoors:grab` | left stick click, hold |
| Push Door | `cfx_compat_spookydoors:push` | left stick up, hold |
| Pull Door | `cfx_compat_spookydoors:pull` | left stick down, hold |

Push/pull magnitude comes from the bound axis, not from the action, so moving push onto a trigger
gives it trigger travel automatically.

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47+ / Fabric Loader 0.15.11+ (Fabric API) |
| ControlFlex | 0.9.0+ — below 0.9.3.0 the bridge starts but stays disabled (no analog magnitudes) |
| Spooky Doors | 1.20+ — **optional**, the actions simply never target a door without it |

## Project layout

```
common/     shared bridge: door logic, ControlFlex plugin, mixin — identical on both loaders
forge/      @Mod entry, mods.toml, pack.mcmeta
fabric/     ClientModInitializer entry, fabric.mod.json
tools/      per-loader build + deploy scripts
```

Only the entry point and the mod metadata differ per loader; the door itself is driven by the same
code in both jars.

## Building

```bash
./gradlew :forge:build     # jar in forge/build/libs/
./gradlew :fabric:build    # jar in fabric/build/libs/
./gradlew test             # shared DragRate tests, run on both loaders

tools/build-forge.sh       # build + deploy Forge jar to $MC_1_20_1_FORGE_PATH
tools/build-fabric.sh      # build + deploy Fabric jar to $MC_1_20_1_FABRIC_PATH
```

`--release` adds a copy to `$RELEASE_DEPLOY_PATH`; `--clean` runs `clean` first.
