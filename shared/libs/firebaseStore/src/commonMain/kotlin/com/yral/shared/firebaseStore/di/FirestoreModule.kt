package com.yral.shared.firebaseStore.di

import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.firebaseStore.repository.FBFirestoreRepository
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.firebaseStore.usecase.UpdateDocumentUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

inline fun <reified T : FirestoreDocument> getCollectionUseCaseFactory(qualifier: String): Module =
    module {
        factory(named(qualifier)) { GetCollectionUseCase(get(), get(), get(), T::class) }
    }

inline fun <reified T : FirestoreDocument> getFBDocumentUseCaseFactory(qualifier: String): Module =
    module {
        factory(named(qualifier)) { GetFBDocumentUseCase(get(), get(), get(), T::class) }
    }

val firestoreModule: Module =
    module {
        factory<FBFirestoreRepositoryApi> { FBFirestoreRepository(get()) }
        factoryOf(::UpdateDocumentUseCase)
        includes(
            getFBDocumentUseCaseFactory<GameConfigDto>("GameConfig"),
            getFBDocumentUseCaseFactory<LeaderboardItemDto>("LeaderBoard"),
            getCollectionUseCaseFactory<AboutGameItemDto>("AboutGame"),
            getCollectionUseCaseFactory<LeaderboardItemDto>("LeaderBoard"),
        )
    }
