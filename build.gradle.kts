import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.fabric.loom)
	`maven-publish`
	// Generates .project/.classpath for IDEs (Eclipse/JDT): ensures the Kotlin
	// compiled output is visible to the Java language server in VS Code.
	eclipse
	alias(libs.plugins.kotlin.jvm)
}

// Repositories live here (project level) rather than settings dependencyResolutionManagement:
// Fabric Loom registers its own project-level repositories, and per
// RepositoriesMode.PREFER_SETTINGS that shadows settings-declared repos, which
// would silently drop Modrinth/JEI from localRuntime resolution. project-level
// repos append to Loom's instead. Content filtering narrows each repo's scope.
repositories {
	// Modrinth first: reachable and hosts all maven.modrinth artifacts.
	maven("https://api.modrinth.com/maven") { // Jade, Sodium, Iris, Mod Menu, etc.
		content {
			includeGroup("maven.modrinth")
		}
	}
	// JEI is fetched via its Modrinth maven id (maven.blamejared.com is flaky/blocked).
	maven("https://maven.blamejared.com/") {
		content {
			includeGroup("mezz.jei")
		}
	}
	// configlibtxf (TXFConfig) is published to Maven Central.
	mavenCentral()
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("ex3-companion") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

dependencies {
	minecraft(libs.minecraft)
	implementation(libs.fabric.loader)

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation(libs.fabric.api)
	implementation(libs.fabric.language.kotlin)

	// Config screen library: configlibtxf (TXFConfig). Bundled jar-in-jar via include():
	// its own fabric.mod.json is loaded, and its AutoModMenu provides our config screen
	// factory to Mod Menu. We register our own ModMenuApi (ModMenuIntegration) which
	// takes priority so we can gate what opens from the config button (server settings
	// are withheld outside of a world).
	implementation(libs.configlibtxf)
	include(libs.configlibtxf)

	// Dev-only test mods: present in runClient, never compiled against, never bundled into the jar.
	// JEI is currently DISABLED (no 26.3 build yet) — the 26.2 jar crashes the datagen/client launch
	// on 26.3 (NoSuchFieldError: InputConstants.Type.KEYSYM). Re-enable once a 26.3 build exists.
	// localRuntime(libs.jei)
	localRuntime(libs.jade)
	// Resource-pack manager, dev convenience only (client-side).
	localRuntime(libs.resourcify)
	// Shader stack for dev testing (Iris needs Sodium).
	localRuntime(libs.sodium)
	localRuntime(libs.iris)

	// Mod Menu: mod list screen + config screens, dev-only.
	compileOnly(libs.modmenu)
	localRuntime(libs.modmenu)

	// Fabric Light API (LambDynamicLights): lets the companion emit light like a
	// torch so shader packs bloom it. The API classes live in a jar-in-jar inside the
	// mod, so we compile against the extracted stubs in libs/ (compileOnly keeps them
	// off our jar) and ship the full mod via localRuntime for runClient only.
	compileOnly(files("libs/lambdynlights-api.jar", "libs/yumi-commons-event.jar"))
	localRuntime(libs.lambdynamiclights)
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

// Per-machine javac override. The Gradle extension can launch Gradle with a
// different JVM than the CLI, which breaks `--release 25` on older JDKs.
// Set `javac.path` in ~/.gradle/gradle.properties (user-level, NOT tracked by
// git) to fork javac from a specific JDK. CI never sets it, so it stays on the
// runner's JDK from actions/setup-java.
val javacPath = providers.gradleProperty("javac.path").orNull
tasks.withType<JavaCompile>().configureEach {
	options.release = 25
	options.encoding = "UTF-8"
	if (javacPath != null) {
		// Setting the executable implicitly enables forking in Gradle 9.
		options.forkOptions.executable = javacPath
	}
}

// Client Java mixins import Kotlin classes from the main source set. Loom wires
// the main output (incl. Kotlin) into the client compile classpath, so Gradle
// infers the ordering implicitly — no explicit dependsOn required.

// Eclipse / VS Code: make client source set see main Kotlin output.
eclipse {
	classpath {
		file {
			withXml {
				val node = asNode()
				// Add build/classes/kotlin/main so Java language server resolves Kotlin classes.
				node.appendNode("classpathentry", mapOf(
					"kind" to "lib",
					"path" to "build/classes/kotlin/main"
				))
			}
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	archiveBaseName.set("Ex3Companion")
	archiveClassifier.set("Fabric")
	archiveVersion.set(libs.versions.minecraft.map { "v${project.version}-mc$it" })

	from("LICENSE") {
		rename { "${it}_Ex3Companion" }
	}
}

// Byte-for-byte reproducible archives (timestamps/ordering normalized).
tasks.withType<AbstractArchiveTask>().configureEach {
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}