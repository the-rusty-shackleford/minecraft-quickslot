"""Validate the approved worn-backpack resources in the two existing real clients.

Partitions: all six tiers; standing rear/side and crouching; approved quick-slot sword fit.
The separate Backpack Scale Preview pack must be disabled in both test profiles.
These captures require visual inspection; they do not prove Curios or shader compatibility.
"""
import body_booth as booth
import network_test as net

TIERS = ("backpack", "copper_backpack", "iron_backpack", "gold_backpack",
         "diamond_backpack", "netherite_backpack")


def equip(item: str) -> None:
    """Equip a server-owned backpack in the driver's chest slot."""
    net._send("server", "equip", slot="CHEST",
              stack={"item": "sophisticatedbackpacks:" + item, "count": 1})


def capture(name: str, view: str) -> None:
    """Save an actual observer framebuffer with the established booth camera."""
    booth.angle(view)
    booth.capture("observer", "backpack-approved-" + name + "-" + view)


def main() -> None:
    """Requires two isolated clients; effects: replace only test-player equipment."""
    booth.prepare()
    net._seed("air", count=0)
    net._send("driver", "sneak", value=False)
    for tier in TIERS:
        equip(tier)
        capture(tier, "rear")
    equip("backpack")
    capture("backpack", "left")
    net._send("driver", "sneak", value=True)
    capture("crouch", "left")
    net._send("driver", "sneak", value=False)
    net._seed("diamond_sword")
    capture("sword-fit", "rear")
    print("Approved backpack captures saved; inspect before accepting", flush=True)


if __name__ == "__main__":
    main()
