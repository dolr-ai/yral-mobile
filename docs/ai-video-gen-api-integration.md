## AI Video Generation API Integration (current branch)
Refer: https://github.com/dolr-ai/off-chain-agent/wiki/AI-Vid-Gen-API

### HTTP endpoints used by AiVideoGenViewModel

-- /api/v2/videogen/generate
  - Method: POST (JSON)
  - Host: `AppConfigurations.OFF_CHAIN_BASE_URL`
  - Path: `/api/v2/videogen/generate`
  - Request body (GenerateVideoRequestDto):
    ```json
    {
      "delegated_identity": { "from_key": [..], "to_secret": {..}, "delegation_chain": [..] },
      "request": {
        "aspect_ratio": "16:9",
        "duration_seconds": 8,
        "generate_audio": true,
        "image": null,
        "model_id": "veo3",
        "negative_prompt": null,
        "prompt": "<prompt>",
        "resolution": null,
        "seed": null,
        "token_type": "Free",
        "user_id": "<user principal>"
      }
    }
    ```
  - Success response:
    ```json
    {
      "operation_id": "...",
      "provider": "veo3",
      "request_key": { "counter": 1, "principal": "..." }
    }
    ```
  - Error response:
    ```json
    { "ProviderError": "<message>" }
    ```
  - Parsed to domain: `GenerateVideoResult`
  - Code: `UploadVideoRemoteDataSource.generateVideo()`

-- /api/v2/videogen/providers
  - Method: GET
  - Host: `AppConfigurations.OFF_CHAIN_BASE_URL`
  - Path: `/api/v2/videogen/providers`
  - Response body example:
    ```json
    {
      "providers": [
        {
          "id": "veo3",
          "name": "Veo 3",
          "description": "...",
          "cost": { "usd_cents": 100, "dolr": 10, "sats": 0 },
          "supports_image": true,
          "supports_negative_prompt": false,
          "supports_audio": true,
          "supports_seed": false,
          "allowed_aspect_ratios": ["9:16","16:9"],
          "allowed_resolutions": ["720p","1080p"],
          "allowed_durations": [8,16],
          "default_aspect_ratio": "16:9",
          "default_resolution": "720p",
          "default_duration": 8,
          "is_available": true,
          "is_internal": false,
          "model_icon": "https://...",
          "extra_info": { }
        }
      ]
    }
    ```
  - Code: `UploadVideoRemoteDataSource.fetchProviders()`

- /api/upload_ai_video_from_url
  - Method: POST (JSON)
  - Host: `AppConfigurations.ANONYMOUS_IDENTITY_BASE_URL`
  - Path: `/api/upload_ai_video_from_url`
  - Request body example:
    ```json
    {
      "video_url": "https://.../sample.mp4",
      "hashtags": [],
      "description": "",
      "delegated_identity_wire": { "from_key": [..], "to_secret": {..}, "delegation_chain": [..] },
      "is_nsfw": false,
      "enable_hot_or_not": false
    }
    ```
  - Response: empty body (HTTP 200)
  - Code: `UploadVideoRemoteDataSource.uploadAiVideoFromUrl()`

### Repository/use cases wiring (AI)

- `UploadRepositoryImpl` (AI):
  - `fetchProviders()` → `fetchProviders()`
  - `generateVideo(GenerateVideoParams)` → `generateVideo(dto)`; injects delegated identity from `SessionManager`
  - `uploadAiVideoFromUrl(UploadAiVideoFromUrlRequest)` → `uploadAiVideoFromUrl(dto)`; injects delegated identity from `SessionManager`

- Use cases (AI): `GetProvidersUseCase`, `GenerateVideoUseCase`, `UploadAiVideoFromUrlUseCase`, `PollGenerationStatusUseCase`

### Rust/Uniffi integrations

- Polling types and repo:
  - `RateLimitRepository.fetchVideoGenerationStatus(VideoGenRequestKey): PollResult2<VideoGenRequestStatus, String>`
  - Enums/Structs: `VideoGenRequestKey`, `VideoGenRequestStatus`, `PollResult2`
  - Used in `PollGenerationStatusUseCase` to poll generation status until `Complete` or `Failed`.

- Polling algorithm (configurable):
  - Params: `requestKey: VideoGenRequestKey`, `isFastInitially: Boolean`, `maxPollingTimeMs`
  - Config provider: `PollingConfigProvider` (earlyPolls, earlyIntervalMs, initialIntervalMs, maxIntervalMs, minIntervalMs, backoffMultiplier, decayMultiplier)
  - Flow emits `Result<VideoGenRequestStatus, Throwable>` on each poll.

### ViewModels separation (context)
- `UploadVideoViewModel`: classic upload (select file → upload → update metadata). No AI-gen logic.
- `AiVideoGenViewModel`: AI generation flow (providers/credits → generate → poll via Rust → on complete call HTTP `/api/upload_ai_video_from_url`).

### Notes
- Delegated identity is required for: generate, upload_ai_video_from_url.


