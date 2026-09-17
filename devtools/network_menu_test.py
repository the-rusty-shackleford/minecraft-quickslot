"""Run Phase 1 regressions, then real GUI clicks and inventory packet transactions.

Partitions: pickup/place/split, shift both directions, number-key exchange, drop,
left/right arm with hotbar attack on/off, survival/creative, count/durability rendering.
Screenshots are evidence for human inspection, never a substitute for seeing the UI.
"""
from pathlib import Path

import network_test as net

ROOT = Path(__file__).resolve().parents[1]


def _cursor(item: str, count: int) -> bool:
    return all(net._stack(role, "QuickDriver", "cursor", item, count) for role in ["server", "driver"])


def _inventory(role: str, index: int, item: str, count: int) -> bool:
    inventory = net._player(role).get("inventory")
    if not isinstance(inventory, list) or index >= len(inventory):
        return False
    stack = net._object(inventory[index])
    return stack.get("item") == f"minecraft:{item}" and stack.get("count") == count


def _capture(name: str) -> None:
    net._settle()
    path = ROOT / "run/network-driver/screenshots" / name
    assert not path.exists(), f"Preserve earlier screenshot before replay: {name}"
    net._send("driver", "capture", name=name)
    net._wait(path.exists, f"screenshot {name} written")


def main(regressions: bool = True) -> None:
    """requires: isolated server/two clients; effects: asserts real menu state and stops on success."""
    if regressions:
        net.main(close=False)
    net._send("driver", "closeGui")
    net._send("server", "seed", quick={"item": "minecraft:air", "count": 0}, held={"item": "minecraft:apple", "count": 17})
    net._wait(lambda: net._all_quick("air", 0), "empty slot seeded")
    net._send("driver", "inventory")
    net._wait(lambda: net._read("driver").get("screen") == "InventoryScreen", "real inventory key opens screen")
    cell = net._number(net._player("server").get("quickIndex"))
    assert cell >= 46
    net._send("driver", "mouseSlot", slot=36, button=0)
    net._wait(lambda: _cursor("apple", 17), "normal mouse pickup synchronizes cursor")
    net._send("driver", "mouseSlot", slot=cell, button=0)
    net._wait(lambda: net._all_quick("apple", 17) and _cursor("air", 0), "real cell hitbox accepts full stack")
    net._pass("real inventory key and mouse coordinates place full stack; observer receives it")
    _capture("phase2-inventory-count.png")

    net._send("driver", "mouseSlot", slot=cell, button=1)
    net._wait(lambda: net._all_quick("apple", 8) and _cursor("apple", 9), "right-click splits without loss")
    net._send("driver", "mouseSlot", slot=cell, button=1)
    net._wait(lambda: net._all_quick("apple", 9) and _cursor("apple", 8), "right-click places exactly one")
    net._send("driver", "mouseSlot", slot=36, button=0)
    net._wait(lambda: _cursor("air", 0), "remaining cursor stack returned")
    assert _inventory("server", 0, "apple", 8)
    net._pass("right-click split and single-item insertion conserve all 17 items across peers")

    net._send("driver", "menuClick", slot=cell, button=0, type="QUICK_MOVE")
    # Vanilla first merges compatible stacks, including the existing eight apples in hotbar.
    net._wait(lambda: net._all_quick("air", 0) and _inventory("server", 0, "apple", 17), "shift merges back into inventory")
    net._send("driver", "menuClick", slot=36, button=0, type="QUICK_MOVE")
    net._wait(lambda: net._all_quick("apple", 17) and _inventory("server", 0, "air", 0), "shift inserts into empty quick slot")
    net._pass("actual inventory click packets shift in both directions with synchronized views")
    net._send("server", "seed", quick={"item": "minecraft:apple", "count": 8}, held={"item": "minecraft:bread", "count": 9})
    net._wait(lambda: net._all_quick("apple", 8) and _inventory("driver", 0, "bread", 9), "distinct stacks ready for number-key exchange")
    net._send("driver", "menuClick", slot=cell, button=0, type="SWAP")
    net._wait(lambda: net._all_quick("bread", 9) and _inventory("server", 0, "apple", 8), "number-key exchange swaps complete stacks")
    net._pass("number-key exchange preserves independent hotbar and quick-slot stacks")
    net._send("server", "clearDrops")
    net._send("driver", "menuClick", slot=cell, button=0, type="THROW")
    net._wait(lambda: net._all_quick("bread", 8), "drop key removes exactly one")
    drops = net._read("server").get("drops")
    assert isinstance(drops, list) and any(net._object(x).get("item") == "minecraft:bread" and net._object(x).get("count") == 1 for x in drops)
    net._pass("drop action creates exactly one real item entity and updates all quick-slot views")
    net._send("driver", "closeGui")

    net._send("server", "seed", quick={"item": "minecraft:diamond_sword", "count": 1, "damage": 750}, held={"item": "minecraft:apple", "count": 12})
    net._wait(lambda: net._all_quick("diamond_sword", 1), "durability sample synced")
    for arm in ["RIGHT", "LEFT"]:
        for attack in ["HOTBAR", "CROSSHAIR"]:
            net._send("driver", "hud", arm=arm, attack=attack)
            _capture(f"phase2-hud-{arm.lower()}-{attack.lower()}.png")
    net._pass("captured actual HUD for both main arms and attack-indicator settings, including durability")

    net._send("server", "mode", mode="creative")
    net._send("driver", "inventory")
    net._wait(lambda: net._read("driver").get("screen") == "CreativeModeInventoryScreen", "creative screen opens")
    _capture("phase2-creative.png")
    net._send("driver", "closeGui")
    net._send("driver", "key")
    net._wait(lambda: net._all_quick("apple", 12), "creative H uses authoritative swap")
    net._pass("creative inventory remains usable and creative H still swaps through the server")
    print(f"PASS: {len(net.RESULTS)} dedicated-server/two-client checks; screenshots require visual review", flush=True)
    for role in ["driver", "observer", "server"]:
        net._send(role, "quit")


if __name__ == "__main__":
    main()
