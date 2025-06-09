package com.yral.shared.firebaseStore.di

import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.repository.FBFirestoreRepository
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

inline fun <reified T : FirestoreDocument> getCollectionUseCaseFactory(): Module =
    module {
        factory { GetCollectionUseCase(get(), get(), get(), T::class) }
    }

inline fun <reified T : FirestoreDocument> getFBDocumentUseCaseFactory(): Module =
    module {
        factory { GetFBDocumentUseCase(get(), get(), get(), T::class) }
    }

val firestoreModule: Module =
    module {
        factory<FBFirestoreRepositoryApi> { FBFirestoreRepository() }
        includes(
            getFBDocumentUseCaseFactory<GameConfigDto>(),
            getCollectionUseCaseFactory<AboutGameItemDto>(),
        )
    }
