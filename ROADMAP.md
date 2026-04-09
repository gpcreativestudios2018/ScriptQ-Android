# ScriptQ Android — Execution Roadmap

> **Last updated:** 2026-04-09
> **Current state:** Pre-release, not shippable
> **Product:** Android teleprompter + script workflow app
> **Business model target:** Free + Pro subscription + annual + lifetime

---

## Reality Check

The repo contains a solid MVP foundation, but several roadmap items previously marked complete are only partially implemented or not release-ready.

### Implemented but not product-complete
- Script library with create, edit, delete, duplicate, search, sort, and favorites
- Floating overlay prompter with auto-scroll, mirror, invert, text size, opacity, countdown
- Basic paywall and RevenueCat wiring
- Settings, onboarding, and tablet-specific layouts
- Rehearsal recording MVP
- WiFi-only remote control MVP
- Voice-reactive scrolling MVP

### Not yet shippable because of product and engineering gaps
- Build and release confidence is weak
- Toolbar/navigation wiring is incomplete
- Premium feature gating is inconsistent with pricing strategy
- Permission handling is too loose for onboarding and overlay flows
- Several “complete” features need hardening before Play Store launch
- Monetization, analytics, crash reporting, and accessibility are missing

---

## Phase 0 — Release Blockers

**Goal:** Make the app buildable, testable, and safe to ship internally.

### 0.1 — Build and Repo Hygiene
- [ ] Fix current compile issues and missing imports
- [ ] Add Gradle wrapper to the repository
- [ ] Verify debug and release builds on a clean machine
- [ ] Add baseline ProGuard / R8 verification for RevenueCat, Room, and Ads
- [ ] Remove dead imports and placeholder code paths

### 0.2 — Navigation and App Shell
- [ ] Wire `MaterialToolbar` correctly in `MainActivity`
- [ ] Ensure search, sort, settings, and remote actions are visible and working
- [ ] Fix Settings screen navigation and top app bar behavior
- [ ] Standardize titles and back navigation across activities

### 0.3 — Permissions and Service Reliability
- [ ] Fix onboarding so completion requires a real post-check of overlay permission
- [ ] Add robust handling for denied mic permission and denied overlay permission
- [ ] Harden foreground service startup and re-entry behavior
- [ ] Ensure overlay widget cleanly updates when launching multiple scripts

### 0.4 — Observability and Stability
- [ ] Add Firebase Crashlytics or Sentry
- [ ] Add Timber or structured logging
- [ ] Replace all placeholder billing/logging callbacks with real error handling
- [ ] Add basic non-fatal logging around billing, imports, recording, and remote control

### 0.5 — Tests and CI
- [ ] Add GitHub Actions build verification on every push
- [ ] Add instrumentation tests for onboarding, save/edit/delete, and paywall load
- [ ] Add regression coverage for script sorting, search, and favorites

**Exit criteria:** Any collaborator can clone the repo, build it, run it, and validate core flows with confidence.

---

## Phase 1 — Make The Existing MVP Actually Good

**Goal:** Upgrade current features from “exists” to “high quality.”

### 1.1 — Script Library UX
- [ ] Display useful metadata in the library: updated date, word count, estimated read time
- [ ] Add empty-state CTA that launches new script creation directly
- [ ] Improve long-press menu anchoring so it opens from the selected row, not the FAB
- [ ] Add bulk actions groundwork for future organization features
- [ ] Improve free-tier messaging when the 3-script limit is hit
- [ ] Add contextual onboarding / one-time tooltips for high-friction controls
- [ ] Add first-run guidance for `New Script`, `Prompt`, search, sort, and premium-only actions
- [ ] Persist tooltip dismissal state so hints teach once and then get out of the way

### 1.2 — Script Editor Quality
- [ ] Preserve and render rich text more reliably
- [ ] Audit HTML round-trip behavior for formatting edge cases
- [ ] Improve import/export failure messaging
- [ ] Support unsaved changes warning on back
- [ ] Add autosave draft behavior
- [ ] Add proper WPM-based read-time preference instead of fixed 150 WPM

### 1.3 — Prompter Hardening
- [ ] Improve speed model so controls feel linear and predictable
- [ ] Add tap-to-show minimal controls and immersive reading mode
- [ ] Add progress indicator or scroll position feedback
- [ ] Add word count/read-time display inside the prompter if roadmap still requires it
- [ ] Improve tablet prompter ergonomics and landscape behavior

### 1.4 — Rehearsal Mode Hardening
- [ ] Replace naive WPM calculation with spoken-progress or session-progress approximation
- [ ] Save rehearsal metadata alongside recording file
- [ ] Add file management for rehearsal recordings
- [ ] Handle recorder errors and interruptions cleanly

### 1.5 — Remote Control Hardening
- [ ] Make remote discovery/connection lifecycle reliable
- [ ] Handle reconnects, disconnects, and duplicate discoveries
- [ ] Improve controller UI states: searching, connecting, connected, failed
- [ ] Add actual section/position jumping instead of reset-only control

### 1.6 — Voice Pacing Hardening
- [ ] Replace simple RMS threshold scrolling with real pace-follow logic
- [ ] Add calibration flow for mic sensitivity / speaking style
- [ ] Add visible voice pacing state and confidence indicator
- [ ] Ensure manual controls and voice mode transition cleanly

**Exit criteria:** Existing features feel intentional, reliable, and demo-ready.

---

## Phase 2 — Monetization Alignment

**Goal:** Make Pro valuable and enforce the value proposition correctly.

### 2.1 — Entitlement Enforcement
- [ ] Audit every feature against the Free vs Pro matrix
- [ ] Gate mirror mode behind Pro
- [ ] Gate voice-paced scrolling behind Pro
- [ ] Gate import/export behind Pro if that remains the pricing decision
- [ ] Gate remote control behind Pro
- [ ] Gate rehearsal mode behind Pro
- [ ] Gate rich text formatting behind Pro if that remains the pricing decision
- [ ] Add graceful upgrade prompts instead of silent access or hard failure

### 2.2 — Paywall Redesign
- [ ] Replace single-package paywall with monthly vs annual selection
- [ ] Add feature comparison: Free vs Pro
- [ ] Add annual savings framing
- [ ] Add trial support if enabled in RevenueCat
- [ ] Add lifetime purchase option if retained in pricing strategy
- [ ] Add restore purchases and manage subscription affordances in the paywall itself

### 2.3 — Ad Strategy
- [ ] Replace test interstitial logic with production-safe placement and pacing
- [ ] Confirm banner and interstitial unit separation via config
- [ ] Track ad impressions and prompt-session conversion impact

**Exit criteria:** Pricing, entitlement checks, and upgrade UX all match the actual product.

---

## Phase 3 — Missing Roadmap Features

**Goal:** Finish the roadmap items that are still absent.

### 3.1 — Import / Export
- [ ] Import from `.docx`
- [ ] Export/share richer formats if needed

### 3.2 — Script Formatting and Prompting Features
- [ ] Color-code sections
- [ ] Highlight cue words during prompting
- [ ] Cue markers and pause points

### 3.3 — Organization
- [ ] Folders / categories
- [ ] Tags / labels
- [ ] Filter chips for favorites, folders, tags, recent

### 3.4 — Rehearsal Expansion
- [ ] Playback recorded audio synced with script position
- [ ] Session history with saved stats
- [ ] Improvement trends over time

### 3.5 — Distribution and Platform Reach
- [ ] Android home screen widget
- [ ] Deep links for scripts and sharing
- [ ] Chromebook optimization
- [ ] Wear OS remote companion

### 3.6 — Accessibility
- [ ] TalkBack support throughout
- [ ] High contrast mode
- [ ] Respect system font scale
- [ ] Reduced motion option

**Exit criteria:** The published roadmap is materially complete rather than aspirational.

---

## Phase 4 — Cloud and Collaboration

**Goal:** Turn ScriptQ into a cross-device workflow product.

### 4.1 — Accounts
- [ ] Google Sign-In
- [ ] Account screen with sign-in/sign-out
- [ ] Link RevenueCat customer identity to app account identity

### 4.2 — Sync
- [ ] Choose backend: Firebase Firestore or Supabase
- [ ] Keep Room as offline-first local source
- [ ] Add debounced auto-sync on save
- [ ] Add conflict resolution strategy
- [ ] Add visible sync status and error handling

### 4.3 — Collaboration
- [ ] Share script via link
- [ ] View-only and editable permissions
- [ ] Shared libraries / team workspaces
- [ ] Comments and notes on scripts
- [ ] Presence / co-editing only after sync foundations are stable

**Exit criteria:** Users can trust ScriptQ across devices and across collaborators.

---

## Phase 5 — Growth Features That Actually Differentiate

**Goal:** Build a category leader, not just another teleprompter.

### 5.1 — Creator Workflow Features
- [ ] Camera-session mode with safe margins and framing guides
- [ ] Script sections, beats, and jump points for real shoots
- [ ] Pace presets: conversational, keynote, podcast, ad read
- [ ] Per-script settings profiles
- [ ] Better tablet and stand-mounted workflows

### 5.2 — AI-Assisted Features
- [ ] Rewrite for clarity / brevity
- [ ] Convert rough notes into a spoken script
- [ ] Estimate read time from the user’s actual rehearsal pace
- [ ] Filler-word detection after rehearsal
- [ ] Emphasis suggestions and cue extraction
- [ ] “Practice coach” summary after each session

### 5.3 — Analytics and Retention
- [ ] Firebase Analytics integration
- [ ] Funnel tracking: onboarding, script creation, prompt start, paywall, conversion
- [ ] Personal stats dashboard
- [ ] Practice streaks and reminders
- [ ] Win-back and upgrade experiments

### 5.4 — Brand and Distribution
- [ ] Play Store listing assets
- [ ] Promo video and product demo flows
- [ ] Referral or sharing loops
- [ ] Landing page / waitlist / creator-focused marketing site

**Exit criteria:** ScriptQ feels like a creator platform with defensible value.

---

## Product Positioning

### Free
- Up to 3 scripts
- Basic library
- Basic auto-scroll
- Ads

### Pro
- Unlimited scripts
- No ads
- Mirror mode
- Voice-paced scrolling
- Import / export
- Remote control
- Rehearsal mode
- Rich formatting
- Cloud sync
- Collaboration
- Organization features

### Future upsell candidates
- Team workspace tier
- AI coaching add-on
- Creator studio bundle

---

## Technical Direction

### Architecture
- [ ] Introduce dependency injection with Hilt
- [ ] Reduce Activity bloat by extracting service, billing, and editor domain logic
- [ ] Introduce a clearer data/domain/ui separation

### UI Stack
- [ ] Continue shipping in Views for speed
- [ ] Start incremental migration to Jetpack Compose only after release blockers are solved

### Data
- [ ] Replace destructive migration with real Room migrations
- [ ] Add schema export
- [ ] Add rehearsal/session data models
- [ ] Prepare data model for sync and collaboration

### Quality Bar
- [ ] No placeholder error handling
- [ ] No roadmap item marked complete unless it is user-complete
- [ ] No premium feature shipped without entitlement enforcement
- [ ] No major feature without instrumentation coverage for the happy path

---

## Immediate Next Sequence

1. Fix build, toolbar, onboarding permission flow, and release blockers.
2. Harden existing MVP features so the current app feels polished.
3. Align paywall, entitlements, and ads with the actual pricing model.
4. Implement missing roadmap items with the highest leverage first.
5. Add cloud sync and collaboration.
6. Build differentiated creator and AI features on top of a stable core.
