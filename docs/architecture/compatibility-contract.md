# Compatibility Contract

The following values are externally consumed and must remain stable across internal refactors.

## Registry identifiers

- Block / item: `aeronautics_utility_objects:brass_universal_joint` (the Universal Joint Head)
- Items: `aeronautics_utility_objects:universal_joint_rod` (brass rod), `aeronautics_utility_objects:universal_joint_rod2` (andesite rod)
- Block entity: `aeronautics_utility_objects:universal_joint`
- Menu: `aeronautics_utility_objects:universal_joint`

`create_aeronautics_toolgun` identifies these block entities by registry id when saving and placing blueprints.

The legacy `aeronautics_utility_objects:universal_joint` block, the pneumatic/hydraulic blocks and items, and the
kinetic conversion bearing were removed. Worlds that contain them will report them as missing.

## Persistent link format

Linked universal joints persist their partner through these NBT keys:

- `LinkedPos`: partner position, encoded as a normal NBT block position.
- `LinkedSubLevel`: optional partner sub-level UUID.

The keys and their coordinate meaning are part of the blueprint compatibility contract. Internal state may be
reorganized, but a loaded legacy tag must retain the same effective link.

## Config

Settings live in the common config file `config/aeronautics_utility_objects-common.toml`, shared by every world.
`universal_joint.stress_impact` is registered with Create's `BlockStressValues.IMPACTS` for the joint head.

## Network and player-facing identifiers

Keep the existing menu ids, payload ids, translation keys, item ids, and block ids. Changes to implementation
packages are allowed when public entry points and serialized data remain compatible.
