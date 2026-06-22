Two fixes to how trees come down.

**Leaf clearing now follows vanilla decay.** With leaf breaking on, BlackTimber used to scan a fixed cube around each log. That left the outer one or two layers of a wide canopy behind and, in a forest, ate the leaves of a neighbouring tree that happened to fall inside the cube. Clearing now reproduces vanilla decay exactly: it removes only the leaves the felled tree was supporting and that no surviving log still keeps within range. The whole canopy comes down, outer layers included, on a tree of any size or shape, while an adjacent tree whose trunk still stands keeps every one of its own leaves, even where two canopies overlap. Player placed (persistent) leaves are still never touched.

**Felling is now bounded by the axe's durability.** An axe could get stuck at 1 durability and fell trees forever without ever breaking. Felling is now limited to the logs the axe can pay for, with Unbreaking honoured per log, so it can never outrun the tool. A worn axe fells the part it can afford and leaves the rest of the tree standing.

**`break-tool` now defaults to true.** The axe wears down and breaks like vanilla once it runs out mid-fell. Set it to false to protect the axe instead: it then stops at 1 durability and leaves the rest of the tree standing rather than breaking.

**Config.**
- New `leaf-clear-budget` (default `8192`): safety ceiling on how many leaves a single fell may examine before deferring cleanup to vanilla decay.
- `leaf-search-radius` is now detection only and no longer limits clearing.
- `break-tool` default changed from `false` to `true`.
