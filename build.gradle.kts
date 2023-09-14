import io.gitlab.arturbosch.detekt.Detekt

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.qodana)
}

val detektPlugins: Configuration = configurations.getByName("detektPlugins")

dependencies {
    detektPlugins(libs.bundles.detektplugins)
}

repositories {
    mavenCentral()
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

tasks {
    val detektAll by registering(Detekt::class) {
        source(files(project.projectDir))

        include("**/*.kt")
        include("**/*.kts")
        exclude("**/build/**")
        exclude("**/generated/**")
        exclude("**/testData/**")

        config.setFrom("$rootDir/.detekt/detekt.yml")
        buildUponDefaultConfig = false
        autoCorrect = true
    }
}