import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.playServices)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinSerialization)
}

val productionOrdersBasePath = "https://invoice-vg.tecodigi.com"

abstract class ValidateReleaseOrdersBasePathTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val platformConfig: RegularFileProperty

    @get:Input
    abstract val expectedOrdersBasePath: Property<String>

    init {
        group = "verification"
        description = "Fails release AAB builds when ReleaseConfigs.ordersBasePath is not production."
    }

    @TaskAction
    fun validate() {
        val platformConfigFile = platformConfig.get().asFile
        val configText = platformConfigFile.readText()
        val releaseConfigsBlock = Regex(
            pattern = """object\s+ReleaseConfigs\s*\{(.*?)^\}""",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        ).find(configText)?.groupValues?.get(1)
            ?: throw GradleException("Release AAB blocked: ReleaseConfigs was not found in $platformConfigFile.")

        val configuredOrdersBasePath = Regex(
            pattern = "(?m)^\\s*const\\s+val\\s+ordersBasePath\\s*:\\s*String\\s*=\\s*\"([^\"]+)\""
        ).find(releaseConfigsBlock)?.groupValues?.get(1)?.trimEnd('/')
            ?: throw GradleException("Release AAB blocked: ReleaseConfigs.ordersBasePath was not found in $platformConfigFile.")

        val expectedOrdersBasePath = expectedOrdersBasePath.get().trimEnd('/')
        if (configuredOrdersBasePath != expectedOrdersBasePath) {
            val host = runCatching { URI(configuredOrdersBasePath).host.orEmpty() }.getOrDefault("")
            val localUrlHint = if (isLocalBuildHost(host)) {
                " It looks like a local development URL."
            } else {
                ""
            }
            throw GradleException(
                "Release AAB blocked: ReleaseConfigs.ordersBasePath is \"$configuredOrdersBasePath\". " +
                    "Expected \"$expectedOrdersBasePath\".$localUrlHint"
            )
        }
    }

    private fun isLocalBuildHost(host: String): Boolean =
        host.equals("localhost", ignoreCase = true) ||
            host == "127.0.0.1" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.") ||
            Regex("""^172\.(1[6-9]|2\d|3[01])\.""").containsMatchIn(host)
}

val validateReleaseOrdersBasePath by tasks.registering(ValidateReleaseOrdersBasePathTask::class) {
    platformConfig.set(layout.projectDirectory.file("src/commonMain/kotlin/com/teco/ventago/Platform.kt"))
    expectedOrdersBasePath.set(productionOrdersBasePath)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val epsonIosHeadersDir = project.file("vendor/epson/ios/Headers")
    val epsonIosArm64Dir = project.file("vendor/epson/ios/libepos2-static.xcframework/ios-arm64")
    val epsonIosSimulatorDir = project.file("vendor/epson/ios/libepos2-static.xcframework/ios-arm64_x86_64-simulator")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main").cinterops.create("epos2") {
            defFile(project.file("src/nativeInterop/cinterop/epos2.def"))
            packageName("com.teco.ventago.vendor.epson")
            compilerOpts("-I${epsonIosHeadersDir.absolutePath}")
        }

        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            val epsonSliceDir = when (iosTarget.name) {
                "iosArm64" -> epsonIosArm64Dir
                else -> epsonIosSimulatorDir
            }
            linkerOpts(
                epsonSliceDir.resolve("libepos2.a").absolutePath,
                "-framework", "Foundation",
                "-framework", "UIKit",
                "-framework", "CoreGraphics",
                "-framework", "CoreImage",
                "-framework", "Security",
                "-framework", "SystemConfiguration"
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.android.firebase.bom))
            implementation(libs.android.firebase.messaging)
            implementation(libs.android.firebase.crashlytics)
            implementation(libs.android.firebase.analytics)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.navigation)
            implementation(libs.koin.android.compat)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.play.services.mlkit.barcode.scanning)

            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.fingerprint.android)
            implementation(libs.googleid)
            implementation(libs.imagekit.android)
            implementation(libs.google.android.places)
            implementation(libs.accompanist.permissions)
            implementation(libs.qr.gen)
            implementation(files("libs/epson/ePOS2.jar"))
            implementation(libs.ktor.client.cio)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.navigation.compose)
            implementation(libs.backhandler.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.database)
            implementation(libs.firebase.firestore)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor2)
            implementation(libs.sqlite.bundled)

            implementation(libs.compottie)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.richeditor.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        }

        appleMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.teco.ventago"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.teco.ventago"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 42
        versionName = "1.5.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

tasks.configureEach {
    if (name == "preReleaseBuild" || name == "bundleRelease" || (name.startsWith("bundle") && name.endsWith("Release"))) {
        dependsOn(validateReleaseOrdersBasePath)
    }
}
