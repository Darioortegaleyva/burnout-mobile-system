// Build script de nivel de proyecto.
// Los plugins se declaran aquí con apply=false y se aplican en el módulo :app.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // El plugin de Compose Compiler pasó a ser un plugin de Kotlin a partir de Kotlin 2.0
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
