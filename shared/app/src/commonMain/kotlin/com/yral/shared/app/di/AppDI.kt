package com.yral.shared.app.di

import co.touchlab.kermit.platformLogWriter
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.app.config.AppHTTPEventListener
import com.yral.shared.app.config.AppRustLogForwardingListener
import com.yral.shared.app.config.AppUseCaseFailureListener
import com.yral.shared.app.config.NBRFailureListener
import com.yral.shared.core.di.coreModule
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.features.account.di.accountsModule
import com.yral.shared.features.auth.di.authModule
import com.yral.shared.features.feed.di.feedModule
import com.yral.shared.features.game.di.gameModule
import com.yral.shared.features.leaderboard.di.leaderboardModule
import com.yral.shared.features.profile.di.profileModule
import com.yral.shared.features.root.di.rootModule
import com.yral.shared.features.uploadvideo.di.uploadVideoModule
import com.yral.shared.features.wallet.di.walletModule
import com.yral.shared.firebaseAuth.di.firebaseAuthModule
import com.yral.shared.firebaseStore.di.firestoreModule
import com.yral.shared.http.HTTPEventListener
import com.yral.shared.http.di.networkModule
import com.yral.shared.libs.arch.data.NetworkBoundResource
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.di.preferencesModule
import com.yral.shared.reportVideo.di.reportVideoModule
import com.yral.shared.rust.service.di.rustModule
import com.yral.shared.rust.service.services.RustLogForwardingListener
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
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
        )

        modules(
            authModule,
            feedModule,
            rootModule,
            accountsModule,
            gameModule,
            leaderboardModule,
            uploadVideoModule,
            profileModule,
            walletModule,
        )
    }
}

internal val loggerModule =
    module {
        single { YralLogger(if (get(IS_DEBUG)) platformLogWriter() else null) }
        singleOf(::AppRustLogForwardingListener) bind RustLogForwardingListener::class
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
