# DreamHouse Glade source

The Glade (jMonkeyEngine) source project is in `glade`. From the repository root, run `CHOI-GAME.cmd` to launch the game after building it. To compile and package the game with JDK 21, run `Start-DreamHouse.ps1 -Rebuild` from this folder.

## Glade materials and assets

| Surface | Material keys | How the color is applied |
|---|---|---|
| Walls | `original`, `plaster`, `brick`, `stone`, `wood` | The selected color tints a generated grayscale texture. |
| Roofs | `original`, `tile`, `slate`, `wood`, `metal` | The host supplies the material and color. Edges and raised tiles use separate brightness values. |
| Roads | `dirt`, `concrete`, `brick`, `gravel` | The road stores width, material and optional RGB color in its save record. |

Store color as a six-digit RGB hex value or leave it empty to use the material default. Use the existing material keys in saves and let the engine generate the texture and mipmaps. Keep the roof surface function and clipping planes aligned with the rendered mesh so attached chimneys and joined roofs use the same geometry.

The Java sources generate the architecture, terrain meshes, material textures and in-game sound effects. The launcher includes `launcher-island.png` as a separate bitmap. Third-party Java libraries retain their bundled license notices in the portable distribution. Confirm the bitmap's provenance before a public release.
