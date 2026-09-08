import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.net.URI

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.playServices)
    alias(libs.plugins.firebase.crashlytics)
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
    platformConfig.set(project(":composeApp").layout.projectDirectory.file("src/commonMain/kotlin/com/teco/ventago/Platform.kt"))
    expectedOrdersBasePath.set(productionOrdersBasePath)
}

android {
    namespace = "com.teco.ventago"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.teco.ventago"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 50
        versionName = "1.6.5"
        buildConfigField("boolean", "IS_POS_BUILD", "false")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("public") {
            dimension = "distribution"
            applicationId = "com.teco.ventago"
            versionNameSuffix = ""
            buildConfigField("boolean", "IS_POS_BUILD", "false")
        }
        create("pos") {
            dimension = "distribution"
            applicationId = "com.teco.ventago.pos"
            versionNameSuffix = "-pos"
            buildConfigField("boolean", "IS_POS_BUILD", "true")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.places)
    implementation(libs.imagekit.android)
    implementation(project.dependencies.platform(libs.android.firebase.bom))
    implementation(libs.android.firebase.analytics)
    implementation(libs.android.firebase.crashlytics)
    implementation(libs.android.firebase.messaging)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    add("posImplementation", project(":printer-h10p"))
    debugImplementation(compose.uiTooling)
}

tasks.configureEach {
    if (name == "preReleaseBuild" || name == "bundleRelease" || (name.startsWith("bundle") && name.endsWith("Release"))) {
        dependsOn(validateReleaseOrdersBasePath)
    }

    when (name) {
        "bundlePublicRelease" -> finalizedBy("uploadCrashlyticsMappingFilePublicRelease")
        "bundlePosRelease" -> finalizedBy("uploadCrashlyticsMappingFilePosRelease")
    }
}
