package com.yral.android.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.koin.koinInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val videos: List<ProfileVideo>,
        val hasMorePages: Boolean,
    ) : ProfileUiState()

    data class Error(
        val message: String,
    ) : ProfileUiState()
}

data class ProfileState(
    val accountInfo: AccountInfo? = null,
    val deleteConfirmation: String? = null,
    val openVideo: ProfileVideo? = null,
    val uiState: ProfileUiState = ProfileUiState.Loading,
)

// Mock ViewModel - replace with actual implementation
class ProfileViewModel : ViewModel() {
    private val _viewState = MutableStateFlow(ProfileState())
    val viewState: StateFlow<ProfileState> = _viewState
    private val sessionManager = koinInstance.get<SessionManager>()

    init {
        val accountInfo = sessionManager.getAccountInfo()
        _viewState.update {
            it.copy(accountInfo = accountInfo)
        }
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(2000)
            _viewState.update {
                it.copy(uiState = ProfileUiState.Success(dummyVideos(), false))
            }
        }
    }

    fun openVideo(video: ProfileVideo?) {
        _viewState.update {
            it.copy(openVideo = video)
        }
    }

    fun confirmDelete(videoID: String?) {
        _viewState.update {
            it.copy(deleteConfirmation = videoID)
        }
    }

    fun deleteVideo() {
        viewModelScope.launch {
            val videoToBeDeleted = _viewState.value.deleteConfirmation
            val currentState = _viewState.value.uiState
            val openVideo = _viewState.value.openVideo
            if (currentState is ProfileUiState.Success) {
                // Set isDeleting to true for the specific video
                val updatedVideos =
                    currentState.videos.map { video ->
                        if (video.videoID == videoToBeDeleted) {
                            video.copy(isDeleting = true)
                        } else {
                            video
                        }
                    }
                _viewState.update {
                    it.copy(
                        deleteConfirmation = null,
                        openVideo = openVideo?.copy(isDeleting = true),
                        uiState = currentState.copy(videos = updatedVideos),
                    )
                }

                // Wait for 5 seconds
                @Suppress("MagicNumber")
                delay(2000)

                // Set isDeleting back to false for the specific video
                val resetVideos = updatedVideos.filter { it.videoID != videoToBeDeleted }
                _viewState.update {
                    it.copy(
                        openVideo = null,
                        uiState =
                            ProfileUiState.Success(
                                resetVideos,
                                currentState.hasMorePages,
                            ),
                    )
                }
            }
        }
    }
}

@Serializable
data class ProfileVideo(
    val postID: Long,
    val videoID: String,
    val url: String,
    val hashtags: List<String>,
    val thumbnail: String,
    val viewCount: ULong,
    val displayName: String,
    val postDescription: String,
    var likeCount: ULong,
    var isLiked: Boolean,
    var nsfwProbability: Double,
    var isDeleting: Boolean = false,
)

@Suppress("LongMethod")
private fun dummyVideos(): List<ProfileVideo> {
    val json = koinInstance.get<Json>()
    val data =
        """
        [
          {
            "postID": 94,
            "videoID": "097519f2a88b46dc94c7a2fc7d021d8b",
            "canisterID": "os4dw-yaaaa-aaaag-aoz7a-cai",
            "principalID": "nd537-csykv-bdqwe-ov6ik-l7nsy-4tjly-v2jhz-oczmm-fkq3g-25y5o-nqe",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/097519f2a88b46dc94c7a2fc7d021d8b/downloads/default.mp4",
            "hashtags": [],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/097519f2a88b46dc94c7a2fc7d021d8b/thumbnails/thumbnail.jpg",
            "viewCount": 207,
            "displayName": "",
            "postDescription": "",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.9937/public",
            "likeCount": 0,
            "isLiked": false,
            "nsfwProbability": 0.1
          },
          {
            "postID": 15,
            "videoID": "90f6fcb06e4d4b7297f76fccfe623793",
            "canisterID": "4cvms-hiaaa-aaaak-qf5pa-cai",
            "principalID": "pyr3q-5wsbn-3zkaf-yjz5w-tnct2-hrfwi-psmo2-fgryv-rtoiq-jdpxf-5ae",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/90f6fcb06e4d4b7297f76fccfe623793/downloads/default.mp4",
            "hashtags": ["hotnot"],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/90f6fcb06e4d4b7297f76fccfe623793/thumbnails/thumbnail.jpg",
            "viewCount": 96,
            "displayName": "",
            "postDescription": "Hot hot hot hot",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.11362/public",
            "likeCount": 23,
            "isLiked": true,
            "nsfwProbability": 0.32
          },
          {
            "postID": 10,
            "videoID": "b739e451c0494b73b1add042aedb6c2e",
            "canisterID": "4dgst-lqaaa-aaaao-qhmgq-cai",
            "principalID": "e3hf6-blb7n-yghev-sne2v-dbn4x-67zhc-dwcdr-ujcuw-j2dvt-ljhql-eae",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/b739e451c0494b73b1add042aedb6c2e/downloads/default.mp4",
            "hashtags": [],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/b739e451c0494b73b1add042aedb6c2e/thumbnails/thumbnail.jpg",
            "viewCount": 172,
            "displayName": "",
            "postDescription": ". . ..    ..",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.5162/public",
            "likeCount": 245,
            "isLiked": false,
            "nsfwProbability": 0.14
          },
          {
            "postID": 0,
            "videoID": "e6e08933ae244b7e86c6fbe8ae4ff7f1",
            "canisterID": "ugsn7-hqaaa-aaaak-anzoq-cai",
            "principalID": "qupfk-3sx22-cjits-tzmdb-ebigg-32auq-2z7ij-xsp43-zuaxa-rnhnh-sae",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/e6e08933ae244b7e86c6fbe8ae4ff7f1/downloads/default.mp4",
            "hashtags": ["night", "jamming"],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/e6e08933ae244b7e86c6fbe8ae4ff7f1/thumbnails/thumbnail.jpg",
            "viewCount": 1862,
            "displayName": "",
            "postDescription": "Night jamming",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.4172/public",
            "likeCount": 0,
            "isLiked": false,
            "nsfwProbability": 0.22
          },
          {
            "postID": 47,
            "videoID": "51d9a26f209d42c18866a48d1f6b5ad7",
            "canisterID": "gcrga-giaaa-aaaah-am4xq-cai",
            "principalID": "aj6fc-auwlk-x55h5-y5ylv-f6kbv-vhopk-ov47r-tlmnp-crnj2-i4lwj-4ae",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/51d9a26f209d42c18866a48d1f6b5ad7/downloads/default.mp4",
            "hashtags": [],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/51d9a26f209d42c18866a48d1f6b5ad7/thumbnails/thumbnail.jpg",
            "viewCount": 127,
            "displayName": "",
            "postDescription": "",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.4056/public",
            "likeCount": 0,
            "isLiked": false,
            "nsfwProbability": 0.38
          },
          {
            "postID": 10,
            "videoID": "2097f95ee6744732a3d750c4674e9ce8",
            "canisterID": "5qqrk-dqaaa-aaaag-qk77q-cai",
            "principalID": "qgg34-hgsnq-ue6ci-lke4r-4bc7r-i77n3-32ip3-pltno-eqkxh-rujrv-bqe",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/2097f95ee6744732a3d750c4674e9ce8/downloads/default.mp4",
            "hashtags": ["Hotnot"],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/2097f95ee6744732a3d750c4674e9ce8/thumbnails/thumbnail.jpg",
            "viewCount": 140,
            "displayName": "",
            "postDescription": "Hot not only",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.5813/public",
            "likeCount": 0,
            "isLiked": false,
            "nsfwProbability": 0.18
          },
          {
            "postID": 448,
            "videoID": "8cff870a31a544e1955be19cc95ebf36",
            "canisterID": "pu547-wiaaa-aaaak-amnrq-cai",
            "principalID": "rtqsl-ewqsx-rz7by-5lbbs-scp3k-3c5ff-epn75-nhtrs-vhm4h-nzoe4-tae",
            "url": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/8cff870a31a544e1955be19cc95ebf36/downloads/default.mp4",
            "hashtags": ["#connorandyvonne", "#fyp", "#couple", "#concert", "#JLS", "#CapitalSTB", "#barclaycard"],
            "thumbnail": "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/8cff870a31a544e1955be19cc95ebf36/thumbnails/thumbnail.jpg",
            "viewCount": 142,
            "displayName": "",
            "postDescription": "AD the way I SCREAMED 😭 well played @barclaycard for bringing JLS to the party 👏🥳🕺🏼",
            "profileImageURL": "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.16040/public",
            "likeCount": 0,
            "isLiked": false,
            "nsfwProbability": 0.24
          }
        ]
        """.trimIndent()
    return json.decodeFromString(data)
}
