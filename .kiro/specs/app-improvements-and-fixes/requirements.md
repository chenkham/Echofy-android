# Requirements Document

## Introduction

This document specifies requirements for multiple UI/UX improvements and bug fixes for the Echofy Android music streaming application. The improvements cover together session functionality, settings interface cleanup, Apple Music-style player behaviors, subscription management, widget creation, and premium features.

## Glossary

- **Echofy_App**: The Android music streaming application
- **Together_Session**: A collaborative listening feature allowing multiple users to listen to music synchronously
- **Host**: The user who creates a Together Session
- **Guest**: A user who joins an existing Together Session
- **Player**: The music playback interface component
- **Queue**: The list of upcoming songs to be played
- **Widget**: An Android home screen component displaying music controls
- **Premium_Labs**: A section of the app containing experimental premium features
- **Settings_UI**: The application settings interface
- **Lyrics_Section**: The interface displaying song lyrics
- **Queue_Section**: The interface displaying the upcoming songs queue
- **Promo_Code**: A code that grants subscription benefits when redeemed
- **Google_Play_Console**: Google's platform for managing Android app subscriptions

## Requirements

### Requirement 1: Together Session Join Functionality

**User Story:** As a guest user, I want to successfully join a Together Session, so that I can listen to music synchronously with the host.

#### Acceptance Criteria

1. WHEN a guest attempts to join a Together Session using a valid room code, THE Together_Session SHALL establish a connection to the host
2. WHEN the connection is established, THE Together_Session SHALL synchronize the guest's playback state with the host's current playback
3. IF the join operation fails, THEN THE Together_Session SHALL display a descriptive error message to the guest
4. THE Together_Session SHALL maintain synchronization between host and guest playback states throughout the session
5. WHEN the host changes playback state, THE Together_Session SHALL update the guest's playback within 2 seconds

### Requirement 2: Settings UI Cleanup

**User Story:** As a user, I want a cleaner settings interface, so that I can navigate settings more easily.

#### Acceptance Criteria

1. THE Settings_UI SHALL render without horizontal slim lines between settings items
2. THE Settings_UI SHALL render function items without arrow-type indicators
3. THE Settings_UI SHALL maintain all existing settings functionality after visual changes
4. THE Settings_UI SHALL display settings items with consistent spacing and alignment

### Requirement 3: Apple Music-Style Player Scroll Behavior

**User Story:** As a user, I want the player to hide when scrolling through lyrics or queue, so that I have more screen space to view content.

#### Acceptance Criteria

1. WHEN the user scrolls down in the Lyrics_Section, THE Player SHALL hide from view
2. WHEN the user scrolls up in the Lyrics_Section, THE Player SHALL reappear
3. WHEN the user scrolls down in the Queue_Section, THE Player SHALL hide from view
4. WHEN the user scrolls up in the Queue_Section, THE Player SHALL reappear
5. THE Player SHALL animate smoothly when hiding and showing with a transition duration between 200ms and 400ms
6. THE Player SHALL complete the hide animation before the user scrolls more than 100 pixels

### Requirement 4: Queue Management Improvements

**User Story:** As a user, I want to remove songs from the queue and not see the currently playing song in the queue list, so that I can better manage my listening experience.

#### Acceptance Criteria

1. THE Queue_Section SHALL display a remove button for each song in the queue
2. WHEN the user taps the remove button, THE Queue_Section SHALL remove that song from the queue
3. THE Queue_Section SHALL exclude the currently playing song from the displayed queue list
4. WHEN a song is removed from the queue, THE Queue_Section SHALL update the display within 500ms
5. THE Queue_Section SHALL maintain queue order after song removal

### Requirement 5: Premium Subscription Simplification

**User Story:** As a user, I want to see only the available subscription options, so that I can make a clear purchasing decision.

#### Acceptance Criteria

1. THE Echofy_App SHALL display exactly two subscription options: ₹15 per month and ₹399 per 2 years
2. THE Echofy_App SHALL remove all other subscription tier options from the subscription interface
3. THE Echofy_App SHALL display only the features and benefits that are actually included in the subscription
4. THE Echofy_App SHALL remove any promotional or exaggerated feature descriptions
5. THE Echofy_App SHALL maintain functional subscription purchase flows for both displayed options

### Requirement 6: Compact Widget Creation

**User Story:** As a user, I want a small home screen widget with essential controls, so that I can control playback without opening the app.

#### Acceptance Criteria

1. THE Echofy_App SHALL provide a compact widget with dimensions suitable for a 2x2 grid space
2. THE Widget SHALL display the following elements: play/pause button, next button, previous button, app logo, cover art, and song name
3. WHEN the user taps the play/pause button, THE Widget SHALL toggle playback state
4. WHEN the user taps the next button, THE Widget SHALL skip to the next song
5. WHEN the user taps the previous button, THE Widget SHALL skip to the previous song
6. THE Widget SHALL update the displayed song name and cover art within 2 seconds when the song changes
7. THE Widget SHALL use a simple, minimalist design without complex animations or effects

### Requirement 7: Together Section UI Improvements

**User Story:** As a user, I want better control over the Together Section interface, so that I don't accidentally close it and have a better viewing experience.

#### Acceptance Criteria

1. THE Together_Session SHALL disable the swipe-down gesture for closing the Together Section
2. THE Together_Session SHALL allow closing only via the cross button located at the top left
3. THE Player SHALL scale to fit the entire screen height when in Together Section
4. THE Together_Session SHALL fill any empty space at the bottom of the screen with appropriate UI elements or background
5. THE Together_Session SHALL maintain touch responsiveness for all controls after UI adjustments

### Requirement 8: Premium Labs Promo Code Feature

**User Story:** As a user, I want to redeem promo codes for subscription benefits, so that I can access premium features through promotional offers.

#### Acceptance Criteria

1. THE Premium_Labs SHALL display a promo code input section
2. WHEN the user enters a valid Promo_Code, THE Premium_Labs SHALL validate the code with Google_Play_Console
3. WHEN validation succeeds, THE Premium_Labs SHALL grant the associated subscription benefits to the user
4. IF validation fails, THEN THE Premium_Labs SHALL display an error message indicating the code is invalid or expired
5. THE Premium_Labs SHALL integrate with Google_Play_Console subscription giveaway functionality
6. THE Premium_Labs SHALL display the redemption status within 5 seconds of code submission
7. WHEN a Promo_Code is successfully redeemed, THE Premium_Labs SHALL update the user's subscription status immediately
