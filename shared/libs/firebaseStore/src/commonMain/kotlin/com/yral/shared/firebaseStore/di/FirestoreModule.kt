package com.yral.shared.firebaseStore.di

import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.repository.FBFirestoreRepository
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.firebaseStore.usecase.UpdateDocumentUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
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
        factory { Firebase.firestore(Firebase.app) }
        factory { Firebase.storage(Firebase.app) }
        factory<FBFirestoreRepositoryApi> { FBFirestoreRepository(get()) }
        factoryOf(::UpdateDocumentUseCase)
        includes(
            getFBDocumentUseCaseFactory<GameConfigDto>("GameConfig"),
            getCollectionUseCaseFactory<AboutGameItemDto>("AboutGame"),
        )
    }
