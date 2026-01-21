package com.yral.shared.libs.videoPlayer.cardstack

/**
 * Configuration constants for the swipeable card stack video player.
 */
object CardStackConstants {
    /** Fraction of screen width/height required to dismiss a card (0.35 = 35%) */
    const val SWIPE_THRESHOLD_FRACTION = 0.35f

    /** Fraction of swipe threshold at which to commit video transition (0.5 = 50% of threshold) */
    const val SWIPE_COMMIT_THRESHOLD = 0.5f

    /** Maximum rotation angle in degrees when card is dragged horizontally */
    const val ROTATION_MULTIPLIER = 15f

    /** Scale difference between stacked cards (front card = 1.0, next = 0.92, etc.) */
    const val CARD_SCALE_STEP = 0.08f

    /** Vertical offset in dp between stacked cards (negative = cards peek from top) */
    const val CARD_OFFSET_STEP_DP = 56

    /** Number of cards visible behind the front card */
    const val VISIBLE_CARDS = 3

    /** Duration for card dismiss animation in milliseconds */
    const val DISMISS_ANIMATION_DURATION_MS = 800

    /** Duration for snap-back animation in milliseconds */
    const val SNAP_BACK_DURATION_MS = 200

    /** Delay before starting playback on new card (allows animation to settle) */
    const val PLAYER_SETUP_DELAY_MS = 100L

    /** Velocity threshold for fling dismissal */
    const val FLING_VELOCITY_THRESHOLD = 1000f

    /** Maximum alpha for swipe feedback overlay */
    const val SWIPE_FEEDBACK_MAX_ALPHA = 0.3f

    /** Corner radius for card rounded corners in dp */
    const val CARD_CORNER_RADIUS_DP = 20

    /** Shadow elevation for cards in dp */
    const val CARD_SHADOW_ELEVATION_DP = 12f

    /** Additional horizontal padding to show card edges */
    const val CARD_HORIZONTAL_PADDING_DP = 24

    /** Minimum movement in pixels required to determine swipe direction */
    const val MIN_DIRECTION_THRESHOLD_PX = 10f

    /** Multiplier for off-screen exit position during dismiss animation */
    const val EXIT_MULTIPLIER = 1.5f

    /** Spring damping ratio for snap-back animation */
    const val SNAP_BACK_DAMPING_RATIO = 0.6f

    /** Spring stiffness for snap-back animation */
    const val SNAP_BACK_STIFFNESS = 400f

    /** Base visual progress when touching (before drag) - set to 0 to disable pop animation */
    const val TOUCH_BASE_PROGRESS = 0f

    /** Shadow elevation decay factor per card in stack */
    const val SHADOW_ELEVATION_DECAY = 0.2f

    /** Fraction of visual progress at which to start playing the next video (0.35 = 35%) */
    const val EARLY_PLAYBACK_THRESHOLD = 0.35f
}
