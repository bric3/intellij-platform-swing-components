
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.annotations)
}

intellij {
    pluginName = "ignored"
    version = properties("platformVersion")
    type = properties("platformType")

    instrumentCode = false
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
        }
    }

    val javadocJar by registering(Jar::class) {
        // Note that we publish the Dokka HTML artifacts as Javadoc
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
    }

    withType<Jar>() {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        manifest {
            attributes["Implementation-Version"] = version
        }
    }

    listOf(
        runIde,
        prepareSandbox,
        runPluginVerifier,
        jarSearchableOptions,
        verifyPlugin,
        instrumentCode,
        buildPlugin,
        patchPluginXml,
    ).forEach {
        it {
            enabled = false
        }
    }
}