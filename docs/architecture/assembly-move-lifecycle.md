# Assembly Move Lifecycle

Universal joint links are held together by elastic forces applied each physics tick, not by a persisted Sable constraint. Persistent data contains only link references (see the compatibility contract).

## Assembly Movement

Sable invokes `BlockSubLevelAssemblyListener.beforeMove` before moving a block and `afterMove` while the old block still exists. Linked endpoints use this as one deterministic transaction:

1. `beforeMove` records the old reference and marks the old block entity as moving.
2. The old entity's `remove` consumes that marker, clears only its local runtime state, and never drops or clears the partner link.
3. `afterMove` resolves the moved endpoint, remaps its own reference, then updates the loaded partner to the new position and sub-level identity.

The temporary player-selection remap cache expires after 600 server game ticks. It intentionally uses game time, not wall-clock time, so a paused or overloaded server cannot turn a successful assembly move into an unintended unlink.
