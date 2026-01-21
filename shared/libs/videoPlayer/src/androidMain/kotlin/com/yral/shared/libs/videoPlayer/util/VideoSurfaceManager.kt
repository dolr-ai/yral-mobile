package com.yral.shared.libs.videoPlayer.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages TextureViews outside of Compose composition to prevent recreation during recomposition.
 * Each ExoPlayer instance gets a persistent TextureView that survives composable recreation.
 */
object VideoSurfaceManager {
    private val playerViews = mutableMapOf<ExoPlayer, ManagedTextureView>()

    /**
     * Data class holding a TextureView and its surface state.
     */
    private data class ManagedTextureView(
        val textureView: TextureView,
        var surface: Surface? = null,
        var isAttached: Boolean = false,
    )

    /**
     * Get or create a TextureView for the given player.
     * The TextureView is kept alive even when composables recreate.
     */
    fun getTextureViewForPlayer(context: Context, player: ExoPlayer): TextureView {
        val existing = playerViews[player]
        if (existing != null) {
            co.touchlab.kermit.Logger.d("VideoSurfaceManager: Reusing existing TextureView for player")
            // Don't detach here - let the caller handle parent management
            // If surface is still valid and attached, ensure player has it
            existing.surface?.let { surface ->
                if (existing.isAttached) {
                    co.touchlab.kermit.Logger.d("VideoSurfaceManager: Re-setting existing surface to player")
                    player.setVideoSurface(surface)
                }
            }
            return existing.textureView
        }

        co.touchlab.kermit.Logger.d("VideoSurfaceManager: Creating NEW TextureView for player")
        // Create new TextureView for this player
        val textureView = TextureView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val managed = ManagedTextureView(textureView)
        playerViews[player] = managed

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                co.touchlab.kermit.Logger.d("VideoSurfaceManager: onSurfaceTextureAvailable")
                val surface = Surface(surfaceTexture)
                managed.surface = surface
                managed.isAttached = true
                player.setVideoSurface(surface)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {}

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                co.touchlab.kermit.Logger.d("VideoSurfaceManager: onSurfaceTextureDestroyed - keeping surface")
                // DON'T release anything here - we want to keep the surface alive
                // Return false to prevent the system from releasing the SurfaceTexture
                // This allows us to reuse it when the view is re-attached
                managed.isAttached = false
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }

        return textureView
    }

    /**
     * Release resources for a player when it's being disposed.
     */
    fun releasePlayer(player: ExoPlayer) {
        playerViews.remove(player)?.let { managed ->
            managed.surface?.release()
            managed.surface = null
            player.setVideoSurface(null)
        }
    }

    /**
     * Clear all managed views. Call when the pool is disposed.
     */
    fun clear() {
        playerViews.forEach { (player, managed) ->
            managed.surface?.release()
            player.setVideoSurface(null)
        }
        playerViews.clear()
    }
}
