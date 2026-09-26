plugins { java }

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.9.1")
    testImplementation("junit:junit:4.13.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Test the actual Core binary selected by the app's current dependency graph.
val coreFixture by configurations.creating
dependencies { coreFixture("androidx.core:core:1.18.0@aar") }
val coreFixtureFile = coreFixture.singleFile
tasks.test {
    inputs.file(coreFixtureFile)
    systemProperty("androidxCoreAar", coreFixtureFile.absolutePath)
}
