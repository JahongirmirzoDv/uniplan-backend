package services

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.TimeTable
import java.io.FileInputStream

class FirestoreService {
    private val firestore = FirestoreOptions.newBuilder()
        .setProjectId("server-uniplan") // Change to the correct Firestore project ID
        .setDatabaseId("timetable")
        .build()
        .service

    private val usersCollection = "users" // Main users collection

    // Save timetables under the user's subcollection
    fun saveTimetables(userId: String, timetables: List<TimeTable>): List<String> {
        println("Saving timetables for user: $userId")
        println("Number of timetables to save: ${timetables.size}")

        val batch = firestore.batch()
        val documentIds = mutableListOf<String>()

        val userCollection = firestore.collection(usersCollection)
            .document(userId)
            .collection("timetables")

        timetables.forEach { timetable ->
            // Debug: Print each timetable to verify its contents
            println("Timetable to save: $timetable")

            // Ensure timetable has all required fields
            if (isValidTimetable(timetable)) {
                val docRef = userCollection.document()
                batch.set(docRef, timetable)
                documentIds.add(docRef.id)
            } else {
                println("Invalid timetable found: $timetable")
            }
        }

        try {
            batch.commit().get() // Synchronous commit to catch errors
            println("Successfully saved ${documentIds.size} timetables")
            return documentIds
        } catch (e: Exception) {
            println("Error saving timetables: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // Get all timetables for a specific user
    fun getTimetables(userId: String): List<TimeTable> {
        // Clean the userId by removing 'userId=' prefix if present
        val cleanUserId = userId.removePrefix("userId=").trim()

        println("Cleaned User ID: $cleanUserId")

        val collectionRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")

        val snapshot = collectionRef.get().get()

        println("Total documents found: ${snapshot.documents.size}")

        return snapshot.documents.mapNotNull { document ->
            document.toObject(TimeTable::class.java)?.copy(id = document.id)
        }
    }

    // Get timetables for a specific group within a user's collection
    fun getTimeTablesByGroup(userId: String, group: String): List<TimeTable> {
        val query = firestore.collection(usersCollection)
            .document(userId)
            .collection("timetables")
            .whereEqualTo("group", group)
            .get()
            .get()

        return query.documents.mapNotNull {
            it.toObject(TimeTable::class.java)?.copy(id = it.id)
        }
    }

    // Get a specific timetable by ID within a user's collection
    fun getTimeTable(userId: String, timetableId: String): TimeTable? {
        val docRef = firestore.collection(usersCollection)
            .document(userId)
            .collection("timetables")
            .document(timetableId)

        val snapshot = docRef.get().get()
        return if (snapshot.exists()) {
            snapshot.toObject(TimeTable::class.java)?.copy(id = snapshot.id)
        } else {
            null
        }
    }

    // Delete all timetables for a specific user
    fun deleteAllTimetables(userId: String) {
        val cleanUserId = userId.removePrefix("userId=").trim()
        val collectionRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")

        val documents = collectionRef.get().get()
        val batch = firestore.batch()

        documents.documents.forEach { document ->
            batch.delete(document.reference)
        }

        batch.commit()
    }

    // Delete a specific timetable for a user
    fun deleteTimetable(userId: String, timetableId: String) {
        val docRef = firestore.collection(usersCollection)
            .document(userId)
            .collection("timetables")
            .document(timetableId)

        docRef.delete()
    }
}

// Validation helper function
private fun isValidTimetable(timetable: TimeTable): Boolean {
    // Add your validation logic here
    // For example:
    return timetable.group.isNotBlank() &&
            timetable.day.isNotBlank()
}