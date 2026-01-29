<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/AirPods_Pro_icon.svg/1024px-AirPods_Pro_icon.svg.png" width="120" height="auto" />
</p>

# <h1 align="center">Podify</h1>

<p align="center">
  <strong>The Ultimate AirPods Companion for Android</strong>
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-1.9.0-purple?style=for-the-badge&logo=kotlin" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-blue?style=for-the-badge&logo=android" alt="Compose"></a>
  <a href="https://android.com"><img src="https://img.shields.io/badge/Android-14-green?style=for-the-badge&logo=android" alt="Android"></a>
</p>

<p align="center">
  Experience the magic of AirPods on Android. 
  <br>
  Beautiful animations, precise battery tracking, and seamless connectivity.
</p>

---

## ✨ Features

Podify brings the premium iOS experience to your Android device with a focus on aesthetics and functionality.

### 🔋 Precision Battery Monitoring
- **Real-time Stats**: View exact battery levels for Left, Right, and Case.
- **Charging Status**: Instantly see which pod is charging.
- **Hexway Parser**: Powered by advanced research for maximum accuracy (27-byte BLE parsing).

### 🎧 Smart Features
- **Auto Play/Pause**: Music automatically pauses when you take a pod out, and resumes when you put it back in.
- **Popup Animation**: A beautiful glassmorphic overlay appears the moment you open your case.
- **In-Ear Detection**: Advanced algorithms detect wearing state (In-Ear, Out-of-Case, Case-Open).

### 🎨 Premium Design
- **Glassmorphism**: Stunning UI with blur effects and transparency.
- **Dynamic Animations**: Smooth transitions and interactive elements.
- **Dark Mode**: Fully optimized for OLED screens.

---

## 📱 Visual Tour

| **Glassmorphic Dashboard** | **Smart Popup** |
|:---:|:---:|
| <div align="center"><i>Beautiful main screen with per-pod stats</i></div> | <div align="center"><i>Instant overlay when case opens</i></div> |
| ![Dashboard](https://via.placeholder.com/300x600/101010/FFFFFF?text=Dashboard+UI) | ![Popup](https://via.placeholder.com/300x600/101010/FFFFFF?text=Popup+Animation) |

---

## 🚀 Getting Started

### 1. Requirements
- **Android Device** (Android 8.0+)
- **AirPods** (Pro, Gen 2, Gen 3, Max)
- Bluetooth enabled

### 2. Installation
Clone the repo and build with Android Studio:
```bash
git clone https://github.com/Jnani-Smart/Podify.git
cd Podify
./gradlew installDebug
```

### 3. Grant Permissions
For the full experience, Podify needs a few permissions:
- **Bluetooth**: To find your AirPods.
- **Location**: Required by Android for BLE scanning.
- **Display Over Other Apps**: **Crucial** for the popup animation (Enable in App Settings).

---

## 🛠️ Under the Hood

Podify is built with modern Android tech stack:

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material3)
- **Architecture**: Clean Architecture (Service-based)
- **Bluetooth**: Custom Low-Latency BLE Scanner
- **Protocol**: Reverse-engineered Apple Proximity Pairing (0x07)

### BLE Parser Logic
We use the `0x07` Proximity Pairing message to decode:
```kotlin
Byte 5:  Wearing State (In-Ear, Case Open, etc.)
Byte 6:  Left Battery & Right Battery
Byte 7:  Case Battery & Charging Flags
```

---

## 🤝 Contributing

We welcome contributions! Whether it's a bug fix or a new feature, feel free to open a PR.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  Made with ❤️ for Audio Lovers
</p>
