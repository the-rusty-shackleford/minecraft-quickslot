# Client placement resources

Add a resource-pack file at
`assets/quickslot/quickslot_placement/minecraft/diamond_sword.json`, then reload with F3+T.
Replace `minecraft/diamond_sword` with the item's namespace/path. These resources affect
appearance only, and have no effect on server admission, item storage or lighting.

Complete example:

```json
{
  "anchor": "back",
  "offset": [0.0, 0.0, 0.0],
  "rotation": [0.0, 0.0, 0.0],
  "scale": 1.0,
  "with_backpack": {
    "offset": [0.0, 0.0, 0.0],
    "rotation": [0.0, 0.0, 0.0],
    "scale": 1.0
  }
}
```

`anchor` is required: `back`, `hip`, `lower_back`, or `hidden`. Other fields are optional.
For consumables, `style` can be `bottle`, `pouch`, `bowl`, or `hang`; `tool` removes that
consumable holder. Missing style keeps the detected/tagged style. `bowl` means hidden:
recognized bowl meals always stay in the pocket verse, even when an override asks for a
visible anchor or another style. Elytra hides every display, including overridden items.
Apart from those rules, a placement override outranks ordinary render tags.

Offsets use blocks in the chosen anchor's local axes; rotations use local Euler XYZ degrees.
Scale multiplies the normalized fit, with back items still capped by maximumBackLength.
There are no automatic armor/head offsets. Legacy `armor_offset_multiplier` is ignored.
with_backpack is an additional
transform for a visibly worn compatible pack; it is parsed now and wired in the backpack phase.
Malformed entries log a warning and fall back to ordinary classification. Reload replaces
the full resource-derived map, so deleting an override also takes effect immediately.

The client TOML supplies global scales and back-side preference. Manual offsets remain
available for unusual item models; default placement follows the approved earlier model
checks and accepts minor hair/helmet clipping. All settings remain development-only until release.
