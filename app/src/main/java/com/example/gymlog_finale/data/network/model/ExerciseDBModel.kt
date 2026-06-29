package com.example.gymlog_finale.data.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ExerciseDBItem(
    val bodyPart: String? = null,
    val equipment: String? = null,
    val gifUrl: String? = null,
    val id: String? = null,
    @SerialName("exerciseId") val exerciseId: String? = null,
    val name: String? = null,
    val target: String? = null,
    val secondaryMuscles: List<String> = emptyList(),
    @Serializable(with = InstructionsSerializer::class)
    val instructions: List<String> = emptyList(),
    val description: String? = null,
    val difficulty: String? = null,
    val category: String? = null
)

object InstructionsSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instructions", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? kotlinx.serialization.json.JsonDecoder ?: return emptyList()
        val element = input.decodeJsonElement()
        return try {
            element.jsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            listOf(element.jsonPrimitive.content)
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        // Simple serialization back to a single string if needed, 
        // though we mostly care about deserialization from the API.
        encoder.encodeString(value.joinToString("\n"))
    }
}
