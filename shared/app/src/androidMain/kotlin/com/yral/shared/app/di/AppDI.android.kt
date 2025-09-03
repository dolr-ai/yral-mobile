package com.yral.shared.app.di

import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.core.di.coreModule
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.features.account.di.accountsModule
import com.yral.shared.features.auth.di.authModule
import com.yral.shared.features.feed.di.feedModule
import com.yral.shared.features.game.di.gameModule
import com.yral.shared.features.profile.di.profileModule
import com.yral.shared.features.root.di.rootModule
import com.yral.shared.features.uploadvideo.di.uploadVideoModule
import com.yral.shared.firebaseAuth.di.firebaseAuthModule
import com.yral.shared.firebaseStore.di.firestoreModule
import com.yral.shared.http.di.networkModule
import com.yral.shared.preferences.di.preferencesModule
import com.yral.shared.rust.service.di.rustModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        // Forbid definition override
        allowOverride(false)
        appDeclaration()
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
        )
        modules(
            authModule,
            feedModule,
            rootModule,
            accountsModule,
            gameModule,
            uploadVideoModule,
            profileModule,
        )
    }
}
