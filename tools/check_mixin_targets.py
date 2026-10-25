#!/usr/bin/env python3
"""Check that every mixin injection really fits its target.

Why this exists
---------------
The refmap that ForgeGradle/Unimined generates records the *mixin target
class* as the owner of every injected method. At runtime Mixin therefore
looks the method up **in that class** - not in a superclass and not in an
interface. A method that is only inherited (for example ``mouseScrolled``,
which comes from the ``GuiEventListener`` interface, or a field that lives
on a superclass) is renamed by the obfuscation mapping and then cannot be
found:

    InvalidInjectionException: Critical injection failure: @Inject annotation
    on <method> could not find any targets matching
    'Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;...'

The callback type has to match the return type of the target as well: a
target that returns a value needs a ``CallbackInfoReturnable<T>``, a target
that returns void needs a plain ``CallbackInfo``:

    InvalidInjectionException: Invalid descriptor on ...! CallbackInfoReturnable
    is required!

Both only show up in a real (production, SRG named) client, never in the dev
environment, so they have to be checked statically.

Usage
-----
    python3 tools/check_mixin_targets.py            # every known version
    python3 tools/check_mixin_targets.py 1.20.1     # a single version
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE_HOME = os.path.expanduser(
    os.environ.get("GRADLE_USER_HOME", os.path.join("~", ".gradle"))
)

# The decompiled Minecraft + Forge jar of each version, as produced by
# Unimined in the Gradle cache. They contain the names the mixin sources are
# written against. The paths are relative to the Gradle user home so the
# check also runs in CI.
JARS = {
    "1.12.2": (
        "caches/unimined/net/minecraft/minecraft/1.12.2/MinecraftForge-FG3/"
        "1.12.2-14.23.5.2860/mcp-stable-39-1.12-searge-1.12.2-20200226.224830/"
        "forge-1.12.2-14.23.5.2860-MinecraftForge-FG3+forge-1.12.2-14.23.5.2860"
        "+fixForge-mcp-at.jar"
    ),
    "1.20.1": (
        "caches/unimined/net/minecraft/minecraft/1.20.1/MinecraftForge-FG3/"
        "1.20.1-47.3.0/mojmap-searge/forge-1.20.1-47.3.0-MinecraftForge-FG3"
        "+forge-1.20.1-47.3.0-mojmap.jar"
    ),
    "1.21.1": (
        "caches/unimined/net/minecraft/minecraft/1.21.1/MinecraftForge-FG3/"
        "1.21.1-52.0.28/mojmap-searge/forge-1.21.1-52.0.28-MinecraftForge-FG3"
        "+forge-1.21.1-52.0.28+fixForge-mojmap.jar"
    ),
}

MIXIN_RE = re.compile(r"@Mixin\(\s*([A-Za-z0-9_.]+)\.class\s*\)")
IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([A-Za-z0-9_.]+)\s*;", re.M)
# @Inject(... method = "target" ...) <return type> name(<params>)
HANDLER_RE = re.compile(
    r'@Inject\((?P<args>[^)]*method\s*=\s*"(?P<target>[A-Za-z0-9_]+)"[^)]*)\)'
    r"(?:\s*@[\w.]+(?:\([^)]*\))?)*\s*"
    r"(?:public|private|protected)?\s*[\w.<>\[\]]+\s+(?P<name>\w+)\s*\((?P<params>[^)]*)\)",
    re.S,
)
SHADOW_RE = re.compile(
    r"@Shadow\s+(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?"
    r"[\w.<>\[\], ]+\s+([A-Za-z0-9_]+)\s*;"
)
METHOD_RE = re.compile(
    r"(?:public|private|protected)?\s*[\w.<>\[\]]+\s+([\w$]+)\s*\(([^)]*)\)"
)


def find_injections(source: str) -> list[tuple[str, str, str]]:
    """Return [(annotation arguments, handler name, handler parameters)].

    The arguments are collected with balanced parentheses, because
    ``@At("HEAD")`` and ``@At("TAIL")`` contain a closing parenthesis of
    their own.
    """
    found: list[tuple[str, str, str]] = []
    index = 0
    while True:
        start = source.find("@Inject(", index)
        if start < 0:
            return found
        depth = 0
        cursor = start + len("@Inject")
        while cursor < len(source):
            if source[cursor] == "(":
                depth += 1
            elif source[cursor] == ")":
                depth -= 1
                if depth == 0:
                    break
            cursor += 1
        arguments = source[start + len("@Inject("):cursor]
        match = METHOD_RE.search(source[cursor:cursor + 400])
        if match:
            found.append((arguments, match.group(1), match.group(2)))
        index = cursor + 1


def resolve_target(source: str, name: str) -> str:
    """Turn a simple class name into a fully qualified one using the imports."""
    if "." in name:
        return name
    for imported in IMPORT_RE.findall(source):
        if imported.rsplit(".", 1)[-1] == name:
            return imported
    return name


def declared_methods(jar: str, class_name: str, workdir: str) -> dict[str, str] | None:
    """Return {method name: return type} for the class, or None if absent."""
    path = class_name.replace(".", "/") + ".class"
    try:
        with zipfile.ZipFile(jar) as archive:
            if path not in archive.namelist():
                return None
            target = os.path.join(workdir, class_name.replace(".", "/"))
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target + ".class", "wb") as handle:
                handle.write(archive.read(path))
    except (OSError, zipfile.BadZipFile):
        return None

    result = subprocess.run(
        ["javap", "-p", "-cp", workdir, class_name],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return None
    methods: dict[str, str] = {}
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line.endswith(";") or "(" not in line:
            continue
        head, _, _tail = line.partition("(")
        words = head.split()
        if len(words) < 2:
            continue
        methods[words[-1]] = words[-2]
    return methods


def callback_type(params: str) -> str:
    """The type of the last parameter of a mixin handler."""
    parts = [p.strip() for p in params.split(",") if p.strip()]
    if not parts:
        return ""
    words = [w for w in parts[-1].split() if w != "final"]
    return words[0] if words else ""


def check_version(version: str, jar: str, workdir: str) -> list[str]:
    problems: list[str] = []
    mixin_dir = os.path.join(
        ROOT, "versions", version, "src/main/java/com/infinitinv/mixin"
    )
    if not os.path.isdir(mixin_dir):
        return problems
    for name in sorted(os.listdir(mixin_dir)):
        if not name.endswith(".java"):
            continue
        source = open(os.path.join(mixin_dir, name)).read()
        target_match = MIXIN_RE.search(source)
        if not target_match:
            problems.append(f"{version}/{name}: no @Mixin target found")
            continue
        target = resolve_target(source, target_match.group(1))
        methods = declared_methods(jar, target, workdir)
        if methods is None:
            print(f"  ! {version}: could not read {target} from the jar, skipping")
            continue
        for arguments, _handler, params in find_injections(source):
            match = re.search(r'method\s*=\s*"([A-Za-z0-9_]+)"', arguments)
            if not match:
                continue
            method = match.group(1)
            if method == "<init>":
                continue
            if method not in methods:
                problems.append(
                    f"{version}/{name}: @Inject into '{method}' - "
                    f"{target} does not declare it"
                )
                continue
            returns = methods[method]
            callback = callback_type(params)
            cancellable = "cancellable = true" in arguments
            if cancellable and returns != "void" and "CallbackInfoReturnable" not in callback:
                problems.append(
                    f"{version}/{name}: @Inject into '{method}' returns {returns}, so the "
                    f"handler needs a CallbackInfoReturnable, not '{callback}'"
                )
            if returns == "void" and callback.startswith("CallbackInfoReturnable"):
                problems.append(
                    f"{version}/{name}: @Inject into '{method}' returns void, so the "
                    f"handler must not use a CallbackInfoReturnable"
                )
        for shadow in SHADOW_RE.findall(source):
            if shadow in ("if", "for", "while", "return"):
                continue
            if shadow not in methods:
                problems.append(
                    f"{version}/{name}: @Shadow of '{shadow}' - "
                    f"{target} does not declare it"
                )
    return problems


def main() -> int:
    workdir = os.path.join("/tmp", "mixin-target-check")
    os.makedirs(workdir, exist_ok=True)
    all_problems: list[str] = []
    wanted = sys.argv[1:] or list(JARS.keys())
    for version in wanted:
        if version not in JARS:
            print(f"  ! unknown version {version}")
            continue
        jar = os.path.join(GRADLE_HOME, JARS[version])
        if not os.path.exists(jar):
            print(f"  ! {version}: jar not found at {jar}, skipping")
            continue
        print(f"== checking {version} ==")
        all_problems.extend(check_version(version, jar, workdir))
    if all_problems:
        print("\nFAILED:")
        for problem in all_problems:
            print("  " + problem)
        return 1
    print("\nAll mixin injections match their target.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
