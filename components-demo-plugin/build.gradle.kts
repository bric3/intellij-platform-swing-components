fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(project(":components"))
}

kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName = "IntelliJ Platform Swing Components Demo"
    version = properties("demoPlatformVersion")
    type = "IC"
    updateSinceUntilBuild = true
}

tasks {
    test {
        useJUnitPlatform()
    }

    runIde {
        jvmArgs(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+IgnoreUnrecognizedVMOptions",
            "-XX:+AllowEnhancedClassRedefinition" // for DCEVM
        )
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
    }

    listOf(
        runPluginVerifier,
        verifyPlugin,
        instrumentCode,
        assemble,
    ).forEach {
        it {
            enabled = false
        }
    }
}