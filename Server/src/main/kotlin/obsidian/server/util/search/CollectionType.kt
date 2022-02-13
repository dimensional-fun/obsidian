package obsidian.server.util.search

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import javax.naming.OperationNotSupportedException

@Serializable(with = CollectionType.Serializer::class)
sealed class CollectionType {
    object Album : CollectionType()

    object Playlist : CollectionType()

    object SearchResult : CollectionType()

    data class Unknown(val name: String) : CollectionType()

    companion object Serializer: KSerializer<CollectionType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("obsidian.server.util.search.CollectionType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): CollectionType {
            throw OperationNotSupportedException() // we're only implementing KSerializer to use @Serializer
        }

        override fun serialize(encoder: Encoder, value: CollectionType) {
            val name = when (value) {
                is Unknown -> value.name
                else -> requireNotNull(value::class.simpleName)
            }

            encoder.encodeString(name)
        }

    }
}
