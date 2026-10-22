plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val firebaseFile = file("google-services.json")
val firebaseRoot = if(firebaseFile.isFile) groovy.json.JsonSlurper().parse(firebaseFile) as Map<*,*> else emptyMap<Any,Any>()
val firebaseProject = firebaseRoot["project_info"] as? Map<*,*> ?: emptyMap<Any,Any>()
val firebaseClient = (firebaseRoot["client"] as? List<*>)?.filterIsInstance<Map<*,*>>()?.firstOrNull {
    ((it["client_info"] as? Map<*,*>)?.get("android_client_info") as? Map<*,*>)?.get("package_name")=="kz.youni4st.app"
} ?: emptyMap<Any,Any>()
fun firebaseString(value:Any?) = groovy.json.JsonOutput.toJson(value?.toString().orEmpty())

android {
    namespace = "kz.youni4st.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "kz.youni4st.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 16
        versionName = "0.7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://youni4st-beckend.onrender.com\"")
        buildConfigField("String","FIREBASE_API_KEY",firebaseString(((firebaseClient["api_key"] as? List<*>)?.firstOrNull() as? Map<*,*>)?.get("current_key")))
        buildConfigField("String","FIREBASE_APP_ID",firebaseString((firebaseClient["client_info"] as? Map<*,*>)?.get("mobilesdk_app_id")))
        buildConfigField("String","FIREBASE_PROJECT_ID",firebaseString(firebaseProject["project_id"]))
        buildConfigField("String","GOOGLE_WEB_CLIENT_ID",firebaseString((firebaseClient["oauth_client"] as? List<*>)?.filterIsInstance<Map<*,*>>()?.firstOrNull {it["client_type"].toString()=="3"}?.get("client_id")))
    }
    signingConfigs {
        getByName("debug") {
            providers.gradleProperty("debugKeystore").orNull?.let { storeFile = file(it) }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled=true
            isShrinkResources=true
            signingConfig=signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
