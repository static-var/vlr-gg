# vlr-gg
Unofficial android app is written in Jetpack compose, which uses REST API that parses vlr.gg

## App overview

The app currently shows the following information
- News and news articles
- (Ongoing, upcoming and completed) matches and matche details
- (Ongoing, upcoming and completed) events and events details
- Team details

#### Additional features
- Users can sign up to get notified for their choice of matches a few minutes before it starts.
- Widget to see scores and updates on your home screen.
- App uses Material 3 theming.

## Dev Overview

The application has been written completely in [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose)

#### Libraries used:
- [Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3) - M3 opens up new possibilities for both brand colors and individual color preferences to converge in one-of-a-kind experiences. The color system embraces the need for color to reflect an app’s design sensibility, while also honoring the settings that individuals choose for themselves. 
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Kotlin solves Asynchronous or non-blocking programming problem in a flexible way by providing coroutine support at the language level and delegating most of the functionality to libraries.
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Hilt is a dependency injection library for Android that reduces the boilerplate of doing manual dependency injection in your project.
- [Room](https://developer.android.com/jetpack/androidx/releases/room) - The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite.
- [Firebase Analytics](https://firebase.google.com/docs/analytics) - Analytics is an app measurement solution, available at no charge, that provides insight on app usage and user engagement.
- [Firebase Messagign](https://firebase.google.com/docs/cloud-messaging) - Firebase Cloud Messaging (FCM) is a cross-platform messaging solution that lets you reliably send messages at no cost.
- [Ktor-client](https://ktor.io/docs/create-client.html) - Ktor is a framework to easily build connected applications
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) - Kotlin serialization consists of a compiler plugin, that generates visitor code for serializable classes, runtime library with core serialization API and support libraries with various serialization formats.
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - Navigation Compose enables you to navigate between composables while taking advantage of the Navigation component’s infrastructure and features.
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way. The ViewModel class allows data to survive configuration changes such as screen rotations.
- [Glance](https://developer.android.com/reference/kotlin/androidx/glance/package-summary) - Build layouts for remote surfaces using a Jetpack Compose-style API.
- [SplashScreen](https://developer.android.com/guide/topics/ui/splash-screen) - The SplashScreen API enables a new app launch animation for all apps when running on a device with Android 12 or higher. This includes an into-app motion at launch, a splash screen showing your app icon, and a transition to your app itself.
- [Material Icons](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons) - Material Design system icons as seen on Google Fonts.
- [Webkit](https://developer.android.com/reference/android/webkit/package-summary) - Provides tools for browsing the web.
- [Accompanist](https://google.github.io/accompanist/) - Accompanist is a group of libraries that aim to supplement Jetpack Compose with features that are commonly required by developers but not yet available.
  - System Ui Controller
  - Navigation Animation
  - Pager & Pager Indicators
  - Webview
- [Jsoup](https://jsoup.org/) - jsoup is a Java library for working with real-world HTML. It provides a very convenient API for fetching URLs and extracting and manipulating data, using the best of HTML5 DOM methods and CSS selectors.
- [Landscapist-glide](https://github.com/skydoves/landscapist) - Jetpack Compose image loading library which fetches and displays network images.