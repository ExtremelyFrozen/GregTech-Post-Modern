package com.gregtechceu.gtceu.data.pattern.binary

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.data.pattern.json.PatternJsonCodec

import net.minecraft.resources.ResourceLocation

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import com.google.gson.JsonParser
import org.jspecify.annotations.NullMarked

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32

@NullMarked
object PatternBinaryCodec {
	private const val MAGIC = 0x4754504D
	private const val COMPRESSION_ZSTD = 1
	private const val HEADER_SIZE = Int.SIZE_BYTES * 4
	private val zstdLevel = Zstd.maxCompressionLevel()
	private const val MAX_PAYLOAD_BYTES = 16 * 1024 * 1024
	private val maxFileBytes = HEADER_SIZE + Zstd.compressBound(MAX_PAYLOAD_BYTES.toLong())

	@JvmStatic
	@Throws(IOException::class)
	fun read(file: Path): PatternDefinition = read(file, fileResource(file))

	@JvmStatic
	@Throws(IOException::class)
	fun read(file: Path, structure: ResourceLocation): PatternDefinition = FileChannel.open(file).use { channel ->
		if (channel.size() > maxFileBytes) throw IOException("Compressed pattern definition exceeds $maxFileBytes bytes: $file")
		val input = DataInputStream(Channels.newInputStream(channel).buffered())
		val magic = input.readInt()
		val compression = input.readInt()
		val uncompressedLength = input.readInt()
		val checksum = input.readInt()
		if (magic != MAGIC) throw IOException("Invalid pattern binary magic in $file")
		if (compression != COMPRESSION_ZSTD) throw IOException("Unsupported pattern binary compression $compression in $file")
		if (uncompressedLength !in 1..MAX_PAYLOAD_BYTES) throw IOException("Invalid pattern binary length $uncompressedLength in $file")

		val payload = ByteArray(uncompressedLength)
		ZstdInputStream(input).use { decoded ->
			if (decoded.readNBytes(payload, 0, payload.size) != payload.size || decoded.read() != -1) {
				throw IOException("Pattern binary length mismatch in $file")
			}
		}
		if (crc32(payload) != checksum) throw IOException("Pattern binary checksum mismatch in $file")
		decodePayload(payload, file, structure)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun write(file: Path, definition: PatternDefinition) {
		val payload = encodePayload(definition)
		if (payload.size > MAX_PAYLOAD_BYTES) throw IOException("Pattern binary payload exceeds $MAX_PAYLOAD_BYTES bytes: $file")
		val compressed = compress(payload)
		val header = ByteBuffer.allocate(HEADER_SIZE)
			.putInt(MAGIC).putInt(COMPRESSION_ZSTD)
			.putInt(payload.size).putInt(crc32(payload))
			.flip()
		val parent = file.toAbsolutePath().normalize().parent
			?: throw IOException("Pattern binary has no parent directory: $file")
		Files.createDirectories(parent)
		val temporary = Files.createTempFile(parent, file.fileName.toString(), ".tmp")
		try {
			FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
				writeFully(channel, header)
				writeFully(channel, ByteBuffer.wrap(compressed))
				channel.force(true)
			}
			try {
				Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
			} catch (exception: AtomicMoveNotSupportedException) {
				throw IOException("Atomic replacement is not supported for pattern binary $file", exception)
			}
		} finally {
			Files.deleteIfExists(temporary)
		}
	}

	private fun encodePayload(definition: PatternDefinition): ByteArray {
		val json = PatternJsonCodec.encode(definition).toString().toByteArray(StandardCharsets.UTF_8)
		return ByteArrayOutputStream(json.size + Int.SIZE_BYTES).use { bytes ->
			DataOutputStream(bytes).use { output ->
				writeVarInt(output, json.size)
				output.write(json)
			}
			bytes.toByteArray()
		}
	}

	private fun decodePayload(payload: ByteArray, file: Path, structure: ResourceLocation): PatternDefinition {
		DataInputStream(ByteArrayInputStream(payload)).use { input ->
			val length = readVarInt(input)
			if (length != input.available()) throw IOException("Pattern binary JSON length mismatch in $file")
			return PatternJsonCodec.decode(structure, JsonParser.parseReader(input.reader(StandardCharsets.UTF_8)))
		}
	}

	private fun fileResource(file: Path): ResourceLocation = ResourceLocation.parse("gtpm:" + file.fileName.toString().removeSuffix(".bin.zst"))

	private fun compress(payload: ByteArray): ByteArray = ByteArrayOutputStream(payload.size).use { bytes ->
		ZstdOutputStream(bytes, zstdLevel).use { output ->
			output.write(payload)
		}
		bytes.toByteArray()
	}

	private fun writeFully(channel: FileChannel, buffer: ByteBuffer) {
		while (buffer.hasRemaining()) channel.write(buffer)
	}

	private fun crc32(bytes: ByteArray): Int = CRC32().apply { update(bytes) }.value.toInt()

	private fun writeVarInt(output: DataOutputStream, value: Int) {
		var current = value
		while (current and 0x7F.inv() != 0) {
			output.writeByte((current and 0x7F) or 0x80)
			current = current ushr 7
		}
		output.writeByte(current)
	}

	private fun readVarInt(input: DataInputStream): Int {
		var result = 0
		var shift = 0
		while (shift < 35) {
			val next = input.readUnsignedByte()
			result = result or ((next and 0x7F) shl shift)
			if (next and 0x80 == 0) return result
			shift += 7
		}
		throw IOException("Invalid pattern binary varint")
	}
}
