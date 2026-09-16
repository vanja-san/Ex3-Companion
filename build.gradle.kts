import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	// Generates .project/.classpath for IDEs (Eclipse/JDT): ensures the Kotlin
	// compiled output is visible to the Java language server in VS Code.
    eclipse
	id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

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
	maven("https://maven.isxander.dev/releases") { // YACL
		name = "Xander Maven"
	}
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
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// Config screen library
	implementation("dev.isxander:yet-another-config-lib:${providers.gradleProperty("yacl_version").get()}")

	// Dev-only test mods: present in runClient, never compiled against, never bundled into the jar.
	// JEI is currently DISABLED (no 26.3 build yet) — the 26.2 jar crashes the datagen/client launch
	// on 26.3 (NoSuchFieldError: InputConstants.Type.KEYSYM). Re-enable once a 26.3 build exists.
	// localRuntime("maven.modrinth:jei:${providers.gradleProperty("jei_version").get()}")
	localRuntime("maven.modrinth:jade:${providers.gradleProperty("jade_version").get()}")
	// Resource-pack manager, dev convenience only (client-side).
	localRuntime("maven.modrinth:resourcify:${providers.gradleProperty("resourcify_version").get()}")
	// Shader stack for dev testing (Iris needs Sodium).
	localRuntime("maven.modrinth:sodium:${providers.gradleProperty("sodium_version").get()}")
	localRuntime("maven.modrinth:iris:${providers.gradleProperty("iris_version").get()}")

	// Mod Menu: mod list screen + config screens, dev-only.
	compileOnly("maven.modrinth:modmenu:${providers.gradleProperty("modmenu_version").get()}")
	localRuntime("maven.modrinth:modmenu:${providers.gradleProperty("modmenu_version").get()}")

	// Fabric Light API (LambDynamicLights): lets the companion emit light like a
	// torch so shader packs bloom it. The API classes live in a jar-in-jar inside the
	// mod, so we compile against the extracted stubs in libs/ (compileOnly keeps them
	// off our jar) and ship the full mod via localRuntime for runClient only.
	val lambdyn = providers.gradleProperty("lambdynlights_version").get()
	compileOnly(files("libs/lambdynlights-api.jar", "libs/yumi-commons-event.jar"))
	localRuntime("maven.modrinth:lambdynamiclights:$lambdyn")
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
	if (javacPath != null) {
		// Setting the executable implicitly enables forking in Gradle 9.
		options.forkOptions.executable = javacPath
	}
}

// Client Java mixins import Kotlin classes from the main source set.
// Ensure main Kotlin is compiled before client Java so the classes are on the classpath.
tasks.named("compileClientJava") {
	dependsOn(tasks.named("compileKotlin"))
}

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
	val mcVersion = providers.gradleProperty("minecraft_version").get()
	inputs.property("mcVersion", mcVersion)

	archiveBaseName.set("Ex3Companion")
	archiveVersion.set("v${project.version}-mc$mcVersion")
	archiveClassifier.set("Fabric")

	from("LICENSE") {
		rename { "${it}_Ex3Companion" }
	}
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
