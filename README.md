# Shadow Fiend Hook


![Frame 48](https://github.com/user-attachments/assets/9efde498-921b-4bd1-bce1-2248e0af354b)

### Lightweight and powerful Android app hooking tool

## Features

+ Support Android 5.0 - 15 Beta2 (API level 21 - 35)
+ Support armeabi-v7a, arm64-v8a, x86, x86-64, riscv64
+ Low detection threshold for reverse engineering
+ Kotlin style hooking

## Usage

Include the dependency in your build.gradle file

```gradle
dependencies {
    implementation ("com.skiy.sf:hook:1.0")
}
```

Intercepting a method before it is executed

```kotlin
OkHttpClient::class.java.getDeclaredMethod("newCall", Request::class.java) beforeMethodCalled { interceptedParams ->
   println(interceptedParams.args)
}
```
