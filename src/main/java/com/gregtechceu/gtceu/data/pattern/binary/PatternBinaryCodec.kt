package com.gregtechceu.gtceu.data.pattern.binary

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.data.pattern.json.PatternJsonCodec

import net.minecraft.resources.ResourceLocation

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
	private const val FORMAT_VERSION = 1
	private const val COMPRESSION_ZSTD = 1
	private const val HEADER_SIZE = Int.SIZE_BYTES * 5
	private const val MAX_PAYLOAD_BYTES = 16 * 1024 * 1024
	private const val MAX_JSON_BYTES = MAX_PAYLOAD_BYTES - Int.SIZE_BYTES
	private const val ZSTD_LEVEL = 5

	@JvmStatic
	@Throws(IOException::class)
	fun read(file: Path): PatternDefinition {
		val encoded = Files.readAllBytes(file)
		if (encoded.size <= HEADER_SIZE) throw IOException("Compressed pattern definition is shorter than its header: $file")
		val header = ByteBuffer.wrap(encoded, 0, HEADER_SIZE)
		val magic = header.int
		val version = header.int
		val compression = header.int
		val uncompressedLength = header.int
		val checksum = header.int
		if (magic != MAGIC) throw IOException("Invalid pattern binary magic in $file")
		if (version != FORMAT_VERSION) throw IOException("Unsupported pattern binary version $version in $file")
		if (compression != COMPRESSION_ZSTD) throw IOException("Unsupported pattern binary compression $compression in $file")
		if (uncompressedLength <= 0 || uncompressedLength > MAX_PAYLOAD_BYTES) throw IOException("Invalid pattern binary payload length $uncompressedLength")
		val compressed = encoded.copyOfRange(HEADER_SIZE, encoded.size)
		val payload = decompress(compressed, uncompressedLength)
		if (crc32(payload) != checksum) throw IOException("Pattern binary checksum mismatch in $file")
		return decodePayload(payload, file)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun write(file: Path, definition: PatternDefinition) {
		val payload = encodePayload(definition)
		val compressed = compress(payload)
		val header = ByteBuffer.allocate(HEADER_SIZE)
			.putInt(MAGIC).putInt(FORMAT_VERSION).putInt(COMPRESSION_ZSTD)
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
		require(json.size <= MAX_JSON_BYTES) { "Pattern JSON payload exceeds binary limit" }
		return ByteArrayOutputStream(json.size + Int.SIZE_BYTES).use { bytes ->
			DataOutputStream(bytes).use { output ->
				writeVarInt(output, json.size)
				output.write(json)
			}
			bytes.toByteArray()
		}
	}

	private fun decodePayload(payload: ByteArray, file: Path): PatternDefinition {
		DataInputStream(ByteArrayInputStream(payload)).use { input ->
			val length = readVarInt(input)
			if (length <= 0 || length > MAX_JSON_BYTES || length > input.available()) throw IOException("Invalid pattern JSON length in $file")
			val json = input.readNBytes(length)
			if (json.size != length || input.available() != 0) throw IOException("Trailing pattern binary data in $file")
			return PatternJsonCodec.decode(fileResource(file), JsonParser.parseString(String(json, StandardCharsets.UTF_8)))
		}
	}

	private fun fileResource(file: Path): ResourceLocation = ResourceLocation.parse("gtpm:" + file.fileName.toString().removeSuffix(".bin.zst"))

	private fun compress(payload: ByteArray): ByteArray = ByteArrayOutputStream(payload.size).use { bytes ->
		ZstdOutputStream(bytes, ZSTD_LEVEL).use { output ->
			output.write(payload)
		}
		bytes.toByteArray()
	}

	private fun decompress(compressed: ByteArray, expectedLength: Int): ByteArray = ZstdInputStream(ByteArrayInputStream(compressed)).use { input ->
		ByteArrayOutputStream(expectedLength).use { bytes ->
			input.transferTo(bytes)
			bytes.toByteArray().also { payload ->
				if (payload.size != expectedLength) throw IOException("Pattern binary length mismatch")
			}
		}
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
