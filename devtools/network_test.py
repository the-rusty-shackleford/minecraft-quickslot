"""Exercise a real dedicated server and two clients; each process runs its test adapter.

Partitions: owner/observer, empty/full, stale/invalid requests, tracking exit/reentry,
relog, death retention/Vanishing, Nether/End travel, driver/passenger H priority.
The files coordinate fixtures and collect evidence; production payloads carry slot state.
"""
import json
import time
from collections.abc import Callable
from pathlib import Path
from typing import cast

ROOT = Path(__file__).resolve().parents[1] / "run/network-control"
SEQUENCES: dict[str, int] = {}
RESULTS: list[dict[str, object]] = []


def _object(value: object) -> dict[str, object]:
    if not isinstance(value, dict):
        return {}
    return cast(dict[str, object], value)


def _read(role: str) -> dict[str, object]:
    path = ROOT / f"{role}-state.json"
    if not path.exists():
        return {}
    raw: object = json.loads(path.read_text())
    state = _object(raw)
    if state.get("error"):
        raise AssertionError(f"{role}: {state['error']}")
    return state


def _player(role: str, name: str = "QuickDriver") -> dict[str, object]:
    return _object(_object(_read(role).get("players")).get(name))


def _number(value: object) -> int:
    return int(value) if isinstance(value, (int, float)) else -1


def _wait(predicate: Callable[[], bool], message: str, timeout: float = 40) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(0.15)
    snapshots = {role: _read(role) for role in ["server", "driver", "observer"]}
    (ROOT / "failure.json").write_text(json.dumps({"message": message, "states": snapshots}, indent=2))
    raise AssertionError(message)


def _send(role: str, op: str, **data: object) -> None:
    sequence = max(SEQUENCES.get(role, 0), _number(_read(role).get("seq"))) + 1
    SEQUENCES[role] = sequence
    payload = dict(data, seq=sequence, op=op)
    path = ROOT / f"{role}-command.json"
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(payload))
    temporary.replace(path)
    if op != "quit":
        _wait(lambda: _number(_read(role).get("seq")) >= sequence, f"{role} acknowledges {op}")


def _stack(role: str, name: str, slot: str, item: str, count: int) -> bool:
    stack = _object(_player(role, name).get(slot))
    return stack.get("item") == f"minecraft:{item}" and stack.get("count") == count


def _all_quick(item: str, count: int, name: str = "QuickDriver") -> bool:
    return all(_stack(role, name, "quick", item, count) for role in ["server", "driver", "observer"])


def _pass(name: str) -> None:
    RESULTS.append({"check": name, "states": {role: _read(role) for role in ["server", "driver", "observer"]}})
    (ROOT / "results.json").write_text(json.dumps(RESULTS, indent=2))
    print("PASS:", name, flush=True)


def _seed(quick: str, held: str = "air", count: int = 1, **extras: object) -> None:
    _send("server", "seed", quick={"item": f"minecraft:{quick}", "count": 0 if quick == "air" else count},
          held={"item": f"minecraft:{held}", "count": 0 if held == "air" else 1}, **extras)
    _wait(lambda: _all_quick(quick, 0 if quick == "air" else count), "seed visible on both clients")


def _settle() -> None:
    before = _number(_read("server").get("tick"))
    _wait(lambda: _number(_read("server").get("tick")) >= before + 15, "server advances after request")


def main(close: bool = True) -> None:
    """requires: three isolated test runs are active; effects: asserts and records live behavior."""
    if _player("server").get("alive") is False:
        _send("driver", "respawn")
    _wait(lambda: all(len(_object(_read(role).get("players"))) == 2 for role in ["server", "driver", "observer"]),
          "two real clients connected and tracking each other", 120)
    assert all(_read(role).get("stowed") is False for role in ["server", "driver", "observer"])
    _pass("two actual clients connected; Stowed absent on all three processes")
    _send("server", "seed", target="QuickObserver", quick={"item": "minecraft:air", "count": 0}, held={"item": "minecraft:air", "count": 0})

    _send("server", "seed", quick={"item": "minecraft:apple", "count": 64},
          held={"item": "minecraft:diamond_sword", "count": 1, "damage": 57, "name": "Carry proof"})
    _wait(lambda: _all_quick("apple", 64), "initial populated sync")
    _send("driver", "key")
    _wait(lambda: _all_quick("diamond_sword", 1), "H stores the sword for owner and observer")
    for role in ["server", "driver", "observer"]:
        stored = _object(_player(role).get("quick"))
        assert stored["damage"] == 57 and stored["name"] == "Carry proof"
    assert _stack("server", "QuickDriver", "held", "apple", 64)
    _pass("real H input swaps complete stacks and synchronizes components to observer")
    _send("driver", "key")
    _wait(lambda: _all_quick("apple", 64), "H swaps back")
    _pass("second H returns the original selected hotbar stack")

    for selected, revision in [(9, None), (-1, None), (0, -1), (1, None)]:
        args: dict[str, object] = {"selected": selected}
        if revision is not None:
            args["revision"] = revision
        _send("driver", "request", **args)
        _settle()
        assert _all_quick("apple", 64)
        assert _stack("server", "QuickDriver", "held", "diamond_sword", 1)
    _pass("real malformed/stale/mismatched-slot packets leave both stacks unchanged")

    _send("server", "mode", mode="spectator")
    _send("driver", "request", selected=0)
    _settle()
    assert _stack("server", "QuickDriver", "quick", "apple", 64)
    _send("server", "mode", mode="survival")
    _pass("spectator cannot swap through a forged request")

    _send("server", "teleport", target="QuickObserver", dimension="minecraft:overworld", x=1024)
    _wait(lambda: not _player("observer"), "driver leaves observer tracking range")
    _send("server", "teleport", target="QuickObserver", dimension="minecraft:overworld", x=6)
    _wait(lambda: _all_quick("apple", 64), "tracking reentry receives current stored stack")
    _pass("tracking exit and reentry restore the observer snapshot")

    _send("server", "save")
    _send("driver", "disconnect")
    _wait(lambda: not _player("server"), "driver disconnected from dedicated server")
    _send("driver", "join")
    _wait(lambda: _all_quick("apple", 64), "relogin restores persisted stack to both clients", 80)
    _pass("actual disconnect/relogin preserves inventory and rebuilds observer state")

    for dimension in ["minecraft:the_nether", "minecraft:overworld", "minecraft:the_end", "minecraft:overworld"]:
        _send("server", "teleport", dimension=dimension)
        def arrived(wanted: str = dimension) -> bool:
            return _player("driver").get("dimension") == wanted and _stack("driver", "QuickDriver", "quick", "apple", 64)
        _wait(arrived, f"dimension transfer to {dimension}", 80)
    _wait(lambda: _all_quick("apple", 64), "observer receives returned traveler")
    _pass("Nether and End travel preserve and resynchronize the quick slot")
    # Keep the observer in tracking range but out of pickup range for drop assertions.
    _send("server", "teleport", target="QuickObserver", dimension="minecraft:overworld", x=6)
    _settle()

    for keep, curse in [(False, False), (False, True), (True, False), (True, True)]:
        _send("server", "clearDrops")
        _send("server", "keep", value=keep)
        _seed("diamond_sword", vanishing=curse)
        _send("server", "kill")
        _wait(lambda: _player("server").get("alive") is False, "server player died")
        _send("driver", "request", selected=0)
        _settle()
        drops = _read("server").get("drops")
        assert isinstance(drops, list)
        swords = [d for d in drops if _object(d).get("item") == "minecraft:diamond_sword"]
        assert len(swords) == (0 if keep or curse else 1), (keep, curse, swords)
        _send("driver", "respawn")
        def respawned(retained: bool = keep) -> bool:
            return _player("driver").get("alive") is True and _all_quick("diamond_sword" if retained else "air", 1 if retained else 0)
        _wait(respawned, "real respawn and observer resync")
        _pass(f"actual death/respawn: keepInventory={keep}, Vanishing={curse}; no duplicate drops")
    _send("server", "keep", value=False)
    _send("server", "keepSlot", value=True)
    _seed("emerald", count=7)
    _send("server", "kill")
    _wait(lambda: _player("server").get("alive") is False, "retention-config death")
    _send("driver", "respawn")
    _wait(lambda: _player("driver").get("alive") is True and _all_quick("emerald", 7), "server retention option survives respawn")
    _send("server", "keepSlot", value=False)
    _pass("server keep-on-death option retains the stack through actual respawn")

    _seed("apple", "diamond_sword", 12)
    _send("server", "mount", passenger="QuickObserver")
    _wait(lambda: _player("driver").get("driving") is True and "vehicle" in _player("observer", "QuickObserver"), "both vehicle seats synchronized")
    lights = _player("server").get("lights")
    _send("driver", "key")
    _wait(lambda: _player("server").get("lights") != lights, "driver H toggles Vanilla Wheels lights")
    assert _all_quick("apple", 12)
    _send("driver", "request", selected=0)
    _settle()
    assert _all_quick("apple", 12)
    _pass("driver H changes vehicle lights only; forged quick-slot request also rejected")
    _send("server", "seed", target="QuickObserver", quick={"item": "minecraft:emerald", "count": 5}, held={"item": "minecraft:air", "count": 0})
    # Seeding dismounts to keep each setup deterministic; remount the passenger using the server fixture.
    _send("server", "dismount")
    _send("server", "mount", passenger="QuickObserver")
    _wait(lambda: "vehicle" in _player("observer", "QuickObserver") and not _player("observer", "QuickObserver").get("driving"), "observer is a passenger")
    _send("observer", "key")
    _wait(lambda: _all_quick("air", 0, "QuickObserver") and _stack("server", "QuickObserver", "held", "emerald", 5), "passenger H swaps quick slot")
    _pass("passenger retains H for Quick Slot")
    _send("server", "dismount")
    _wait(lambda: _player("driver").get("driving") is False, "driver has dismounted")
    _send("driver", "key")
    _wait(lambda: _all_quick("diamond_sword", 1), "H returns to Quick Slot on foot")
    _pass("dismount restores H to Quick Slot")

    print(f"PASS: {len(RESULTS)} dedicated-server/two-client checks", flush=True)
    if close:
        for role in ["driver", "observer", "server"]:
            _send(role, "quit")


if __name__ == "__main__":
    main()
