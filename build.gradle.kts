// Archivo de build raíz: aquí solo se declaran las versiones de los plugins.
// Cada módulo (en este proyecto solo :app) decide cuáles aplica.
plugins {
    id("com.android.application") version "9.1.1" apply false
    // AGP 9 trae Kotlin integrado; el plugin de Compose fija la versión de Kotlin que se usa.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    // KSP genera en compilación el código de Room (DAOs y base de datos).
    id("com.google.devtools.ksp") version "2.3.13" apply false
}
