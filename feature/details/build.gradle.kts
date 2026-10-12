plugins {
  id("skydoves.pokedex.android.feature")
  id("skydoves.pokedex.android.hilt")
}

android {
  namespace = "com.skydoves.pokedex.compose.feature.details"
}

dependencies {
  testImplementation(projects.core.test)
  testImplementation(libs.turbine)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.kotlinx.coroutines.test)
}
