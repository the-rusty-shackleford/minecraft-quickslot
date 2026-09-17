"""Actual third-person captures with Refined Tools through a dedicated server and two clients.

Partitions: 3D tools, sprite/custom renderers, consumable styles, named/ordinary blocks;
rear/front/left/right cameras. These are visual evidence, not automated quality assertions.
"""
import time
from pathlib import Path

import network_test as net

ROOT = Path(__file__).resolve().parents[1]
OBSERVER = "QuickObserver"


def capture(role: str, name: str) -> None:
    """Capture a settled live framebuffer, refusing to overwrite prior evidence."""
    path = ROOT / f"run/network-{role}/screenshots/{name}.png"
    assert not path.exists(), path
    time.sleep(0.8)
    net._send(role, "capture", name=f"{name}.png")
    net._wait(path.exists, f"saved {name}")


def prepare() -> None:
    """Put both real clients at the existing booth with fixed neutral daylight."""
    net._wait(lambda: len(net._object(net._read("server").get("players"))) == 2
              and all(net._read(r).get("connected") is True for r in ["driver", "observer"]),
              "two clients joined", 120)
    global OBSERVER
    OBSERVER = str(net._read("observer").get("self"))
    net._send("server", "booth")
    net._send("server", "seed", target=OBSERVER, quick={"item": "minecraft:air", "count": 0},
              held={"item": "minecraft:air", "count": 0})
    net._send("server", "mode", target=OBSERVER, mode="spectator")
    for role in ["driver", "observer"]:
        net._send(role, "view", camera="FIRST_PERSON", fov=50, hide=True, width=1280, height=960, yaw=0, pitch=0)


def angle(name: str) -> None:
    """Observe the driver from a real second player's first-person camera."""
    x, z, yaw = {"rear": (0, -3.2, 0), "front": (0, 3.2, 180),
                 "left": (3.2, 0, 90), "right": (-3.2, 0, -90)}[name]
    net._send("server", "position", target=OBSERVER, x=x, y=70.7, z=z, yaw=yaw, pitch=12)


def item_views(item: str, prefix: str, views: tuple[str, ...] = ("rear", "front", "left", "right")) -> None:
    net._seed(item)
    for view in views:
        angle(view)
        capture("observer", f"{prefix}-{item}-{view}")


def main() -> None:
    prepare()
    for item in ["diamond_sword", "diamond_pickaxe", "trident", "shield", "potion", "apple", "lantern"]:
        item_views(item, "phase3-first")
    net._seed("diamond_sword")
    net._send("driver", "view", camera="THIRD_PERSON_BACK", fov=70, hide=True, yaw=0, pitch=10)
    capture("driver", "phase3-first-own-back")
    net._send("driver", "sneak", value=True)
    capture("driver", "phase3-first-own-crouch")
    net._send("driver", "sneak", value=False)
    print("Captured initial fit; keep the same clients for inspection and follow-up", flush=True)


if __name__ == "__main__":
    main()
