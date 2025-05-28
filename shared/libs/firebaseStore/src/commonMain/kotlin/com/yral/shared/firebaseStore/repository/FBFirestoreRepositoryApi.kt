package com.yral.shared.firebaseStore.repository

import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.QueryOptions
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface FBFirestoreRepositoryApi {
    suspend fun <T : FirestoreDocument> getDocument(
        collectionPath: String,
        documentId: String,
        documentType: KClass<T>,
    ): Result<T>

    suspend fun <T : FirestoreDocument> getCollection(
        collectionPath: String,
        documentType: KClass<T>,
    ): Result<List<T>>

    suspend fun <T : FirestoreDocument> queryCollection(
        collectionPath: String,
        documentType: KClass<T>,
        queryOptions: QueryOptions,
    ): Result<List<T>>

    fun <T : FirestoreDocument> observeDocument(
        collectionPath: String,
        documentId: String,
        documentType: KClass<T>,
    ): Flow<T?>

    fun <T : FirestoreDocument> observeCollection(
        collectionPath: String,
        documentType: KClass<T>,
    ): Flow<List<T>>

    fun <T : FirestoreDocument> observeQueryCollection(
        collectionPath: String,
        documentType: KClass<T>,
        queryOptions: QueryOptions,
    ): Flow<List<T>>

    suspend fun documentExists(path: String): Result<Boolean>

//    suspend fun <T : FirestoreDocument> setDocument(
//        collectionPath: String,
//        document: T,
//        merge: Boolean = false
//    ): Result<Unit>

    suspend fun deleteDocument(
        collectionPath: String,
        documentId: String,
    ): Result<Unit>
}
