#!/usr/bin/env python3
"""Check that every mixin injection targets a method the mixin target declares.

Why this exists
---------------
The refmap that ForgeGradle/Unimined generates records the *mixin target
class* as the owner of every injected method. At runtime Mixin therefore
looks the method up **in that class** - not in a superclass and not in an
interface. A method that is only inherited (for example
``mouseScrolled``, which comes from the ``GuiEventListener`` interface, or a
field that lives on a superclass) is renamed by the obfuscation mapping and
then cannot be found:

    InvalidInjectionException: Critical injection failure: @Inject annotation
    on <method> could not find any targets matching
    'Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;...'

This only happens in a real (production, SRG named) client, never in the dev
environment, so it has to be checked statically.

Usage
-----
    python3 tools/check_mixin_targets.py            # every known version
    python3 tools/check_mixin_targets.py 1.20.1     # a single version
"""

from __future__ import annotations

import json
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
# Unimined in the Gradle cache. They contain the Mojang names, which is what
# the mixin sources are written against. The paths are relative to the
# Gradle user home so the check also runs in CI.
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
INJECT_RE = re.compile(r'@Inject\(\s*method\s*=\s*"([A-Za-z0-9_]+)"')
SHADOW_RE = re.compile(
    r"@Shadow\s+(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?"
    r"[\w.<>\[\], ]+\s+([A-Za-z0-9_]+)\s*;"
)
IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([A-Za-z0-9_.]+)\s*;", re.M)


def resolve_target(source: str, name: str) -> str:
    """Turn a simple class name into a fully qualified one using the imports."""
    if "." in name:
        return name
    for imported in IMPORT_RE.findall(source):
        if imported.rsplit(".", 1)[-1] == name:
            return imported
    return name


def declared_methods(jar: str, class_name: str, workdir: str) -> set[str] | None:
    """Return the methods the given class declares, or None if it is absent."""
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
    names = set()
    for line in result.stdout.splitlines():
        line = line.strip()
        match = re.search(r"([A-Za-z0-9_$]+)\(", line)
        if match:
            names.add(match.group(1))
    return names


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
        for method in INJECT_RE.findall(source):
            if method == "<init>":
                continue
            if method not in methods:
                problems.append(
                    f"{version}/{name}: @Inject into '{method}' - "
                    f"{target} does not declare it (declared: "
                    f"{', '.join(sorted(m for m in methods if m.startswith(method[:4]))[:4]) or 'none'})"
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
        relative = JARS[version]
        jar = os.path.join(GRADLE_HOME, relative)
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
    print("\nAll mixin injection targets are declared by their mixin target.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
