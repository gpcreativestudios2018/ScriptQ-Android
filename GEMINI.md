# ScriptQ Android Project

## Overview
ScriptQ is a teleprompter and script management application for Android. It features a local library, a floating prompter widget, and a tiered business model (AdMob for free tier, RevenueCat for Premium).

## Architecture
- **Language:** Kotlin
- **UI Framework:** Android Views with ViewBinding
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Database:** Room (for local script storage)
- **Concurrency:** Kotlin Coroutines & Flow
- **Billing:** RevenueCat (Entitlement ID: `pro`)
- **Monetization:** Google Mobile Ads (AdMob Banner)

## Key Components
- `MainActivity`: Script library overview with FAB to create new scripts.
- `ScriptEditorActivity`: Interface for creating and saving scripts to Room.
- `FloatingPromptService`: Foreground service that manages the floating teleprompter widget with auto-scroll and playback controls.
- `PaywallActivity`: UI for upgrading to the Premium tier via RevenueCat.
- `App.kt`: Application class for global SDK initialization (RevenueCat).
- `BillingViewModel`: Shared ViewModel for managing billing state and offerings.

## Constraints & Business Logic
- **Free Tier:** 
  - Maximum of 3 scripts.
  - Banner ads visible in `MainActivity` and `ScriptEditorActivity`.
- **Premium Tier:**
  - Unlimited scripts.
  - Ads are hidden.
- **System Permissions:** Requires `SYSTEM_ALERT_WINDOW` for the floating prompter.

## Future Development
- Implement editing/deletion of existing scripts.
- Enhance the floating widget with more customization (text size, opacity).
- Securely store AdMob and RevenueCat API keys in `local.properties` or environment variables for production.

## Tooling
- **Gradle Version:** 8.3.1 (via `build.gradle.kts`)
- **Kotlin Version:** 1.9.22
- **GitHub CLI:** Used for repository management.
