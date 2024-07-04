/*
 * IntelliJ Platform Swing Components
 *
 * Copyright (c) 2023 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
// gradleProperty do not find sub-project gradle.properties
// https://github.com/gradle/gradle/issues/23572
fun ProviderFactory.localGradleProperty(name: String): Provider<String> = provider {
    if (project.hasProperty(name)) project.property(name)?.toString() else null
}

plugins {
    `java-library`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.jetbrains.intellijPlatform)
    alias(libs.plugins.jetbrains.idea.ext)
}


group = "io.github.bric3.intellij-platform"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly(libs.annotations)
    api(libs.jsvg)

    intellijPlatform {
        create(
            providers.localGradleProperty("platformType"),
            providers.localGradleProperty("platformVersion")
        )

        pluginVerifier()
    }
}

kotlin {
    jvmToolchain(17)
}

// intellij {
//     pluginName = "ignored"
//     version = properties("platformVersion")
//     type = properties("platformType")
//
//     instrumentCode = false
// }

// // Read more:
// // * https://github.com/JetBrains/intellij-platform-gradle-plugin/
// // * https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
// intellijPlatform {
//     projectName.set("excalidraw-intellij-plugin")
//     pluginConfiguration {
//         id = providers.localGradleProperty("pluginId")
//         name = providers.localGradleProperty("pluginName")
//         version = providers.localGradleProperty("pluginVersion")
//
//         // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
//         description = providers.fileContents(rootProject.layout.projectDirectory.file("./README.md")).asText.map {
//             it.lines().run {
//                 val start = "<!-- Plugin description -->"
//                 val end = "<!-- Plugin description end -->"
//
//                 if (!containsAll(listOf(start, end))) {
//                     throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
//                 }
//                 subList(indexOf(start) + 1, indexOf(end))
//             }.joinToString("\n")
//         }.map {
//             markdownToHTML(it)
//         }
//
//         // Get the latest available change notes from the changelog file
//         changeNotes.set(provider {
//             changelog.renderItem(
//                 changelog.getLatest(),
//                 Changelog.OutputType.HTML
//             )
//         })
//
//         ideaVersion {
//             sinceBuild = providers.localGradleProperty("pluginSinceBuild")
//             untilBuild = provider { null } // removes until-build in plugin.xml
//         }
//
//         vendor {
//             name = providers.localGradleProperty("pluginVendor")
//             url = providers.localGradleProperty("pluginVendorUrl")
//         }
//     }
//
//     publishing {
//         token = providers.environmentVariable("PUBLISH_TOKEN")
//
//         // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
//         // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
//         // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//         channels = providers.localGradleProperty("pluginVersion").map {
//             Regex(".+-(\\[a-zA-Z]+).*")
//                 .find(it)
//                 ?.groupValues
//                 ?.getOrNull(1)
//                 ?: "default"
//         }.map { listOf(it) }
//     }
//
//     verifyPlugin {
//         ides {
//             ides(providers.localGradleProperty("pluginVerifierIdeVersions").map { it.split(',') }.getOrElse(emptyList()))
//             recommended()
//             // channels = listOf(ProductRelease.Channel.RELEASE)
//
//             select {
//                 types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
//                 channels = listOf(ProductRelease.Channel.RELEASE, ProductRelease.Channel.RC)
//                 sinceBuild = "223"
//                 untilBuild = "241.*"
//             }
//         }
//     }
//
//     buildSearchableOptions = false
//
// }

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

val baseName = "swing-components"
tasks {
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
            javaParameters = true
        }
    }

    withType<DokkaTask>().configureEach {
        notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
    }

    val javadocJar by registering(Jar::class) {
        // Note that we publish the Dokka HTML artifacts as Javadoc
        dependsOn(dokkaHtml)
        archiveBaseName.set(baseName)
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
        }
    }

    val sourceJar by registering(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set(baseName)
        archiveClassifier.set("sources")
        from(sourceSets.main.map(SourceSet::getAllSource))
        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
        }
    }

    assemble {
        dependsOn(javadocJar, sourceJar)
    }

    val jar by getting(Jar::class) {
        archiveBaseName.set(baseName)
        duplicatesStrategy = DuplicatesStrategy.FAIL
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        manifest {
            attributes["Implementation-Version"] = version
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Platform"] = attributes["Build-SDK"] ?: "unknown"
            // clear gradle-intellij-plugin keys
            attributes.keys.removeIf { it.startsWith("Build-") }
            println("Manifest attributes: ${attributes["Implementation-Title"]}")
        }

        from(file("$rootDir/LICENSE")) {
            rename("LICENSE", "LICENSE-$baseName")
        }
    }

    // this is a library for a plugin, not a plugin
    listOf(
        runIde,
        prepareSandbox,
        prepareTestSandbox,
        prepareTestIdeUiSandbox,
        prepareTestIdePerformanceSandbox,
        buildSearchableOptions,
        jarSearchableOptions,
        publishPlugin,
        signPlugin,
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

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter.get())
            dependencies {
                implementation.add(libs.assertj)
            }
        }

        withType(JvmTestSuite::class) {
            targets.configureEach {
                testTask.configure {
                    systemProperty("gradle.test.suite.report.location", reports.html.outputLocation.get().asFile)

                    testLogging {
                        showStackTraces = true
                        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

                        events = setOf(
                            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                        )
                    }
                }
            }
        }
    }
}
