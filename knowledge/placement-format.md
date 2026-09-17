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
  "armor_offset_multiplier": 1.0,
  "with_backpack": {
    "offset": [0.0, 0.0, 0.0],
    "rotation": [0.0, 0.0, 0.0],
    "scale": 1.0
  }
}
```

`anchor` is required: `back`, `hip`, `lower_back`, or `hidden`. Other fields are optional.
For consumables, `style` can be `bottle`, `pouch`, `bowl`, or `hang`; `tool` removes that
consumable holder. Missing style keeps the detected/tagged style. A placement override
outranks render tags, so it can explicitly display an otherwise hidden item.

Offsets use blocks in the chosen anchor's local axes; rotations use local Euler XYZ degrees.
Scale multiplies the normalized fit, with back items still capped by maximumBackLength.
Armor multiplier changes clearance from actual worn armor. with_backpack is an additional
transform for a visibly worn compatible pack; it is parsed now and wired in the backpack phase.
Malformed entries log a warning and fall back to ordinary classification. Reload replaces
the full resource-derived map, so deleting an override also takes effect immediately.

The client TOML supplies global scales and armor offsets. Tag bulky armor with
`quickslot:bulky_armor`, or set `armorItemOffsets` entries such as
`example:large_chestplate=0.125` for extra block-space clearance. Chest-slot backpacks do
not receive armor inflation. All defaults and new config remain development-only until release.
