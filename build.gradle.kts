plugins {
	java
	id("xyz.wagyourtail.unimined") version "1.4.1"
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://maven.wagyourtail.xyz/releases")
	}
	maven {
		url = uri("https://repo.spongepowered.org/maven")
	}
	maven {
		url = uri("https://nexus.gtnewhorizons.com/repository/public/")
	}
	// Hosts zone.rong:mixinbooter (the coremod that lets 1.12.2 load the
	// Mixin library into the FML classloader) and the legacy
	// org.spongepowered:mixin builds.
	maven {
		url = uri("https://maven.cleanroommc.com")
	}
	maven {
		url = uri("https://maven.minecraftforge.net")
	}
}

dependencies {
}

// Per-version dependencies live in versions/<mcVersion>/deps.gradle and are
// applied by the stonecutter-generated project for that version.
if (file("deps.gradle").exists()) apply(from = "deps.gradle")

unimined.minecraft {
	version = property("mcVersion").toString()

	mappings {
		// 1.16.5+ ships Mojang's official mappings; 1.12.2 only has
		// SRG/MCP, so use the stable_39 MCP channel there.
		if (stonecutter.eval(stonecutter.current.version, ">=1.17")) {
			mojmap()
		} else {
			mcp("stable", property("mcpMappingVersion").toString())
		}
	}

	minecraftForge {
		loader(property("forgeVersion").toString())
		mixinConfig("mixins.infinitinv.json")
	}
}

java {
	// 1.12.2 runs on Java 8, 1.17-1.20 on Java 17 and 1.21+ on Java 21.
	toolchain {
		languageVersion = JavaLanguageVersion.of(
			if (stonecutter.eval(stonecutter.current.version, ">=1.21")) 21
			else if (stonecutter.eval(stonecutter.current.version, ">=1.17")) 17
			else 8
		)
	}
}

// The project is set up for several Minecraft versions through Stonecutter.
// Only the versions listed in "readyVersions" are built by default; the other
// version directories are kept in the tree so the porting work stays visible.
// See PORTING.md for what a new version needs.
val readyVersions: List<String> = (project.findProperty("readyVersions") as String? ?: "1.12.2")
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

if (stonecutter.current.version !in readyVersions) {
    logger.lifecycle("Skipping build of ${stonecutter.current.version} (not in readyVersions)")
    tasks.matching {
        it.name in setOf("compileJava", "jar", "remapJar", "assemble", "build")
    }.configureEach {
        enabled = false
    }
}

