# AGENTS.md

## 專案目標

- 本專案是供單一 Sony Google TV 側載使用的個人 App。
- 核心功能只有限制 `AudioManager.STREAM_MUSIC` 的最高音量。
- 使用 `AccessibilityService` 攔截遙控器音量增加鍵，並以週期性檢查補救其他來源造成的超限。
- 這不是企業管理或不可繞過的家長控制方案；HDMI ARC、CEC、外接音響與部分藍牙裝置不在保證範圍內。

## 溝通

- 使用正體中文與使用者溝通。
- 說明保持直接、精簡，清楚交代變更、驗證結果與仍需 Sony TV 實測的風險。

## 開發限制

- 不得在目前工作環境執行 `gradle`、`./gradlew`、`gradlew.bat`、Android Studio、模擬器或任何 APK/AAB build。
- 所有 `test`、`lint` 與 `assembleDebug` 必須由 `.github/workflows/android.yml` 在 GitHub Actions 執行。
- 本機只允許不會建立 Android build 產物的靜態檢查，例如讀取檔案、解析 XML、檢查 Git diff。
- Android CLI 不是必要建置工具；除非使用者明確要求，否則不要安裝、初始化或加入 workflow。
- 不要全域安裝 Python package；必要時使用 `uv run --with`。
- Windows-only 操作使用 PowerShell，不使用 `cmd.exe`，PowerShell 指令不要使用 `&&`。

## 實作原則

- 維持 Kotlin + 傳統 XML View，不為這個單機工具加入 Compose、後端或額外架構層。
- 保持最小功能，不加入多裝置、每 App 設定、網路遙控、亮度、Device Admin 或前景服務。
- 優先使用 Android 公開 API；不要依賴 root、隱藏 API 或廠商私有 API。
- 啟動與執行時必須尊重 `AudioManager.isVolumeFixed()`，不能在不支援的裝置上顯示限制已生效。
- 音量增加按鍵的 down/up 事件必須一致處理，避免產生不完整的按鍵事件流。
- PIN 只保護 App 內設定，不宣稱能防止停用 AccessibilityService、清除資料或解除安裝。
- 新增依賴、權限或背景執行機制前，先確認它對這台 Sony TV 的核心需求確實必要。

## CI 與測試

- GitHub Actions 固定使用 `ubuntu-24.04`、JDK 17、Android SDK 35 與專案提交的 Gradle Wrapper。
- 變更音量計算、按鍵處理或 PIN 邏輯時，同步新增或更新 JVM 單元測試。
- 推送後要確認 GitHub Actions 的 `test`、`lint`、`assembleDebug` 與 artifact 上傳全部成功。
- GitHub-hosted runner 無法驗證 Sony 遙控器、CEC 或實際音訊路由；這些行為必須標記為實機測試項目。

## Git

- Commit message 使用 Conventional Commits，例如 `feat: ...`、`fix: ...`、`docs: ...`。
- Commit 保持小而單一目的，不混入無關重構或格式調整。
- 不使用 force push，不重寫已推送歷史，也不覆蓋使用者未提交的變更。
