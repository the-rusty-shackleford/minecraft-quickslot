"""Real-client review of D-0006. Captures require human inspection, not pixel assertions.

Partitions: approved earlier back models; armor/head motion without automatic offsets;
elytra hiding back/pouch/bottle/hanging/component items; every vanilla bowl meal without
elytra; a placement override trying to show a bowl; H swaps while the display is hidden.
"""
import json
from pathlib import Path

import body_booth as booth
import equipment_booth as equipment
import network_test as net


def capture(name: str, angles: tuple[str, ...] = ("rear",)) -> None:
    """Capture current geometry through the real observer camera."""
    for angle in angles:
        booth.angle(angle)
        booth.capture("observer", "feedback-" + name + "-" + angle)


def swap_hidden(item: str) -> None:
    """Assert that hiding a body display never prevents the ordinary H exchange."""
    net._send("driver", "key")
    net._wait(lambda: net._all_quick("air", 0)
              and net._stack("server", "QuickDriver", "held", item, 1), "hidden item drawn")
    net._send("driver", "key")
    net._wait(lambda: net._all_quick(item, 1)
              and net._stack("server", "QuickDriver", "held", "air", 0), "hidden item restowed")
    print("PASS: hidden-item H swap: " + item, flush=True)


def main() -> None:
    booth.prepare()
    net._send("driver", "sneak", value=False)
    for item in ["diamond_sword", "diamond_pickaxe", "bow", "crossbow", "shield", "trident"]:
        net._seed(item)
        capture("restored-" + item, ("rear", "left"))
    net._seed("diamond_sword")
    for slot, item in [("HEAD", "iron_helmet"), ("CHEST", "iron_chestplate"), ("LEGS", "iron_leggings")]:
        equipment.equip(slot, item)
    net._send("driver", "sneak", value=True)
    net._send("driver", "view", yaw=0, pitch=-70, hide=True)
    capture("head-up-keeps-sword", ("rear", "left"))
    net._send("driver", "sneak", value=False)
    net._send("driver", "view", yaw=0, pitch=0, hide=True)
    for item in ["diamond_sword", "apple", "potion", "iron_ingot", "shield", "lantern"]:
        net._seed(item)
        equipment.equip("CHEST", "elytra")
        capture("elytra-hides-" + item, ("rear", "left"))
    swap_hidden("lantern")
    equipment.equip("CHEST", "air")
    capture("elytra-removed-lantern", ("rear", "left"))
    for item in ["mushroom_stew", "beetroot_soup", "rabbit_stew", "suspicious_stew"]:
        net._seed(item)
        capture("bowl-hidden-" + item, ("rear", "left"))
    net._seed("mushroom_stew")
    swap_hidden("mushroom_stew")
    override = (Path("run/network-observer/resourcepacks/Refined Tools 3.0/assets")
                / "quickslot/quickslot_placement/minecraft/mushroom_stew.json")
    previous = override.read_bytes() if override.exists() else None
    override.parent.mkdir(parents=True, exist_ok=True)
    try:
        override.write_text(json.dumps({"anchor": "hip", "style": "pouch", "scale": 1}))
        equipment.reload_resources()
        capture("bowl-override-still-hidden", ("rear", "left"))
    finally:
        if previous is None:
            override.unlink(missing_ok=True)
        else:
            override.write_bytes(previous)
        equipment.reload_resources()
    net._seed("bowl")
    capture("empty-bowl-is-component")
    net._seed("diamond_sword")
    net._send("driver", "view", camera="THIRD_PERSON_BACK", fov=70, hide=True, yaw=0, pitch=10)
    booth.capture("driver", "feedback-restored-own-third-person")
    print("Feedback captures saved; inspect them before accepting the change", flush=True)


if __name__ == "__main__":
    main()
