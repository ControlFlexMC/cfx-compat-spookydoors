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
  so the bridge keeps its own virtual cursor, seeded from the real one at grab time. The delta
  carries the door's hinge (`DoorSide`), because the door mod flips a mouse delta for a left hinge;
  its other flip — player side — is deliberately left alone, and that is what makes push close the
  door from behind and pull open it.
* Before injecting a press, `DoorTargeting` re-checks every condition of the door mod's own grab
  check — the `spookydoors:doors` / `excluded_doors` tags, the door type, the `haunts_doors` /
  `exorcises_doors` items, and the per-door `spooky` flag (read by reflection). A press the door mod
  would decline falls through to vanilla use and would place a block, so it must never be sent.
* `DragRate` converts the input into mouse displacement. An analog binding (stick direction,
  trigger) integrates its deflection as speed. A digital one has no travel to read, so the trigger
  mode decides the step: a HOLD advances 5% of the travel per tick (about a second end to end),
  while a one-shot mode such as PRESS moves 15% on its single tick.
* Everything runs off the ControlFlex SPI plugin (`SpookyDoorsPlugin`), so nothing happens until
  ControlFlex reports it is ready.

## Actions

Registered as ControlFlex-native actions (no `KeyMapping`, nothing in the vanilla controls screen),
grouped under **Spooky Doors** — they are listed on that adaptation card and nowhere else:

| Action | Id |
|---|---|
| Grab Door | `cfx_compat_spookydoors:grab` |
| Push Door | `cfx_compat_spookydoors:push` |
| Pull Door | `cfx_compat_spookydoors:pull` |

## Bindings

The actions ship **unbound**: no registration default, and the bundled compat config carries no
bindings either. You bind them yourself in ControlFlex's binding screen.

A controller button can drive several actions at once, so **the layer you bind them in** is what
keeps the left stick from doing two things at the same time. Use one layer whose left stick is
otherwise free:

1. Pick a shift layer (`LB` = shift1, `RB` = shift2 by default) and clear that layer's
   `move_forward` / `move_backward` / `move_left` / `move_right` and `sprint` — they normally sit
   on the same four directions and on left-stick click.
2. In that same layer bind Grab Door → left stick click (hold), Push Door → left stick forward
   (hold), Pull Door → left stick back (hold).
3. Play: hold the layer button, look at a door, click to grab, push or pull, release to let go.

Releasing the stick lets go because HOLD ends the action and the bridge injects the matching mouse
release. Main works too if you accept the stick walking you at the same time — ControlFlex has no
per-button exclusivity, so the layer is the contract.

Push/pull magnitude comes from the bound axis, not from the action, so putting push on a trigger
gives it trigger travel automatically.

> This needs a ControlFlex build that latches a registered action by the mode that actually fired
> (core `4058359b5`+). Older builds derive that from the registration instead, so an action you
> bound as HOLD yourself would report `isActive()` for a single tick and the drag would end
> immediately.

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47+ / Fabric Loader 0.15.11+ (Fabric API) |
| ControlFlex | 0.9.0+ with API 0.9.2.4 and the fix that latches a registered action by the mode that fired (`4058359b5`+) — below that the bridge starts but registers nothing, or HOLD only lasts a tick |
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
