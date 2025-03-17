package services

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import models.TimeTable
import models.UploadResponse
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class FirestoreService {
    private val firestore: Firestore
    private val usersCollection = "users" // Main users collection

    init {
        // Load credentials from environment variable or file
        val options = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId("server-uniplan")
            .setDatabaseId("timetable")

        // Google App Engine will use the service account automatically
        // This is for local development
        val credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            ?: "service-account.json"

        try {
            val credentials = FileInputStream(credentialsPath).use {
                FirestoreOptions.getDefaultInstance().service.options.credentialsProvider.credentials
            }
            options.setCredentials(credentials)
        } catch (e: Exception) {
            // In production, will use the default credentials
            println("Using default credentials: ${e.message}")
        }

        firestore = options.build().service
    }

    // Save timetables under the user's subcollection with duplicate check
    fun saveTimetables(userId: String, timetables: List<TimeTable>): UploadResponse {
        println("Saving timetables for user: $userId")
        println("Number of timetables to save: ${timetables.size}")

        val batch = firestore.batch()
        val documentIds = mutableListOf<String>()
        val duplicates = mutableListOf<TimeTable>()

        val userCollection = firestore.collection(usersCollection)
            .document(userId)
            .collection("timetables")

        // First, check for duplicates
        timetables.forEach { timetable ->
            // Check if a similar timetable already exists
            val query = userCollection
                .whereEqualTo("group", timetable.group)
                .whereEqualTo("day", timetable.day)
                .whereEqualTo("date", timetable.date)
                .whereEqualTo("startTime", timetable.startTime)
                .whereEqualTo("className", timetable.className)
                .whereEqualTo("teacherName", timetable.teacherName)
                .limit(1)
                .get()
                .get(30, TimeUnit.SECONDS)

            if (query.isEmpty) {
                // No duplicate found, add to batch
                if (isValidTimetable(timetable)) {
                    val docRef = userCollection.document()
                    batch.set(docRef, timetable)
                    documentIds.add(docRef.id)
                }
            } else {
                // Duplicate found
                duplicates.add(timetable)
            }
        }

        try {
            // Only commit if there are non-duplicate entries
            if (documentIds.isNotEmpty()) {
                batch.commit().get(30, TimeUnit.SECONDS)
                println("Successfully saved ${documentIds.size} timetables")
            }

            return UploadResponse(
                message = "Upload successful",
                note = "Saved ${documentIds.size} timetables, ${duplicates.size} duplicates skipped",
                count = documentIds.size,
                ids = documentIds
            )
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

        println("Getting timetables for user: $cleanUserId")

        val collectionRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")

        val snapshot = collectionRef.get().get(30, TimeUnit.SECONDS)

        println("Total documents found: ${snapshot.documents.size}")

        return snapshot.documents.mapNotNull { document ->
            document.toObject(TimeTable::class.java)?.copy(id = document.id)
        }
    }

    // Get timetables for a specific group within a user's collection
    fun getTimeTablesByGroup(userId: String, group: String): List<TimeTable> {
        val cleanUserId = userId.removePrefix("userId=").trim()
        val query = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")
            .whereEqualTo("group", group)
            .get()
            .get(30, TimeUnit.SECONDS)

        return query.documents.mapNotNull {
            it.toObject(TimeTable::class.java)?.copy(id = it.id)
        }
    }

    // Get a specific timetable by ID within a user's collection
    fun getTimeTable(userId: String, timetableId: String): TimeTable? {
        val cleanUserId = userId.removePrefix("userId=").trim()
        val docRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")
            .document(timetableId)

        val snapshot = docRef.get().get(30, TimeUnit.SECONDS)
        return if (snapshot.exists()) {
            snapshot.toObject(TimeTable::class.java)?.copy(id = snapshot.id)
        } else {
            null
        }
    }

    // Delete all timetables for a specific user
    fun deleteAllTimetables(userId: String): Int {
        val cleanUserId = userId.removePrefix("userId=").trim()
        val collectionRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")

        val documents = collectionRef.get().get(30, TimeUnit.SECONDS)
        val batch = firestore.batch()
        val count = documents.size()

        documents.documents.forEach { document ->
            batch.delete(document.reference)
        }

        batch.commit().get(30, TimeUnit.SECONDS)
        return count
    }

    // Delete a specific timetable for a user
    fun deleteTimetable(userId: String, timetableId: String): Boolean {
        val cleanUserId = userId.removePrefix("userId=").trim()
        val docRef = firestore.collection(usersCollection)
            .document(cleanUserId)
            .collection("timetables")
            .document(timetableId)

        return try {
            docRef.delete().get(30, TimeUnit.SECONDS)
            true
        } catch (e: Exception) {
            false
        }
    }
}

// Improved validation helper function
private fun isValidTimetable(timetable: TimeTable): Boolean {
    return timetable.group.isNotBlank() &&
            timetable.day.isNotBlank() &&
            timetable.startTime.isNotBlank() &&
            timetable.endTime.isNotBlank() &&
            timetable.className.isNotBlank() &&
            timetable.teacherName.isNotBlank()
}