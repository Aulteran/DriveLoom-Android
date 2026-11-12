package io.github.aulteran.driveloom.core.fuel

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A chosen file, classified and prepared for the import endpoints. */
class ImportPayload(val format: String, val data: JsonElement) {
    companion object {
        /** The server's request limit for imports. */
        const val MAX_BYTES = 8 * 1024 * 1024

        sealed interface Result {
            data class Ok(val payload: ImportPayload) : Result
            data class Invalid(val message: String) : Result
        }

        /**
         * JSON files are sent as a document, anything else as CSV text. The
         * server validates every row; this only rejects what cannot be sent.
         */
        fun from(fileName: String, mime: String, bytes: ByteArray, requireJson: Boolean = false): Result {
            if (bytes.isEmpty()) return Result.Invalid("That file is empty.")
            if (bytes.size > MAX_BYTES) return Result.Invalid("That file is larger than 8 MB.")
            val text = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
            val looksJson = fileName.endsWith(".json", ignoreCase = true) || mime.contains("json", ignoreCase = true) || text.trimStart().startsWith("{")
            if (looksJson) {
                val element = try {
                    Json.parseToJsonElement(text)
                } catch (_: SerializationException) {
                    return Result.Invalid("That file is not valid JSON.")
                }
                if (element !is JsonObject) return Result.Invalid("That file is not a DriveLoom export.")
                return Result.Ok(ImportPayload("json", element))
            }
            if (requireJson) return Result.Invalid("Choose a DriveLoom JSON export.")
            return Result.Ok(ImportPayload("csv", JsonPrimitive(text)))
        }
    }
}
