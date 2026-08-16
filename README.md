# Sony TV 音量限制

這是一個只供單一 Sony Google TV 使用的小型 Android TV App，用來限制
`STREAM_MUSIC` 的最高音量。

## 建置

本專案只透過 GitHub Actions 建置，不在本機執行 Gradle。Workflow 會依序執行單元測試、
Android lint 與 `assembleDebug`，再將 `app-debug.apk` 上傳為
`sony-tv-volume-limiter-debug` artifact。

不需要安裝 Android CLI。Android SDK 套件由 `sdkmanager` 管理，APK 使用專案內提交的
Gradle Wrapper 建置。

## 安裝

1. 在 GitHub Actions 執行 **Android CI** workflow。
2. 下載 `sony-tv-volume-limiter-debug` artifact。
3. 將 `app-debug.apk` 側載到 Sony TV。
4. 開啟 App 並選擇最高音量。
5. 建立四位數 PIN，儲存音量上限。
6. 開啟無障礙設定並啟用 **Sony TV 音量限制服務**。

App 只限制 Android 媒體音量。HDMI ARC、HDMI CEC、AV Receiver、Soundbar 與部分藍牙
裝置可能在 Android 之外控制音量，因此不保證會受到限制。
