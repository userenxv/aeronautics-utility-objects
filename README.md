# Create Aeronautics: Transmission & Linkage

Adds universal joints, hydraulic links, and kinetic conversion bearings for Aeronautics structures.

## Requirements

A. Minecraft 1.21.1  
B. NeoForge 21.1.226 or newer in the 21.1 line  
C. Create 6.0.10 or newer  
D. Sable 2.0.0 or newer  
E. Simulated 1.3.0 or newer

The mod targets Sable 2.x only. It does not include a Sable 1.x compatibility layer.

## Build and verification

Use JDK 21.

```powershell
.\gradlew.bat build --no-daemon --console=plain
```

`check` also runs compatibility, physics-control, and hydraulic rod picking regression checks.
The first build needs network access to resolve NeoForge, mappings, Sable, and Simulated.
Use `--offline` only after those dependencies have been cached.

The `libs/` directory contains the Create, Flywheel, Ponder, Registrate, and Sable companion
dependencies used by this build. Sable 2.x and Simulated 1.3.x are resolved from Maven;
obsolete Sable 1.x packages and extracted dependency resources are not part of the project.

Keep machine-specific proxy settings in your user-level Gradle configuration, not in this
repository. IDE launch configurations, game instances, logs, build output, and local reports
should remain untracked.

## Compatibility

Blueprint and tool integrations depend on registry ids and persistent link references, not internal Java
classes. The maintained contract is documented in
[docs/architecture/compatibility-contract.md](docs/architecture/compatibility-contract.md).

## Architecture

The Sable constraint and assembly-move lifecycle is documented in
[docs/architecture/sable-constraint-lifecycle.md](docs/architecture/sable-constraint-lifecycle.md).
Ownership boundaries for the main runtime components are documented in
[docs/architecture/component-ownership.md](docs/architecture/component-ownership.md).

## License

CC BY-NC 4.0. See [LICENSE](LICENSE).
