package routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.TimeTable
import models.UploadResponse
import repositories.TimeTableRepository
import services.ExcelService
import services.FirestoreService
import java.io.File

fun Route.timeTableRoutes(
    excelService: ExcelService, firestoreService: FirestoreService
) {
    get("/") {
        call.respondText("Assalomu Alaykum!")
    }

    post("/api/upload") {
        val multipart = call.receiveMultipart()
        var file: File? = null
        var userId: String? = null

        try {
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "userId") {
                            userId = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        val tempFile = File.createTempFile("upload", ".xlsx")
                        part.streamProvider().use { input ->
                            tempFile.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }

                        println("Uploaded file size: ${tempFile.length()} bytes")
                        if (tempFile.length() == 0L) {
                            return@forEachPart call.respond(
                                HttpStatusCode.BadRequest,
                                UploadResponse(
                                    message = "Error",
                                    error = "The uploaded file is empty"
                                )
                            )
                        }

                        file = tempFile
                    }

                    else -> part.dispose()
                }
            }

            if (file == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    UploadResponse(
                        message = "Error",
                        error = "No file uploaded"
                    )
                )
                return@post
            }

            if (userId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    UploadResponse(
                        message = "Error",
                        error = "User ID is missing"
                    )
                )
                return@post
            }

            // Process the uploaded file
            val timeTables = excelService.parseExcelFile(file!!.inputStream())

            // Save timetables associated with the user ID
            val ids = firestoreService.saveTimetables(userId!!, timeTables)

            call.respond(
                HttpStatusCode.OK,
                UploadResponse(
                    message = "Upload successful",
                    note = "user id :$userId",
                    count = timeTables.size,
                    ids = ids.ids
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                UploadResponse(
                    message = "Error",
                    error = e.message ?: e.toString()
                )
            )
        } finally {
            file?.delete()
        }
    }

    // Get all timetables for a specific user
    get("/api/timetable/{userId}") {
        try {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "User ID is missing"
                )
            )
            val timeTables = firestoreService.getTimetables(userId)
            call.respond(HttpStatusCode.OK, timeTables)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                UploadResponse(
                    message = "Error",
                    error = e.message ?: e.toString()
                )
            )
        }
    }

    // Get timetables by group for a specific user
    get("/api/group/{userId}/{group}") {
        try {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "User ID is missing"
                )
            )
            val group = call.parameters["group"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "Group parameter is missing"
                )
            )
            val timeTables = firestoreService.getTimeTablesByGroup(userId, group)
            call.respond(HttpStatusCode.OK, timeTables)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                UploadResponse(
                    message = "Error",
                    error = e.message ?: e.toString()
                )
            )
        }
    }

    // Get timetable by id for a specific user
    get("/api/{userId}/{id}") {
        try {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "User ID is missing"
                )
            )
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "Timetable ID is missing"
                )
            )
            val timeTable = firestoreService.getTimeTable(userId, id) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                UploadResponse(
                    message = "Error",
                    error = "Time Table not found"
                )
            )
            call.respond(HttpStatusCode.OK, timeTable)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                UploadResponse(
                    message = "Error",
                    error = e.message ?: e.toString()
                )
            )
        }
    }

    // Delete all timetables for a specific user
    delete("/api/timetable/{userId}") {
        try {
            val userId = call.parameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                UploadResponse(
                    message = "Error",
                    error = "User ID is missing"
                )
            )
            firestoreService.deleteAllTimetables(userId)
            call.respond(
                HttpStatusCode.OK,
                UploadResponse(
                    message = "All timetables deleted successfully $userId"
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                UploadResponse(
                    message = "Error",
                    error = e.message ?: e.toString()
                )
            )
        }
    }

    // Delete a specific timetable for a user
//    delete("/api/timetable/{userId}/{id}") {
//        try {
//            val userId = call.parameters["userId"] ?: return@delete call.respond(
//                HttpStatusCode.BadRequest,
//                UploadResponse(
//                    message = "Error",
//                    error = "User ID is missing"
//                )
//            )
//            val id = call.parameters["id"] ?: return@delete call.respond(
//                HttpStatusCode.BadRequest,
//                UploadResponse(
//                    message = "Error",
//                    error = "Timetable ID is missing"
//                )
//            )
//            firestoreService.deleteAllTimetables(userId)
//            call.respond(
//                HttpStatusCode.OK,
//                UploadResponse(
//                    message = "Timetable deleted successfully"
//                )
//            )
//        } catch (e: Exception) {
//            call.respond(
//                HttpStatusCode.InternalServerError,
//                UploadResponse(
//                    message = "Error",
//                    error = e.message ?: e.toString()
//                )
//            )
//        }
//    }

    // Commented out the previous download and global delete routes
    // as they no longer make sense with user-specific operations

    get("/_ah/warmup") {
        call.respondText("OK", status = HttpStatusCode.OK)
    }

    get("/health") {
        call.respondText("OK", status = HttpStatusCode.OK)
    }
}