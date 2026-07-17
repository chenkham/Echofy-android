# Implementation Plan: App Improvements and Fixes

## Overview

This implementation plan covers eight distinct improvements to the Echofy Android music streaming application. The tasks are organized by priority and follow the phased approach outlined in the design document. Each task builds incrementally and references specific requirements for traceability.

## Tasks

### Phase 1: Bug Fixes and Core Improvements

- [x] 1. Fix Together Session join functionality
  - [x] 1.1 Debug and analyze current join flow issues
    - Review `AppwriteTogetherSessionManager.joinSession()` method
    - Identify timing issues in room document fetching
    - Analyze realtime subscription race conditions
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Implement eager room document fetching
    - Modify `joinSession()` to fetch room document before subscribing to realtime updates
    - Add synchronous application of initial playback state
    - Ensure room metadata is available before proceeding
    - _Requirements: 1.1, 1.2_

  - [x] 1.3 Add retry logic with exponential backoff
    - Implement retry mechanism for room document fetching
    - Add exponential backoff strategy (initial: 500ms, max: 5s)
    - Handle timeout scenarios gracefully
    - _Requirements: 1.1, 1.3_

  - [x] 1.4 Enhance error handling and user messages
    - Create `TogetherSessionError` sealed class hierarchy
    - Implement `toUserMessage()` extension function
    - Map Appwrite exceptions to user-friendly error messages
    - Display descriptive errors in UI
    - _Requirements: 1.3_

  - [ ]* 1.5 Write unit tests for Together Session join logic
    - Test room code validation
    - Test error message mapping
    - Mock Appwrite responses for various scenarios
    - Test retry logic with different failure patterns
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Implement queue management improvements
  - [x] 2.1 Add remove functionality to MusicService
    - Create `removeFromQueue(mediaItemIndex: Int)` method in MusicService
    - Implement ExoPlayer `removeMediaItem()` integration
    - Update queue state flow after removal
    - _Requirements: 4.1, 4.2, 4.5_

  - [x] 2.2 Filter currently playing song from queue display
    - Modify Queue composable to exclude current media item
    - Implement filtering logic based on `mediaId` comparison
    - Ensure queue indices remain correct after filtering
    - _Requirements: 4.3_

  - [x] 2.3 Add remove button UI to queue items
    - Add remove icon button to each QueueItem composable
    - Wire button click to `removeFromQueue()` method
    - Add confirmation for remove action (optional)
    - _Requirements: 4.1, 4.2_

  - [x] 2.4 Ensure queue updates within 500ms
    - Optimize queue state flow emissions
    - Test update latency on various devices
    - Add performance logging if needed
    - _Requirements: 4.4_

  - [ ]* 2.5 Write unit tests for queue management
    - Test queue filtering logic (exclude current song)
    - Test remove operation index calculations
    - Test queue state updates after removal
    - Test edge cases (empty queue, single item)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 2: UI/UX Enhancements

- [x] 4. Clean up Settings UI
  - [x] 4.1 Remove horizontal dividers from settings screens
    - Locate all `Divider()` composables in SettingsScreen.kt
    - Remove divider components between settings items
    - _Requirements: 2.1_

  - [x] 4.2 Remove arrow icons from settings items
    - Remove trailing icon parameters from settings item composables
    - Update all settings categories and items
    - _Requirements: 2.2_

  - [x] 4.3 Adjust spacing and maintain functionality
    - Increase vertical padding between items for visual separation
    - Verify all settings remain clickable and navigable
    - Test navigation to sub-settings screens
    - _Requirements: 2.3, 2.4_

  - [ ]* 4.4 Write UI tests for settings screen
    - Verify dividers are removed
    - Verify arrow icons are removed
    - Verify all settings remain clickable
    - Test navigation flows
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 5. Implement Apple Music-style player scroll behavior
  - [ ] 5.1 Create ScrollAwarePlayerController
    - Implement scroll detection logic with threshold (100 pixels)
    - Create `PlayerVisibility` sealed class
    - Implement `onScroll()` method to track scroll offset
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6_

  - [ ] 5.2 Add player visibility animations
    - Implement hide/show animations with 250ms duration
    - Use FastOutSlowInEasing for natural feel
    - Animate translate Y offset and alpha properties
    - _Requirements: 3.5_

  - [ ] 5.3 Integrate scroll behavior in Lyrics section
    - Add scroll listener to lyrics LazyColumn
    - Connect to ScrollAwarePlayerController
    - Test hide on scroll down, show on scroll up
    - _Requirements: 3.1, 3.2_

  - [ ] 5.4 Integrate scroll behavior in Queue section
    - Add scroll listener to queue LazyColumn
    - Connect to ScrollAwarePlayerController
    - Test hide on scroll down, show on scroll up
    - _Requirements: 3.3, 3.4_

  - [ ]* 5.5 Write UI tests for player scroll behavior
    - Test scroll detection triggers hide/show
    - Test animation completes within time bounds
    - Test player visibility state transitions
    - Test on both lyrics and queue sections
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 6. Improve Together Section UI
  - [ ] 6.1 Disable swipe-down gesture for Together Section
    - Modify EchofyJamSheet to disable swipe dismiss
    - Remove drag handle from ModalBottomSheet
    - Set `shouldDismissOnBackPress` to false
    - _Requirements: 7.1_

  - [ ] 6.2 Add explicit close button at top left
    - Add IconButton with Close icon at top left
    - Wire button to dismiss callback
    - Ensure button is always visible and accessible
    - _Requirements: 7.2_

  - [ ] 6.3 Scale player to full screen height
    - Use `Modifier.fillMaxHeight()` for main container
    - Use `Modifier.weight(1f)` for player content
    - Fill bottom empty space with background color
    - _Requirements: 7.3, 7.4_

  - [ ] 6.4 Verify touch responsiveness
    - Test all controls remain accessible
    - Verify no touch target overlaps
    - Test on various screen sizes
    - _Requirements: 7.5_

  - [ ]* 6.5 Write UI tests for Together Section
    - Verify swipe-down gesture is disabled
    - Verify close button functionality
    - Verify player fills screen height
    - Test touch responsiveness
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 3: Subscription and Premium Features

- [ ] 8. Simplify premium subscription options
  - [ ] 8.1 Update SubscriptionManager product IDs
    - Define only two product IDs: `premium_monthly_15` and `premium_2year_399`
    - Remove references to old subscription tiers
    - Update product query logic
    - _Requirements: 5.1, 5.2_

  - [ ] 8.2 Simplify PremiumScreen UI
    - Display only 2 subscription options (₹15/month, ₹399/2 years)
    - Remove exaggerated feature descriptions
    - Update feature list to include only: ad-free, offline downloads, high-quality audio, Together Sessions, Premium Labs
    - _Requirements: 5.1, 5.3, 5.4_

  - [ ] 8.3 Test subscription purchase flows
    - Test monthly subscription purchase
    - Test 2-year subscription purchase
    - Verify subscription status updates correctly
    - Test on Google Play Billing test environment
    - _Requirements: 5.5_

  - [ ]* 8.4 Write unit tests for subscription logic
    - Test product ID filtering
    - Test subscription status updates
    - Mock Google Play Billing responses
    - Test error handling for billing failures
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 9. Implement Premium Labs promo code feature
  - [ ] 9.1 Create PromoCodeRedemption UI component
    - Create new composable in `ui/screens/premium/PromoCodeRedemption.kt`
    - Add promo code input field with validation
    - Add submit button and loading state
    - Display redemption status messages
    - _Requirements: 8.1, 8.4, 8.6_

  - [ ] 9.2 Implement promo code format validation
    - Create `validatePromoCodeFormat()` function
    - Check for minimum length (6 characters)
    - Validate character set (A-Z, 0-9, hyphen)
    - Return validation result with error messages
    - _Requirements: 8.1, 8.4_

  - [ ] 9.3 Add promo code redemption to SubscriptionManager
    - Create `redeemPromoCode(code: String)` method
    - Integrate with Google Play Billing API
    - Handle redemption response and status
    - Update subscription status on success
    - _Requirements: 8.2, 8.3, 8.7_

  - [ ] 9.4 Implement error handling for promo codes
    - Create `RedemptionStatus` enum (PENDING, SUCCESS, INVALID_CODE, ALREADY_REDEEMED, EXPIRED, ERROR)
    - Map billing errors to redemption statuses
    - Display user-friendly error messages
    - _Requirements: 8.4_

  - [ ] 9.5 Integrate promo code UI into Premium Labs
    - Add PromoCodeRedemption component to Premium Labs screen
    - Wire up ViewModel and state management
    - Ensure redemption status displays within 5 seconds
    - _Requirements: 8.1, 8.5, 8.6_

  - [ ]* 9.6 Write unit tests for promo code feature
    - Test promo code format validation
    - Test redemption status mapping
    - Mock Google Play Billing responses
    - Test error scenarios (invalid, expired, already redeemed)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6, 8.7_

  - [ ]* 9.7 Write integration tests for promo code redemption
    - Test full redemption flow with test codes
    - Test subscription status update after redemption
    - Test error handling with invalid codes
    - _Requirements: 8.2, 8.3, 8.5, 8.7_

- [ ] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 4: Widget Enhancement

- [ ] 11. Enhance compact music widget
  - [ ] 11.1 Optimize widget layout for 2x2 grid
    - Update `widget_music_compact.xml` layout
    - Set dimensions: minWidth=110dp, minHeight=110dp, targetWidth=180dp, targetHeight=180dp
    - Arrange elements: logo + song info (top), album art (middle), controls (bottom)
    - _Requirements: 6.1, 6.2_

  - [ ] 11.2 Add app logo to widget
    - Add ImageView for app logo in top row
    - Position logo next to song title
    - Ensure logo is visible but not overwhelming
    - _Requirements: 6.2_

  - [ ] 11.3 Implement widget control functionality
    - Wire play/pause button to toggle playback
    - Wire next button to skip to next song
    - Wire previous button to skip to previous song
    - Test all button actions
    - _Requirements: 6.3, 6.4, 6.5_

  - [ ] 11.4 Implement widget update mechanism
    - Create `updateAllWidgets()` method
    - Update song title, artist, and cover art
    - Update play/pause button state
    - Ensure updates occur within 2 seconds of song change
    - _Requirements: 6.6_

  - [ ] 11.5 Improve widget visual design
    - Apply minimalist design principles
    - Ensure text is readable on various backgrounds
    - Optimize image loading for cover art
    - Test on multiple launcher apps
    - _Requirements: 6.7_

  - [ ]* 11.6 Write widget tests
    - Test widget layout in 2x2 grid
    - Test control button functionality
    - Test widget updates on song change
    - Measure update latency (target: <2s)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [ ] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 5: Integration and Polish

- [ ] 13. Integration testing and verification
  - [ ] 13.1 Test Together Session end-to-end flow
    - Test guest join with real room codes
    - Verify playback synchronization accuracy (within 2 seconds)
    - Test error messages for various failure scenarios
    - Test on multiple devices simultaneously
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 13.2 Test subscription flows end-to-end
    - Test purchase flows for both subscription tiers
    - Test promo code redemption with test codes from Google Play Console
    - Verify subscription status updates correctly
    - Test subscription persistence across app restarts
    - _Requirements: 5.1, 5.2, 5.5, 8.2, 8.3, 8.7_

  - [ ] 13.3 Test widget on various devices and launchers
    - Test widget on Android 8.0, 10, 12, 14
    - Test on different launcher apps (Pixel Launcher, Nova, etc.)
    - Verify widget updates within 2 seconds
    - Test battery impact of widget updates
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [ ] 13.4 Perform regression testing
    - Verify existing Together Session host functionality unchanged
    - Verify existing subscription features still work
    - Verify player controls remain responsive
    - Verify queue operations don't affect playback
    - Test app stability and performance
    - _Requirements: All_

- [ ] 14. Performance optimization and polish
  - [ ] 14.1 Optimize player scroll animations
    - Measure scroll animation frame rate (target: 60fps)
    - Test animation smoothness on low-end devices
    - Verify no jank during scroll
    - Profile and optimize if needed
    - _Requirements: 3.5, 3.6_

  - [ ] 14.2 Optimize widget performance
    - Measure widget update latency
    - Test battery impact of widget updates
    - Test memory usage of multiple widget instances
    - Optimize update frequency if needed
    - _Requirements: 6.6_

  - [ ] 14.3 Add accessibility improvements
    - Add content descriptions to all new UI elements
    - Ensure minimum touch target size (48dp)
    - Test with TalkBack enabled
    - Verify keyboard navigation works
    - _Requirements: All UI changes_

- [ ] 15. Final checkpoint - Complete implementation
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- All code should follow existing Kotlin and Jetpack Compose conventions
- Test on Android versions 8.0+ (API 26+)
- Use Hilt for dependency injection throughout
- Maintain MVVM architecture patterns
- Add comprehensive comments for complex logic
- Profile performance for animations and widget updates
