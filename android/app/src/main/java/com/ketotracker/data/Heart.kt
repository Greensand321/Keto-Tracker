package com.ketotracker.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Not annotated with @Serializable — that annotation on an enum generates a companion
// object referencing kotlinx.serialization internal APIs, which Android Studio's layoutlib
// preview renderer stubs out (causing a NoClassDefFoundError on Heart.<clinit>).
// HeartSerializer achieves the same encoding using only the public API, so previews work.
enum class Heart { GOOD, MILD, BAD }

internal object HeartSerializer : KSerializer<Heart> {
    override val descriptor = PrimitiveSerialDescriptor("Heart", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Heart) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): Heart =
        Heart.entries.firstOrNull { it.name == decoder.decodeString() } ?: Heart.GOOD
}
