package repositories

import com.google.cloud.firestore.FirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.TimeTable
import services.FirestoreService

class TimeTableRepository {
//    private val collection = FirestoreService()
//
//    suspend fun getAllTimeTables(): List<TimeTable> = withContext(Dispatchers.IO) {
//        try {
//            collection.get().documents.mapNotNull { doc ->
//                doc.toObject(TimeTable::class.java)?.copy(id = doc.id)
//            }
//        } catch (e: FirestoreException) {
//            throw e
//        }
//    }
//
//    suspend fun getTimeTableById(id: String): TimeTable? = withContext(Dispatchers.IO) {
//        try {
//            val doc = collection.document(id).get().get()
//            if (doc.exists()) {
//                doc.toObject(TimeTable::class.java)?.copy(id = doc.id)
//            } else {
//                null
//            }
//        } catch (e: FirestoreException) {
//            throw e
//        }
//    }
//
//    suspend fun getTimeTablesByGroup(group: String): List<TimeTable> = withContext(Dispatchers.IO) {
//        try {
//            collection.whereEqualTo("group", group).get().get().documents.mapNotNull { doc ->
//                doc.toObject(TimeTable::class.java)?.copy(id = doc.id)
//            }
//        } catch (e: FirestoreException) {
//            throw e
//        }
//    }
//
//    suspend fun insertTimeTable(timeTable: TimeTable): String = withContext(Dispatchers.IO) {
//        try {
//            // Create a new document with auto-generated ID
//            val docRef = collection.add(timeTable).get()
//            docRef.id
//        } catch (e: FirestoreException) {
//            throw e
//        }
//    }
//
//    suspend fun insertTimeTables(timeTables: List<TimeTable>): List<String> = withContext(Dispatchers.IO) {
//        try {
//            val batch = firestore.batch()
//            val docRefs = timeTables.map { timeTable ->
//                val docRef = collection.document()
//                batch.set(docRef, timeTable)
//                docRef
//            }
//
//            batch.commit().get()
//            docRefs.map { it.id }
//        } catch (e: FirestoreException) {
//            throw e
//        }
//    }
//
//    suspend fun updateTimeTable(id: String, timeTable: TimeTable): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val docRef = collection.document(id)
//            // Make sure we're not modifying the ID
//            val updatedTimeTable = timeTable.copy(id = null)
//            docRef.set(updatedTimeTable).get()
//            true
//        } catch (e: FirestoreException) {
//            false
//        }
//    }
//
//    suspend fun deleteTimeTable(id: String): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val docRef = collection.document(id)
//            docRef.delete().get()
//            true
//        } catch (e: FirestoreException) {
//            false
//        }
//    }
//
//    suspend fun deleteAllTimeTables(): Boolean = withContext(Dispatchers.IO) {
//        try {
//            val documents = collection.get().get()
//            if (documents.isEmpty) {
//                return@withContext false
//            }
//
//            val batch = firestore.batch()
//            for (document in documents) {
//                batch.delete(document.reference)
//            }
//            batch.commit().get()
//            true
//        } catch (e: FirestoreException) {
//            false
//        }
//    }
}