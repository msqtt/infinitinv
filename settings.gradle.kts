pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven {
			name = "WayYourMaven"
			url = uri("https://maven.wagyourtail.xyz/releases")
		}
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.7.11"
}

stonecutter {
	create(rootProject) {
		versions("1.12.2", "1.20.1", "1.21.1")
		vcsVersion = "1.20.1"
	}
}
