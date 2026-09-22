pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

// Centralized repositories in settings are shadowed by Fabric Loom's own
// project-level repository declarations (RepositoriesMode.PREFER_SETTINGS), so
// dependency repositories stay in the project build script where they append to
// Loom's. Content filtering narrows each repo's scope there.

// Should match your modid
rootProject.name = "ex3-companion"