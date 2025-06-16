package com.yral.shared.firebaseStore.di

import com.yral.shared.firebaseStore.model.FirestoreDocument
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
        factory<FBFirestoreRepositoryApi> { FBFirestoreRepository() }
        factoryOf(::UpdateDocumentUseCase)
    }
