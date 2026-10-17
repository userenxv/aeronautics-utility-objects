# How Transmission & Linkage Works

An in-depth walkthrough of the trimmed mod: what each piece does, how a link is made, how rotation and stress
cross it, how the physics holds two structures together, and what happens when structures move or break.

Class names refer to `src/main/java/com/enxv/aerouniversaljoint/`.

---

## Contents

1. [The big picture](#1-the-big-picture)
2. [Blocks, items and recipes](#2-blocks-items-and-recipes)
3. [Code map](#3-code-map)
4. [Startup: what happens when the mod loads](#4-startup-what-happens-when-the-mod-loads)
5. [Making a link](#5-making-a-link)
6. [What a link is: the saved data](#6-what-a-link-is-the-saved-data)
7. [Rotation transmission](#7-rotation-transmission)
8. [Stress cost](#8-stress-cost)
9. [Physics: holding structures together](#9-physics-holding-structures-together)
10. [Strain warning colours](#10-strain-warning-colours)
11. [The tick loop: keeping a link healthy](#11-the-tick-loop-keeping-a-link-healthy)
12. [When a structure is assembled or moved](#12-when-a-structure-is-assembled-or-moved)
13. [Breaking a link](#13-breaking-a-link)
14. [Rendering](#14-rendering)
15. [Config reference](#15-config-reference)
16. [Mixins](#16-mixins)
17. [Quirks and limitations](#17-quirks-and-limitations)

---

## 1. The big picture

Create Aeronautics (built on the Sable physics engine) turns groups of blocks into moving physical structures
called **sub-levels**. A normal Create shaft can't reach across from one sub-level to another, or from a
sub-level to the world, because they are separate block spaces.

This mod adds a **Universal Joint Head**. Put one head on each side, right-click both with a **joint rod**, and
the two heads become a linked pair that:

- passes **rotation** from one side's kinetic network to the other's, and
- applies **elastic forces** that pull (and, for andesite, push) the two structures toward the length they were
  linked at.

```
      Structure A (sub-level)                         Structure B (sub-level or the world)
   +---------------------------+                    +---------------------------+
   |                           |                    |                           |
   |  motor ==shaft==[HEAD A]o-+==== joint rod =====+-o[HEAD B]==shaft== fan    |
   |                           |   rotation  ---->  |                           |
   |                           |   <--- pull --->   |                           |
   +---------------------------+                    +---------------------------+
```

---

## 2. Blocks, items and recipes

| Thing | Registry id | Role |
| --- | --- | --- |
| Universal Joint Head | `aeronautics_utility_objects:brass_universal_joint` | The block on each end. A Create kinetic block with a shaft on both ends of its facing axis. |
| Andesite Universal Joint Rod | `aeronautics_utility_objects:universal_joint_rod2` | Makes a **long, rest-length** link (up to 15 blocks). |
| Brass Universal Joint Rod | `aeronautics_utility_objects:universal_joint_rod` | Makes a **short, elastic leash** link (up to 5 blocks). |

The head does not decide the link type. **The rod you link with does.** Both kinds of link use the same head.

```
  Universal Joint Head           Brass Universal Joint Rod      Andesite Universal Joint Rod
  +-------+-------+-------+      +-------+-------+-------+      +-------+-------+-------+
  |       | shaft |       |      |       | shaft |       |      |       | shaft |       |
  +-------+-------+-------+      +-------+-------+-------+      +-------+-------+-------+
  | dried | prec. | dried |      | brass |spring | brass |      | andes.|spring | andes.|
  | kelp  | mech. | kelp  |      | sheet |(Sim.) | sheet |      | alloy |(Sim.) | alloy |
  +-------+-------+-------+      +-------+-------+-------+      +-------+-------+-------+
  |       | brass |       |      |       | shaft |       |      |       | shaft |       |
  |       |casing |       |      |       |       |       |      |       |       |       |
  +-------+-------+-------+      +-------+-------+-------+      +-------+-------+-------+
```

The rod is used up when a link is made (not in creative), and dropped back when the link breaks.

---

## 3. Code map

```
AeroUniversalJointMod ............ entry point: registers everything, config, stress value
 |
 +-- ModBlocks / ModItems / ModBlockEntities / ModMenuTypes / ModCreativeTabs
 |
 +-- AeroUniversalJointConfig ..... common config file (stress_impact, ranges, stiffness)
 |
 +-- AeroUniversalJointCommonEvents  per-tick cleanup, clears selections on logout/respawn/dimension change
 |
 +-- network/
 |     AeroUniversalJointNetwork, SetJointSpeedRatioPayload   (speed ratio is disabled, see section 17)
 |
 +-- content/
 |     UniversalJointBlock .......... placement, right-click handling, wrench, assembly-move hooks
 |     UniversalJointBlockEntity .... THE CORE: link data, transmission, physics, validation, strain
 |     UniversalJointRodItem ........ the two rods; sneak-click clears a pending selection
 |     JointVariant ................. ANDESITE / BRASS ranges (read from config)
 |     PendingRodSelections ......... "first head clicked" per player (server copy + client copy)
 |       PlayerSelectionStore
 |       JointBindingData.Selection  (dimension, pos, sub-level UUID)
 |     SubLevelMoveHandler .......... before/after-move handoff when Sable moves a block
 |     RecentMoveRemapper ........... old position -> new position lookup, kept 600 game ticks
 |     UniversalJointRenderer ....... draws the spinning head and the rod between heads
 |     LinkVisualEffects ............ strain colour (white -> red, or a green recovery flash)
 |     UniversalJointMenu, WrenchRemovalHelper, DirectionalConnectionShapes
 |     ponder/ ...................... the in-game Ponder scene for joints
 |
 +-- client/
 |     AeroUniversalJointClient ..... registers renderer, screen, preview, Ponder
 |     ConnectionPreviewRenderer .... the coloured "where will this link go" preview
 |     AeroUniversalJointPartials ... the partial models (head, sleeve, cores)
 |     UniversalJointScreen ......... speed ratio screen (unreachable while the feature is off)
 |     ToolgunCompatibilityNotice ... login hint about the Aeronautics Toolgun mod
 |
 +-- mixin/ + access/ .............. two small Create hooks (section 16)
 |
 +-- util/SubLevelReferenceHelper ... "find the block entity at pos X, possibly inside sub-level U"
```

---

## 4. Startup: what happens when the mod loads

```
 Game launch
     |
     v
 AeroUniversalJointMod(...)                         (mod constructor)
     |-- register blocks, items, block entities, menus, creative tab
     |-- register network payloads (protocol "3")
     |-- register NeoForge event listeners (selections, move-remap cleanup)
     |-- registerConfig(COMMON)  ---> config/aeronautics_utility_objects-common.toml
     |                                (created with defaults if missing)
     |-- on CLIENT only: renderer, screen, preview renderer, Ponder plugin
     v
 FMLCommonSetupEvent
     |
     +-- BlockStressValues.IMPACTS.register(head, () -> config.stress_impact)
              |
              +-- a *supplier*, not a number: Create asks it every time it recalculates
                  a network, so editing the file takes effect without a restart.
```

---

## 5. Making a link

### 5.1 The two clicks

A link takes two right-clicks with the **same kind of rod**. The first click only remembers the head; the
second click creates the link.

```
 Player right-clicks a head with a rod
              |
              v
   +----------------------+   yes   +----------------------------------+
   | sneaking AND a head  |-------->| clear selection                  |
   | already selected?    |         | "Link target cleared."           |
   +----------------------+         +----------------------------------+
              | no
              v
   +----------------------+   no    +----------------------------------+
   | a head already       |-------->| remember THIS head + rod type    |
   | selected?            |         | (dimension, pos, sub-level UUID) |
   +----------------------+         | "First joint head selected."     |
              | yes                 +----------------------------------+
              v
   +----------------------+   no    +----------------------------------+
   | same rod type as the |-------->| clear selection                  |
   | first click?         |         | "Link target cleared."           |
   +----------------------+         +----------------------------------+
              | yes
              v
   +----------------------+
   | first head still at  |  no --> look it up in RecentMoveRemapper
   | the stored position? |         (it may have been assembled/moved
   +----------------------+          in the meantime, section 12)
              |
              v
     linkToSelection(...)  --->  LinkResult (table below)
              |
              v
   result clears the selection?  -> clear it
   result consumes a rod?         -> take one rod (not in creative)
   show the result message
```

The selection is stored twice: a **server copy** (authoritative) and a **client copy** (only used to draw the
preview line, section 14.3). Both are keyed by player UUID and cleared on logout, respawn and dimension change.

### 5.2 Link checks

`createMutualLink(other, brassRod)` runs these checks in order:

```
 other == this?                       --> SELF             "Cannot link to itself."
 different dimension?                 --> WRONG_DIMENSION  "Both ends must be in the same dimension."
 distance > rod's link_range?         --> TOO_FAR          "Too far away to link."
 already linked to each other?        --> ALREADY_LINKED   "Link created."   (no rod used)
 otherwise:
   1. break any existing link on BOTH heads (each old partner is told too)
   2. A.linked = B,  B.linked = A          (position + sub-level UUID)
   3. both store: variant (rod type), rest length (= current distance)
   4. both store: are the two rotation axes pointing the same way? (section 7.3)
   5. both re-join their Create kinetic networks
                                      --> SUCCESS          "Link created."   (one rod used)
```

Distance is measured between head centres **in world space**, so it accounts for where each sub-level
currently is.

| Result | Clears selection | Uses a rod |
| --- | --- | --- |
| SUCCESS | yes | yes |
| ALREADY_LINKED | yes | no |
| TARGET_UNAVAILABLE | yes | no |
| WRONG_DIMENSION | no | no |
| TOO_FAR | no | no |
| SELF | no | no |

---

## 6. What a link is: the saved data

A link is **two block entities pointing at each other**. There is no separate link object.

```
   HEAD A (block entity)                          HEAD B (block entity)
  +------------------------------+              +------------------------------+
  | LinkedPos      = B's pos  ---+------------->|                              |
  | LinkedSubLevel = B's UUID    |              |                              |
  |                              |<-------------+--- LinkedPos      = A's pos   |
  |                              |              |    LinkedSubLevel = A's UUID |
  | LinkedVariant  = "andesite"  |   (same on   | LinkedVariant  = "andesite"  |
  | LinkedRestLength = 7.42      |    both)     | LinkedRestLength = 7.42      |
  | TransmissionAxisAligned=true |              | TransmissionAxisAligned=true |
  | LinkStrainEffect / Strained  |              | LinkStrainEffect / Strained  |
  +------------------------------+              +------------------------------+
```

| NBT key | Meaning |
| --- | --- |
| `LinkedPos` | Partner's block position. Inside a sub-level this is the sub-level's own block coordinates. |
| `LinkedSubLevel` | Partner's sub-level UUID. Absent when the partner is in the normal world. |
| `LinkedVariant` | `andesite` or `brass`: which rod made the link. |
| `LinkedRestLength` | Distance at the moment of linking. The andesite link pulls back toward this. |
| `TransmissionAxisAligned` | Whether rotation crosses unchanged (`true`) or reversed (`false`). |
| `LinkStrainEffect`, `LinkStrained` | Strain colour state, so it renders correctly after loading. |

`LinkedPos` and `LinkedSubLevel` are the stable part of the format: blueprint tools (the Aeronautics Toolgun)
rely on them.

A link only counts when it is **mutual**: `A.references(B) && B.references(A)`. Almost every code path checks
both directions, so a half-broken link (one side pointing at nothing) is treated as no link.

---

## 7. Rotation transmission

### 7.1 How it plugs into Create

Create builds kinetic networks by asking each block entity "who are your neighbours?" and "what speed do you
pass to them?". The joint head answers both questions so its partner, possibly blocks away on another
structure, counts as a neighbour.

```
                                Create's network walk
                                        |
                +-----------------------+------------------------+
                v                                                v
     addPropagationLocations()                         propagateRotationTo(target)
     "who are my neighbours?"                          "what speed multiplier to target?"
                |                                                |
     normal shaft neighbours                           target is my linked partner?
     + my partner's position,                              no  -> normal Create rules
       ONLY IF I am the source side                        yes -> I am source?  +1 or -1  (section 7.3)
                                                                  I am target?   0  (no backflow)
```

Only one side of a link is ever the **source**, so rotation flows one way across the link. Without that rule,
the two networks would each try to drive the other and Create would treat the loop as conflicting.

### 7.2 Which side is the source?

Chosen fresh each tick (`isTransmissionSourceFor`), first rule that decides wins:

```
 1. Does one head already get its rotation FROM the other?
       A.source == B      -->  B is the source
       B.source == A      -->  A is the source
                |
                v undecided
 2. Does exactly one head have rotation coming from somewhere else
    (a motor, another shaft) and actually spinning?
       only A has external input  -->  A is the source
       only B has external input  -->  B is the source
                |
                v undecided (both or neither have input)
 3. Keep the previous decision ("sticky"), so it doesn't flip back and forth.
                |
                v no previous decision
 4. Tie-break: the head with the smaller block position
    (then smaller sub-level UUID) is the source.
```

Every tick the server compares the answer with the last one. If it changed, both heads re-join their
networks so Create rebuilds the speeds. This is what lets you put the motor on **either** side.

```
   motor on A's side                         motor moved to B's side
   [A] ==source==> [B]        ---->          [A] <==source== [B]
        (network rebuilt when the decision flips)
```

### 7.3 Direction: same way or reversed?

At link time each head works out its rotation axis **in world space** (its facing axis, rotated by its
sub-level's current orientation) and the two are compared:

```
   axisA . axisB >= 0   -->  aligned   -->  multiplier +1  (same direction)
   axisA . axisB <  0   -->  opposed   -->  multiplier -1  (reversed)

        A: --->              B: --->            aligned, +1
        A: --->              B: <---            opposed, -1
```

The result is stored (`TransmissionAxisAligned`) and **kept for the life of the link**. If a structure later
turns upside down, the direction doesn't suddenly flip mid-flight. Re-link to recompute it.

The speed ratio is always **1:1** (section 17).

---

## 8. Stress cost

Each Universal Joint Head has a stress impact of `stress_impact` SU per RPM (default **2.0**), registered with
Create's `BlockStressValues.IMPACTS` at startup.

```
   Stress used by one head  =  stress_impact  x  |RPM|

   Example, default 2.0, at 64 RPM:
      one head            2.0 x 64 = 128 SU
      a linked pair       both heads are in the same network once linked
                          -> 2 x 128 = 256 SU
```

- Set it to `0` in the config file for no cost.
- Create's goggles and the item tooltip show the value automatically.
- The value is read live. Networks that already exist use the new value the next time Create recalculates
  them (for example when something in the network changes).
- It's a **common** config, so each side reads its own file. On a server, the server's file decides real stress;
  a client's goggles show the client's own value.

---

## 9. Physics: holding structures together

### 9.1 Who applies the force?

Sable calls `sable$physicsTick` on block entities that live inside a sub-level, once per physics step. Both
heads could get that call, but the force must be applied **once** per step, so one head is elected:

```
                 Where are the two heads?
                          |
     +--------------------+---------------------+---------------------+
     v                    v                     v                     v
 both in the world   one in a sub-level,   both in the SAME      in two different
                     other in the world    sub-level             sub-levels
     |                    |                     |                     |
 no physics tick     the sub-level head     no force (they can't  the head whose sub-level
 (nothing moves)     applies the force      pull on themselves)   UUID is smaller applies it
```

The elected head computes one impulse and applies it **equally and oppositely** to both bodies, at each head's
position. If the partner is in the world, only the sub-level gets pushed.

```
   body A  <---- -impulse ----[A]~~~~~~~~~~~~~~~~[B]---- +impulse ---->  body B
                     (applied at the head, so it can also make the body spin)
```

### 9.2 Andesite link: a spring around the rest length

The andesite link remembers its **rest length** (the distance when linked) and resists both stretching and
compressing beyond a dead zone.

```
   deviation = distance - restLength

   |deviation| <= soft_range (1.0)          no force (free play)
   |deviation| >  soft_range                force = pull_stiffness x n^2        (direction: back toward rest)
                                            n = (|deviation| - 1.0) / (2.0 - 1.0)
                                            + pull_damping x (relative speed along the link)
   |deviation| >  disconnect_range (2.5)    LINK BREAKS, rod drops


   force
     ^
     |                                         |
     |                                       * |
     |                                    *    |
     |                                *        |  break
     |                          *              |
     |                   *                     |
     |  dead zone  *                           |
     +-------------+---------------+-----------+------>  |deviation| (blocks)
     0            1.0             2.0         2.5
                  soft          n = 1        disconnect
```

Defaults: `pull_stiffness` 1320, `pull_damping` 18. The 2.0 point (where `n = 1`) is fixed in code; beyond it
the force keeps rising quadratically until the 2.5 break.

### 9.3 Brass link: a leash with a hard end

The brass link ignores rest length and measures the **absolute distance** between heads. It only ever **pulls**;
it never pushes the structures apart.

```
   distance <= soft_range (4.0)             slack: no force at all
   distance >  soft_range                   overshoot = distance - 4.0
                                            p = overshoot / (5.5 - 4.0)               (0..1)
                                            endpoint = 1 + (4 - 1) x p^2             (ramps up to x4 near the end)
                                            spring  = pull_stiffness x (e^(overshoot x pull_curve) - 1) x endpoint
                                            damping = separatingSpeed x (pull_damping + overshoot x pull_damping_gain)
   distance >  disconnect_range (5.5)       LINK BREAKS, rod drops


   force
     ^                                     |
     |                                    *|
     |                                   * |
     |                                 *   |   break
     |                              *      |
     |                         *           |
     |   slack (no force)  *               |
     +---------------------+---------------+------>  distance (blocks)
     0                    4.0             5.5
                         soft          disconnect
```

Defaults: `pull_stiffness` 12, `pull_curve` 4, `pull_damping` 7, `pull_damping_gain` 3,
`endpoint_pull_multiplier` 4. Only movement that separates the heads is damped (`separatingSpeed` is never
negative).

### 9.4 Andesite vs. brass at a glance

| | Andesite | Brass |
| --- | --- | --- |
| Link range (max distance to create) | 15 | 5 |
| Measures | change from rest length | absolute distance |
| Pushes when compressed | yes | no |
| Free play | +/- 1.0 around rest | 0 to 4.0 |
| Breaks at | +/- 2.5 from rest | 5.5 |
| Force curve | quadratic | exponential, steepening near the end |
| Feel | stiff rod | elastic leash |

---

## 10. Strain warning colours

The physics step also checks how close the link is to breaking, and the rod changes colour to warn you.

```
   measured = |distance - rest|   (andesite)
              distance            (brass)

   strained when  measured >= 0.85 x disconnect_range
                  (andesite: >= 2.125 off rest,   brass: >= 4.675 blocks)


                 strained                     no longer strained
   +-------+  ------------->  +--------+  ------------------->  +---------------+
   | white |                  |  RED   |                        | GREEN flash   |
   +-------+  <-------------  +--------+                        | holds 12 ticks|
       ^        (fades)                                         | then fades    |
       |                                                        | 10% per tick  |
       +--------------------------------------------------------+---------------+
```

Both heads are set at the same time. If Create's own **overstressed** effect is stronger at that moment, that
is shown instead.

---

## 11. The tick loop: keeping a link healthy

On the server, a linked head does three things on different schedules:

```
   every tick ........ check which side is the transmission source (section 7.2);
                       if it changed, rebuild both kinetic networks
   every 10 ticks .... light check: if the partner is loaded and still points back,
                       is the link stretched past disconnect_range? -> break (rod drops)
                       (a missing partner is left alone here; the 20-tick check handles it)
   every 20 ticks .... full check (lazyTick): same as above, plus
                       - if the partner is missing, try RecentMoveRemapper (it may have moved)
                       - if it is really gone or points elsewhere, clear this side
                         quietly (no rod drop: the partner side already handled it)

   physics step ...... forces + strain colour + break check (section 9), only on sub-levels
```

The 10/20-tick checks matter for links where neither end is in a sub-level that's being simulated, so the
physics step never runs. A link is only judged once the partner's chunk or sub-level is actually loaded
(`isReferenceReady`), so a partner that's merely unloaded doesn't cause an unlink.

Sable is also told the partner's sub-level is a **connection dependency** (`sable$getConnectionDependencies`),
so it loads and groups the two structures together.

---

## 12. When a structure is assembled or moved

When Sable assembles blocks into a sub-level, or moves them between sub-levels, the head block is **removed at
its old position and recreated at a new one**. Normally removing a head breaks its link and drops the rod, so
the move is wrapped in a careful handoff.

```
  Sable                     Head (old)            SubLevelMoveHandler        RecentMoveRemapper        Partner head
    |                           |                         |                          |                       |
    |-- beforeMove(old pos) --->|------------------------>|                          |                       |
    |                           |                         |-- prepare(old pos,      ->|                       |
    |                           |                         |   old sub-level)          |  (pending entry)      |
    |                           |<-- preserveLink... -----|                          |                       |
    |                           |   (flag: "I'm moving")   |                          |                       |
    |                           |                         |                          |                       |
    |-- remove old block ------>|                         |                          |                       |
    |                           |  sees the flag:         |                          |                       |
    |                           |  clears ONLY its own    |                          |                       |
    |                           |  data; partner untouched|                          |                       |
    |                           |  no rod dropped         |                          |                       |
    |                                                     |                          |                       |
    |-- create new block (Head (new)) -- copies NBT, so it still has LinkedPos etc.  |                       |
    |                                                     |                          |                       |
    |-- afterMove(old, new) ----------------------------->|                          |                       |
    |                                                     |-- record(old -> new) --->|                       |
    |                                                     |                          |  (kept 600 ticks)     |
    |                                         Head (new) <|-- remap my own reference |                       |
    |                                                     |   (partner may have moved too)                   |
    |                                                     |-- updateReferenceTo(new pos, new sub-level) ---->|
    |                                                     |                          |      partner now     |
    |                                                     |                          |      points at new   |
```

`RecentMoveRemapper` keeps `old position + old sub-level -> new position + new sub-level` entries for
**600 game ticks** (30 s). It's used by:

- the head itself, if its partner moved while it wasn't loaded,
- the 20-tick check, before giving up on a missing partner,
- a player's pending first click, so "click head, assemble structure, click other head" still works.

It counts **game time**, not real time, so a lagging or paused server can't expire an entry early.

---

## 13. Breaking a link

```
 Cause                                         Rod dropped?   Partner cleared?
 ------------------------------------------    ------------   ----------------
 Sneak + empty-hand right-click a head         yes            yes      "Link removed."
 Break a head (pickaxe, explosion...)          yes            yes
 Sneak + wrench a head (picks the block up)    yes            yes
 Stretched past disconnect_range               yes            yes
 Linking either head to something else         no             yes      (old link replaced)
 Partner gone / points elsewhere (20-tick)     no             -        (quiet cleanup)
 Head moved by Sable assembly                  no             no       (link carried over, section 12)
```

The rod dropped matches the link type: an andesite link drops an Andesite Universal Joint Rod, a brass link
drops a Brass one. It drops at the head that broke the link.

---

## 14. Rendering

### 14.1 The head

The head body is a normal block model; the rotating part is a Create **partial model**
(`brass_universal_jointhead`) spun with the head's kinetic speed, like a shaft.

### 14.2 The rod

Only **one** of the two heads draws the rod, so it isn't drawn twice: the one with the smaller block position
(then smaller sub-level UUID). It draws, in render space, using each sub-level's smoothed render pose:

```
   [HEAD A]                                                          [HEAD B]
      o====sleeve====|=================== core ====================|====sleeve====o
      ^              ^                     ^                       ^              ^
      start          core start         middle core piece          core end       end
                     (stretched to fit the gap)

   sleeve ........... fixed-length end piece at each head, rolled to match the head
   stretched core ... one model scaled along the link to fill the gap
   middle core ...... a detail piece in the middle (different model for andesite vs. brass)
   spin ............. the core spins with the head's rotation angle
   tint ............. strain colour from section 10
```

### 14.3 The link preview

While holding a rod with a head already selected, a preview line is drawn from the selected head to whatever
you're looking at:

```
   colour   meaning
   ------   -----------------------------------------------------------------
   GREEN    looking at a head, and the link would succeed
   YELLOW   not looking at a head, or slightly too far (within link_range + 0.25)
   RED      would fail (same head, wrong dimension, ...) or clearly out of range
```

---

## 15. Config reference

File: `config/aeronautics_utility_objects-common.toml` (created on first launch, shared by all worlds, reloaded
when saved).

```toml
[universal_joint]
    # SU per RPM for each joint head. 0 = no stress cost.
    stress_impact = 2.0

    [universal_joint.andesite]
        link_range = 15.0          # max distance to create the link
        soft_range = 1.0           # free play around rest length
        disconnect_range = 2.5     # break when this far from rest length
        pull_stiffness = 1320.0
        pull_damping = 18.0

    [universal_joint.brass]
        link_range = 5.0           # max distance to create the link
        soft_range = 4.0           # slack before it starts pulling
        disconnect_range = 5.5     # break at this distance
        pull_stiffness = 12.0
        pull_curve = 4.0           # how fast the pull grows (exponential)
        pull_damping = 7.0
        pull_damping_gain = 3.0    # extra damping per block of overshoot
        endpoint_pull_multiplier = 4.0
```

`disconnect_range` is always kept at least 0.01 above `soft_range`, even if the file says otherwise.

---

## 16. Mixins

Two small hooks into Create, both in `mixin/`:

| Mixin | Target | Why |
| --- | --- | --- |
| `KineticBlockEntityTickMixin` | `KineticBlockEntity.tick` | Skips the tick for a joint head that has no level (a detached copy, for example in a blueprint), so it doesn't try to tick without a world. |
| `KineticEffectHandlerMixin` | `KineticEffectHandler` | Same guard for the effect tick, and exposes Create's overstressed-effect value so the strain colour can defer to it (section 10). |

They only act on blocks that implement the marker interface `DetachedKineticSafetyGuard`, which in this mod is
just the joint head.

---

## 17. Quirks and limitations

- **Speed ratio is switched off.** `UniversalJointBlockEntity.isSpeedRatioFeatureEnabled()` returns `false`
  and `getSpeedRatio()` always returns `1.0`. The screen, menu and network payload still exist but can't be
  opened. Empty-hand right-click on a linked head therefore does nothing (sneak + empty hand still unlinks).
- **Direction is fixed at link time** (section 7.3). Re-link if a structure has been flipped and you want the
  direction recomputed.
- **No physics between two world blocks or within one sub-level** (section 9.1). In those cases the link still
  transmits rotation; it just doesn't apply forces.
- **The andesite force curve's full-force point is hard-coded** at 2.0 blocks off rest (section 9.2), not
  configurable.
- **Common config isn't synced to clients.** Goggles on a client show that client's `stress_impact`.
- **Leftovers from the full mod:** `JointBindingData.Selection` still carries two unused hydraulic flags, the
  `same_structure` message has no code path that shows it, and `LinkResult.MISMATCHED_VARIANT` is never
  returned (a mismatched rod just clears the selection). None of these affect behaviour.
- **Relinking loses the old rod.** Linking a head that is already linked to a different head replaces the old
  link without dropping the old link's rod (section 13).
- **Removed content:** worlds that contain the legacy head, pneumatic/hydraulic blocks or the kinetic
  conversion bearing will report those blocks as missing.
