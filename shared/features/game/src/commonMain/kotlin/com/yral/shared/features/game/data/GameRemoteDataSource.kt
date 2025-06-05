package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.AboutGameItemDto
import com.yral.shared.features.game.data.models.GameConfigDto
import kotlinx.serialization.json.Json

class GameRemoteDataSource(
    private val json: Json,
) : IGameRemoteDataSource {
    override suspend fun getConfig(): GameConfigDto =
        json
            .decodeFromString(dummyConfig())

    override suspend fun getRules(): List<AboutGameItemDto> =
        json
            .decodeFromString(dummyRules())
}

private fun dummyConfig(): String =
    """
{
  "available_smileys": [
    {
      "id": "laugh",
      "image_name": "laugh",
      "image_url": "smiley_game/how_to_play/laughing.png",
      "click_animation": "smiley_game/animations/smiley_game_laugh.json",
      "is_active": true
    },
    {
      "id": "heart",
      "image_name": "heart",
      "image_url": "smiley_game/how_to_play/heart.png",
      "click_animation": "smiley_game/animations/smiley_game_heart.json",
      "is_active": true
    },
    {
      "id": "fire",
      "image_name": "fire",
      "image_url": "smiley_game/how_to_play/fire.png",
      "click_animation": "smiley_game/animations/smiley_game_fire.json",
      "is_active": true
    },
    {
      "id": "surprise",
      "image_name": "surprise",
      "image_url": "smiley_game/how_to_play/surprise.png",
      "click_animation": "smiley_game/animations/smiley_game_surprise.json",
      "is_active": true
    },
    {
      "id": "rocket",
      "image_name": "rocket",
      "image_url": "smiley_game/how_to_play/rocket.png",
      "click_animation": "smiley_game/animations/smiley_game_rocket.json",
      "is_active": true
    }
  ],
  "loss_penalty": 10
}
    """.trimIndent()

@Suppress("LongMethod")
private fun dummyRules(): String =
    """
[
    {
      "body": [
        {
          "colors": ["grey50"],
          "content": ["While watching a video, vote for one of 5 emojis:"],
          "type": "text"
        },
        {
          "image_urls": [
            "smiley_game/how_to_play/laughing.png",
            "smiley_game/how_to_play/heart.png",
            "smiley_game/how_to_play/fire.png",
            "smiley_game/how_to_play/surprise.png",
            "smiley_game/how_to_play/rocket.png"
          ],
          "type": "images"
        },
        {
          "colors": ["grey50"],
          "content": ["Each vote costs 10 points"],
          "type": "text"
        }
      ],
      "name": "Pick an emoji",
      "thumbnail_url": "smiley_game/how_to_play/rule_1.png"
    },
    {
      "body": [
        {
          "colors": ["grey50"],
          "content": ["You can only vote once per video, so choose wisely!"],
          "type": "text"
        }
      ],
      "name": "One Vote per Video",
      "thumbnail_url": "smiley_game/how_to_play/rule_2.png"
    },
    {
      "body": [
        {
          "colors": ["grey50", "green300", "grey50", "red300"],
          "content": [
            "If your emoji gets that most votes, you ",
            "win 30 points. ",
            "If not, ",
            "you lose 10 points"
          ],
          "type": "text"
        }
      ],
      "name": "Win or Loose",
      "thumbnail_url": "smiley_game/how_to_play/rule_3.png"
    },
    {
      "body": [
        {
          "colors": ["grey50"],
          "content": ["Your balance updates in real time on the home screen"],
          "type": "text"
        }
      ],
      "name": "Live Points Update",
      "thumbnail_url": "smiley_game/how_to_play/rule_4.png"
    },
    {
      "body": [
        {
          "colors": ["grey50"],
          "content": ["Top 10 users ranked by total points (tie-breaker: most wins)."],
          "type": "text"
        }
      ],
      "name": "Leaderboard",
      "thumbnail_url": "smiley_game/how_to_play/rule_5.png"
    }
]
    """.trimIndent()
