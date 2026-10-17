# Create Aeronautics: Transmission & Linkage

Adds universal joints for linking Aeronautics structures and passing rotation across the link.

This is a trimmed fork of the original mod by enxv233. It keeps only the universal joint transmission:

- **Universal Joint Head** (`brass_universal_joint`)
- **Andesite Universal Joint Rod** (`universal_joint_rod2`): long link, up to 15 blocks
- **Brass Universal Joint Rod** (`universal_joint_rod`): short elastic link

The legacy universal joint head, all pneumatic/hydraulic content, and the kinetic conversion bearing were removed.

## Config

Settings are in `config/aeronautics_utility_objects-common.toml`. The file is created on first launch, applies to
every world, and is reloaded when you save it. Under `[universal_joint]`:

- `stress_impact`: stress cost of each joint head, in SU per RPM (default 2.0, 0 disables it). A linked pair counts
  both heads.
- `[universal_joint.andesite]` / `[universal_joint.brass]`: link range, stretch limits, and pull stiffness/damping.

## Requirements

A. Minecraft 1.21.1  
B. NeoForge 21.1.226 or newer in the 21.1 line  
C. Create 6.0.10 or newer  
D. Sable 2.0.0 or newer  
E. Simulated 1.3.0 or newer

The mod targets Sable 2.x only. It does not include a Sable 1.x compatibility layer.

## Build and verification

GitHub Actions (`.github/workflows/build.yml`) builds the mod on every push and pull request, and publishes the
jar as a release `v<mod_version>` on pushes to `main` or `v*` tags.

Use JDK 21.

```powershell
.\gradlew.bat build --offline --no-daemon --console=plain
```

`check` also runs a source-level compatibility regression check. The adjacent
`aeronautics_structure_tool` project is intentionally not coupled to implementation classes; verify its
compatibility with:

```powershell
.\gradlew.bat compileJava --offline --no-daemon --console=plain
```

Run that command from the `aeronautics_structure_tool` project directory.

## Compatibility

Blueprint and tool integrations depend on registry ids and persistent link references, not internal Java
classes. The maintained contract is documented in
[docs/architecture/compatibility-contract.md](docs/architecture/compatibility-contract.md).

## Architecture

A full walkthrough of how the mod works, with flow diagrams, is in [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md).

The assembly-move lifecycle is documented in
[docs/architecture/assembly-move-lifecycle.md](docs/architecture/assembly-move-lifecycle.md).
Ownership boundaries for the main runtime components are documented in
[docs/architecture/component-ownership.md](docs/architecture/component-ownership.md).

## License

CC BY-NC 4.0. See [LICENSE](LICENSE). Original work by enxv233; this fork removes content and changes the config as described above.
