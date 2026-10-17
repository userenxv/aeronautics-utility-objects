# Component Ownership

`UniversalJointBlockEntity` is the lifecycle coordinator. Its collaborators own rules that do not need a world or a
block-entity synchronization call.

| Component | Owns | Does not own |
| --- | --- | --- |
| `UniversalJointBlockEntity` | Link references, NBT keys, elastic link forces, rotation transmission across the link | Config values |
| `JointVariant` | Andesite vs. brass link ranges (read from config) | Link state |
| `AeroUniversalJointConfig` | Common config spec, including the joint head stress impact | Runtime state |
| `SubLevelMoveHandler` | Deterministic before/after assembly move handoff | Link force ownership |
| `RecentMoveRemapper` | Short-lived player-selection remaps using server game time | Persistent link data |
| `PendingRodSelections` | The first head a player clicked with a rod | Link creation rules |
