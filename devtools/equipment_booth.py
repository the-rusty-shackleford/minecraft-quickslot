"""Real-client equipment and resource-reload captures; inspect images before acceptance.

Partitions: no/chest/legs/full armor, crouched head pitch, narrow/wide elytra items,
consumable holders, and valid/invalid/deleted placement resources. Uses only isolated
network-* profiles; preserves and restores any pre-existing test-pack override.
"""
import json
import time
from pathlib import Path

import body_booth as booth
import network_test as net


def reload_resources() -> None:
    """Wait for the running observer's actual resource reload to finish."""
    net._send("observer", "resourceReload")
    time.sleep(1)
    net._wait(lambda: net._read("observer").get("reloading") is False,
                    "placement resources reloaded", 60)


def equip(slot: str, item: str) -> None:
    """Equip actual server inventory, which vanilla synchronizes to both clients."""
    net._send("server", "equip", slot=slot, stack={"item": "minecraft:" + item})


def views(name: str, angles: tuple[str, ...] = ("rear", "left")) -> None:
    """Save current geometry from real second-player cameras."""
    for angle in angles:
        booth.angle(angle)
        booth.capture("observer", "phase4-fit-" + name + "-" + angle)


def main() -> None:
    booth.prepare()
    for armor in ["bare", "chest", "legs", "full"]:
        net._seed("diamond_sword")
        if armor in ["chest", "full"]:
            equip("CHEST", "iron_chestplate")
        if armor in ["legs", "full"]:
            equip("LEGS", "iron_leggings")
        if armor == "full":
            equip("HEAD", "iron_helmet")
            equip("FEET", "iron_boots")
        for crouch in [False, True]:
            net._send("driver", "sneak", value=crouch)
            views(armor + ("-crouch" if crouch else "-stand"))
    for pitch in [-70, 70]:
        net._send("driver", "view", yaw=0, pitch=pitch, hide=True)
        views("head-" + str(pitch))
    net._send("driver", "view", yaw=0, pitch=0, hide=True)
    net._send("driver", "sneak", value=False)
    for item in ["diamond_pickaxe", "trident", "shield", "bow", "crossbow"]:
        net._seed(item)
        net._send("driver", "sneak", value=True)
        views(item + "-crouch")
    net._send("driver", "sneak", value=False)
    for item in ["diamond_sword", "diamond_pickaxe", "trident", "shield", "bow", "crossbow"]:
        net._seed(item)
        equip("CHEST", "elytra")
        views("elytra-" + item)
    for item in ["potion", "apple", "lantern"]:
        net._seed(item)
        equip("CHEST", "iron_chestplate")
        equip("LEGS", "iron_leggings")
        views("armor-" + item)
    net._seed("diamond_sword")
    override = (Path("run/network-observer/resourcepacks/Refined Tools 3.0/assets")
                / "quickslot/quickslot_placement/minecraft/diamond_sword.json")
    previous = override.read_bytes() if override.exists() else None
    override.parent.mkdir(parents=True, exist_ok=True)
    try:
        for name, anchor in [("hidden", "hidden"), ("hip", "hip"), ("invalid", "oops")]:
            override.write_text(json.dumps({"anchor": anchor}))
            reload_resources()
            views("json-" + name, ("rear",))
        override.unlink()
        reload_resources()
        views("json-removed", ("rear",))
    finally:
        if previous is None:
            override.unlink(missing_ok=True)
        else:
            override.write_bytes(previous)
        reload_resources()
    print("Equipment and JSON captures saved; visual inspection still required", flush=True)


if __name__ == "__main__":
    main()
