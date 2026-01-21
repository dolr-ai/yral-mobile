package com.yral.shared.app.di

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.platformLogWriter
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.app.config.AppHTTPEventListener
import com.yral.shared.app.config.AppRustLogForwardingListener
import com.yral.shared.app.config.AppUseCaseFailureListener
import com.yral.shared.app.config.NBRFailureListener
import com.yral.shared.app.logging.SentryLogWriter
import com.yral.shared.core.di.coreModule
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.data.di.commonDataModule
import com.yral.shared.features.account.di.accountsModule
import com.yral.shared.features.auth.di.authModule
import com.yral.shared.features.chat.di.chatModule
import com.yral.shared.features.feed.di.feedModule
import com.yral.shared.features.game.di.gameModule
import com.yral.shared.features.leaderboard.di.leaderboardModule
import com.yral.shared.features.profile.di.profileModule
import com.yral.shared.features.root.di.rootModule
import com.yral.shared.features.subscriptions.di.subscriptionsModule
import com.yral.shared.features.tournament.di.tournamentModule
import com.yral.shared.features.uploadvideo.di.uploadVideoModule
import com.yral.shared.features.wallet.di.walletModule
import com.yral.shared.firebaseAuth.di.firebaseAuthModule
import com.yral.shared.firebaseStore.di.firestoreModule
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.di.networkModule
import com.yral.shared.libs.arch.data.NetworkBoundResource
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.libs.filedownloader.di.fileDownloaderModule
import com.yral.shared.libs.videoPlayer.pool.PlatformMediaSourceFactory
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayerFactory
import com.yral.shared.preferences.di.preferencesModule
import com.yral.shared.reportVideo.di.reportVideoModule
import com.yral.shared.rust.service.di.rustModule
import com.yral.shared.rust.service.services.RustLogForwardingListener
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.includes
import org.koin.dsl.module

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        // Forbid definition override
        allowOverride(false)
        includes(config)
        modules(
            platformModule,
            coreModule,
            preferencesModule,
            analyticsModule,
            crashlyticsModule,
            networkModule,
            fileDownloaderModule,
            rustModule,
            firebaseAuthModule,
            firestoreModule,
            dispatchersModule,
            archModule,
            featureFlagModule,
            routingModule,
            sharingModule,
            reportVideoModule,
            loggerModule,
            httpListenerModule,
            commonDataModule,
            videoPlayerModule,
        )

        modules(
            authModule,
            feedModule,
            rootModule,
            accountsModule,
            gameModule,
            leaderboardModule,
            tournamentModule,
            uploadVideoModule,
            profileModule,
            walletModule,
            chatModule,
            subscriptionsModule,
        )
    }
}

internal val loggerModule =
    module {
        single { SentryLogWriter() }
        single<LogWriter>(named("httpLogWriter")) { SentryLogWriter() }
        single<LogWriter>(named("rustLogWriter")) { SentryLogWriter() }
        single<LogWriter>(named("installReferrerLogWriter")) { SentryLogWriter() }
        single {
            val writers = mutableListOf<LogWriter>()
            if (get(IS_DEBUG)) {
                writers += platformLogWriter()
            }
            YralLogger(writers)
        }
        single {
            AppRustLogForwardingListener(
                get(),
                get(named("rustLogWriter")),
            )
        } bind RustLogForwardingListener::class
    }

internal val dispatchersModule = module { single { AppDispatchers() } }

internal val archModule =
    module {
        singleOf(::NBRFailureListener) bind NetworkBoundResource.OnFailureListener::class
        singleOf(::AppUseCaseFailureListener) bind UseCaseFailureListener::class
    }

internal val httpListenerModule =
    module {
        factoryOf(::AppHTTPEventListener) bind HTTPEventListener::class
    }

internal val videoPlayerModule =
    module {
        factory { createPlatformPlayerFactory() }
        factory { createPlatformMediaSourceFactory() }
    }

expect fun Scope.createPlatformPlayerFactory(): PlatformPlayerFactory
expect fun Scope.createPlatformMediaSourceFactory(): PlatformMediaSourceFactory
