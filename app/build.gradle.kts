plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.afyatrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.afyatrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("androidx.activity:activity:1.9.3")
    implementation ("com.google.firebase:firebase-auth:22.3.1")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.firebase:firebase-auth:23.0.0")
    implementation ("com.google.android.gms:play-services-auth:21.4.0")

    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    // Lottie for animations (optional)
    implementation ("com.airbnb.android:lottie:6.1.0")

    implementation ("com.squareup.picasso:picasso:2.71828")
    // Add the SQLite dependencies
    implementation ("androidx.sqlite:sqlite:2.4.0")
    implementation ("androidx.sqlite:sqlite-framework:2.4.0")
    // Firebase
    implementation ("com.google.firebase:firebase-bom:32.1.0")
    implementation ("com.google.firebase:firebase-auth")

    implementation ("androidx.core:core:1.9.0")
    implementation ("androidx.core:core-ktx:1.9.0")

    implementation ("com.squareup.okhttp3:okhttp:4.9.0" )  // For networking
    implementation ("org.simpleframework:simple-xml:2.7.1") // For XML parsing

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.google.firebase:firebase-messaging:23.3.1")

    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.2")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.2")
    implementation(libs.firebase.database)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}