package com.yral.shared.firebaseStore.repository

import android.icu.util.UniversalTimeScale.toLong
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.firebaseStore.model.QueryOptions
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@Suppress("TooGenericExceptionCaught")
internal class FBFirestoreRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : FBFirestoreRepositoryApi {
    override suspend fun <T : FirestoreDocument> getDocument(
        collectionPath: String,
        documentId: String,
        documentType: KClass<T>,
    ): Result<T> =
        try {
            val document =
                firestore
                    .collection(collectionPath)
                    .document(documentId)
                    .get()
            if (!document.exists) {
                Result.failure(YralException("Document does not exist"))
            } else {
                Result.success(document.safeData(documentType))
            }
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error getting document"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error getting document: ${e.message}"))
        }

    override suspend fun <T : FirestoreDocument> getCollection(
        collectionPath: String,
        documentType: KClass<T>,
    ): Result<List<T>> =
        try {
            val querySnapshot =
                firestore
                    .collection(collectionPath)
                    .get()
            val documents =
                querySnapshot.documents.map {
                    it.safeData(documentType)
                }
            Result.success(documents)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error getting collection"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error getting collection: ${e.message}"))
        }

    override suspend fun <T : FirestoreDocument> queryCollection(
        collectionPath: String,
        documentType: KClass<T>,
        queryOptions: QueryOptions,
    ): Result<List<T>> =
        try {
            val collection =
                firestore
                    .collection(collectionPath)
            val querySnapshot = applyQueryOptions(collection, queryOptions).get()
            val documents =
                querySnapshot.documents.map {
                    it.safeData(documentType)
                }
            Result.success(documents)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error querying collection"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error querying collection: ${e.message}"))
        }

    override fun <T : FirestoreDocument> observeDocument(
        collectionPath: String,
        documentId: String,
        documentType: KClass<T>,
    ): Flow<T?> =
        firestore
            .collection(collectionPath)
            .document(documentId)
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.safeData(documentType)
                } else {
                    null
                }
            }.catch { e ->
                throw when (e) {
                    is FirebaseFirestoreException -> {
                        YralException(e.message ?: "Error observing document")
                    }

                    else -> {
                        YralException("Unexpected error observing document: ${e.message}")
                    }
                }
            }

    override fun <T : FirestoreDocument> observeCollection(
        collectionPath: String,
        documentType: KClass<T>,
    ): Flow<List<T>> =
        firestore
            .collection(collectionPath)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map {
                    it.safeData(documentType)
                }
            }.catch { e ->
                throw when (e) {
                    is FirebaseFirestoreException -> {
                        YralException(e.message ?: "Error observing collection")
                    }

                    else -> {
                        YralException("Unexpected error observing collection: ${e.message}")
                    }
                }
            }

    override fun <T : FirestoreDocument> observeQueryCollection(
        collectionPath: String,
        documentType: KClass<T>,
        queryOptions: QueryOptions,
    ): Flow<List<T>> {
        val collection = firestore.collection(collectionPath)
        return applyQueryOptions(collection, queryOptions)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map {
                    it.safeData(documentType)
                }
            }.catch { e ->
                throw when (e) {
                    is FirebaseFirestoreException -> {
                        YralException(e.message ?: "Error observing query")
                    }

                    else -> {
                        YralException("Unexpected error observing query: ${e.message}")
                    }
                }
            }
    }

//    override suspend fun <T : FirestoreDocument> setDocument(
//        collectionPath: String,
//        document: T,
//        merge: Boolean
//    ): Result<Unit> = try {
//        firestore.collection(collectionPath)
//            .document(document.id)
//            .set(document, merge)
//        Result.success(Unit)
//    } catch (e: FirebaseFirestoreException) {
//        Result.failure(YralException(e.message ?: "Error setting document"))
//    } catch (e: Exception) {
//        Result.failure(YralException("Unexpected error setting document: ${e.message}"))
//    }

    override suspend fun deleteDocument(
        collectionPath: String,
        documentId: String,
    ): Result<Unit> =
        try {
            firestore
                .collection(collectionPath)
                .document(documentId)
                .delete()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error deleting document"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error deleting document: ${e.message}"))
        }

    override suspend fun documentExists(path: String): Result<Boolean> {
        return try {
            // Split the path into segments to get collection and document parts
            val segments = path.split("/")
            if (segments.size % 2 != 0) {
                return Result.failure(
                    YralException("Invalid document path $path"),
                )
            }

            // Build the document reference by traversing the path
            var reference: Any = firestore.collection(segments[0])
            for (i in 1 until segments.size) {
                reference =
                    if (i % 2 == 0) {
                        // Even indices (0-based) are collection references
                        (reference as DocumentReference).collection(segments[i])
                    } else {
                        // Odd indices are document references
                        (reference as CollectionReference).document(segments[i])
                    }
            }

            // The final reference must be a document reference since we validated even number of segments
            val document = (reference as DocumentReference).get()
            Result.success(document.exists)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error checking document existence"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error checking document existence: ${e.message}"))
        }
    }

    override suspend fun getCollectionCount(
        collectionPath: String,
        queryOptions: QueryOptions,
    ): Result<Long> =
        try {
            val collection = firestore.collection(collectionPath)
            val query = applyQueryOptions(collection, queryOptions)
            val querySnapshot = query.get()
            Result.success(querySnapshot.documents.size.toLong())
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error getting collection count"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error getting collection count: ${e.message}"))
        }

    private fun applyQueryOptions(
        collection: CollectionReference,
        options: QueryOptions,
    ): Query {
        var resultQuery = collection as Query

        // Apply filters
        for (filter in options.filters) {
            resultQuery =
                when (filter) {
                    is QueryOptions.Filter.Equals ->
                        resultQuery.where {
                            filter.field equalTo filter.value
                        }

                    is QueryOptions.Filter.GreaterThan ->
                        resultQuery.where {
                            filter.field greaterThan filter.value
                        }

                    is QueryOptions.Filter.LessThan ->
                        resultQuery.where {
                            filter.field lessThan filter.value
                        }

                    is QueryOptions.Filter.Contains ->
                        resultQuery.where {
                            filter.field contains filter.value
                        }

                    is QueryOptions.Filter.In ->
                        resultQuery.where {
                            filter.field inArray filter.values
                        }
                }
        }

        // Apply ordering
        options.orderBy?.let { orderBy ->
            resultQuery =
                when (orderBy.direction) {
                    QueryOptions.OrderBy.Direction.ASCENDING ->
                        resultQuery.orderBy(orderBy.field)

                    QueryOptions.OrderBy.Direction.DESCENDING ->
                        resultQuery.orderBy(orderBy.field, Direction.DESCENDING)
                }
        }

        // Apply pagination
        options.startAfter?.let { startAfter ->
            resultQuery = resultQuery.startAfter(startAfter)
        }

        options.limit?.let { limit ->
            resultQuery = resultQuery.limit(limit)
        }

        return resultQuery
    }

    override suspend fun updateDocument(
        collectionPath: String,
        documentId: String,
        fieldAndValue: Pair<String, Any?>,
    ): Result<Unit> =
        try {
            firestore
                .collection(collectionPath)
                .document(documentId)
                .update(fieldAndValue)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(YralException(e.message ?: "Error updating document"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error updating document: ${e.message}"))
        }
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
@OptIn(InternalSerializationApi::class)
private fun <T : FirestoreDocument> DocumentSnapshot.safeData(documentType: KClass<T>): T =
    try {
        // Create a new instance with the document ID populated
        when (val data = data(documentType.serializer())) {
            is LeaderboardItemDto -> data.copy(id = id) as T
            is AboutGameItemDto -> data.copy(id = id) as T
            is GameConfigDto -> data.copy(id = id) as T
            else -> data
        }
    } catch (e: Exception) {
        throw YralException("Error deserializing document")
    }
