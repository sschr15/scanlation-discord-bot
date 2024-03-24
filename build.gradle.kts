import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	application

	kotlin("jvm")
	kotlin("plugin.serialization")

//	id("com.github.johnrengelman.shadow")
	id("io.gitlab.arturbosch.detekt")
}

group = "com.sschr15"
version = "1.0-SNAPSHOT"

repositories {
	google()
	mavenCentral()

	maven {
		name = "Sonatype Snapshots (Legacy)"
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}

	maven {
		name = "Sonatype Snapshots"
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
	}
}

dependencies {
	detektPlugins(libs.detekt)

	implementation(libs.bundles.kord)
	implementation(libs.bundles.kordex)
	implementation(libs.bundles.mongodb)
	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)

	// Logging dependencies
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
}

application {
	mainClass.set("com.sschr15.scanlation.AppKt")
}

tasks.withType<KotlinCompile> {
	// Current LTS version of Java
	kotlinOptions.jvmTarget = "17"

	kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.jar {
	manifest {
		attributes(
			"Main-Class" to "com.sschr15.scanlation.AppKt"
		)
	}
}

tasks.register("shadowJar", Jar::class) {
	archiveBaseName.set("scanlation")
	archiveClassifier.set("all")

	from(zipTree(tasks.jar.get().archiveFile))
	from(configurations.runtimeClasspath.get().map { it.takeIf { it.isDirectory } ?: zipTree(it) }) {
		exclude("module-info.class")
		exclude("META-INF/**")
	}

	manifest {
		attributes(
			"Main-Class" to "com.sschr15.scanlation.AppKt"
		)
	}

	dependsOn(tasks.jar)

	duplicatesStrategy = DuplicatesStrategy.WARN
}

java {
	// Current LTS version of Java
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

detekt {
	buildUponDefaultConfig = true

	config.from(rootProject.files("detekt.yml"))
}
