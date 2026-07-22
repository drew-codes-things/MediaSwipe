buildscript {
    dependencies {
        // AGP 9's built-in Kotlin support otherwise bundles an older KGP
        // whose compiler can't read metadata from newer Kotlin stdlib
        // versions pulled in transitively (e.g. by Coil/coroutines).
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
