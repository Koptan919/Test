plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// App identity lives in gradle.properties so the app can be renamed in one place.
val appName: String = providers.gradleProperty("noor.appName").get()
val appNameArabic: String = providers.gradleProperty("noor.appNameArabic").get()
val appId: String = providers.gradleProperty("noor.applicationId").get()

android {
    namespace = "com.noor.app"
    compileSdk = 35

    defaultConfig {
        applicationId = appId
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("noor.versionCode").get().toInt()
        versionName = providers.gradleProperty("noor.versionName").get()

        resValue("string", "app_name", appName)
        buildConfigField("String", "APP_NAME_EN", "\"$appName\"")
        buildConfigField("String", "APP_NAME_AR", "\"$appNameArabic\"")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Localized launcher label generated from gradle.properties (values-ar/app_name).
abstract class GenerateLocalizedAppName : DefaultTask() {
    @get:Input abstract val arabicName: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val escaped = arabicName.get().replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'")
        val file = outputDir.get().dir("values-ar").file("app_name.xml").asFile
        file.parentFile.mkdirs()
        file.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <string name=\"app_name\">$escaped</string>\n</resources>\n")
    }
}

val generateLocalizedAppName = tasks.register<GenerateLocalizedAppName>("generateLocalizedAppName") {
    arabicName.set(appNameArabic)
    outputDir.set(layout.buildDirectory.dir("generated/res/appName"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(generateLocalizedAppName, GenerateLocalizedAppName::outputDir)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
