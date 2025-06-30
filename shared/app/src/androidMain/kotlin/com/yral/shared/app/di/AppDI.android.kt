package com.yral.shared.app.di

import com.yral.shared.analytics.di.analyticsModule
import com.yral.shared.core.AppConfigurations.FIREBASE_APP_NAME
import com.yral.shared.core.di.coreModule
import com.yral.shared.crashlytics.di.crashlyticsModule
import com.yral.shared.features.account.di.accountsModule
import com.yral.shared.features.auth.di.authModule
import com.yral.shared.features.feed.di.feedModule
import com.yral.shared.features.game.di.gameModule
import com.yral.shared.features.root.di.rootModule
import com.yral.shared.firebaseAuth.di.firebaseAuthModule
import com.yral.shared.firebaseStore.di.firestoreModule
import com.yral.shared.http.di.networkModule
import com.yral.shared.preferences.di.preferencesModule
import com.yral.shared.rust.di.rustModule
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

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
            module {
                factory { Firebase.firestore(Firebase.app(FIREBASE_APP_NAME)) }
                factory { Firebase.auth(Firebase.app(FIREBASE_APP_NAME)) }
                factory { Firebase.storage(Firebase.app(FIREBASE_APP_NAME)) }
            },
        )
        modules(
            authModule,
            feedModule,
            rootModule,
            accountsModule,
            gameModule,
        )
    }
}
