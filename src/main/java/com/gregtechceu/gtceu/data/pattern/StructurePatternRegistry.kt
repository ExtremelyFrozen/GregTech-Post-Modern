package com.gregtechceu.gtceu.data.pattern

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern

import net.minecraft.resources.ResourceLocation

import org.jetbrains.annotations.ApiStatus

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** Registry and asynchronous reload coordinator for Java multiblock definitions. */
object StructurePatternRegistry {
	private val javaDefinitions = ConcurrentHashMap<StructurePatternKey, JavaDefinition>()
	private val reloadListeners = ConcurrentHashMap.newKeySet<Runnable>()
	private val reloadExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
	private val generationCounter = AtomicLong()

	@JvmStatic
	fun registerJavaDefinition(definition: MultiblockMachineDefinition, structureName: String) {
		val key = StructurePatternKey(definition.id, structureName)
		javaDefinitions[key] = JavaDefinition(key, definition, structureName)
	}

	@JvmStatic
	fun resolvePattern(definition: MultiblockMachineDefinition, structureName: String): MultiBlockPattern {
		val key = StructurePatternKey(definition.id, structureName)
		return resolvePattern(key, definition, definition.createJavaPattern(structureName))
	}

	@JvmStatic
	fun resolvePattern(key: StructurePatternKey, definition: MultiblockMachineDefinition, javaPattern: MultiBlockPattern): MultiBlockPattern = StructureCache.resolvePattern(key, definition, javaPattern)

	/** Returns the generation of the latest successfully published pattern reload. */
	@JvmStatic
	fun generation(): Long = generationCounter.get()

	@ApiStatus.Internal
	@JvmStatic
	fun addReloadListener(listener: Runnable) {
		reloadListeners.add(listener)
	}

	@ApiStatus.Internal
	@JvmStatic
	fun reloadAllPatternsAsync(): CompletableFuture<Int> = runReloadTasksAsync(null, null as ResourceLocation?)

	@ApiStatus.Internal
	@JvmStatic
	fun reloadTypePatternsAsync(type: StructureDefinitionType): CompletableFuture<Int> = runReloadTasksAsync(StructureDefinitionSource.fromDefinitionType(type), null as ResourceLocation?)

	@ApiStatus.Internal
	@JvmStatic
	fun reloadPatternAsync(machineId: ResourceLocation): CompletableFuture<Int> = runReloadTasksAsync(machineId)

	@ApiStatus.Internal
	@JvmStatic
	fun reloadPatternAsync(key: StructurePatternKey): CompletableFuture<Int> = runReloadTasksAsync(null, key)

	@ApiStatus.Internal
	@JvmStatic
	fun reloadPatternAsync(type: StructureDefinitionType, machineId: ResourceLocation): CompletableFuture<Int> = runReloadTasksAsync(StructureDefinitionSource.fromDefinitionType(type), machineId)

	@ApiStatus.Internal
	@JvmStatic
	fun reloadPatternAsync(type: StructureDefinitionType, key: StructurePatternKey): CompletableFuture<Int> = runReloadTasksAsync(StructureDefinitionSource.fromDefinitionType(type), key)

	private fun runReloadTasksAsync(source: StructureDefinitionSource?, machineId: ResourceLocation?): CompletableFuture<Int> {
		val tasks = javaDefinitions.values.asSequence()
			.filter { machineId == null || it.key.machineId == machineId }
			.filter { matchesSource(it, source) }
			.map { definition ->
				CompletableFuture.supplyAsync({ if (runTask(definition)) 1 else 0 }, reloadExecutor)
			}
			.toList()
			.toTypedArray()
		return joinReloadTasks(tasks).thenApply(::notifyReloadListeners)
	}

	private fun runReloadTasksAsync(source: StructureDefinitionSource?, key: StructurePatternKey?): CompletableFuture<Int> {
		val definition = key?.let(javaDefinitions::get)
		if (definition == null || !matchesSource(definition, source)) {
			return CompletableFuture.completedFuture(0)
		}
		return CompletableFuture.supplyAsync({ if (runTask(definition)) 1 else 0 }, reloadExecutor)
			.thenApply(::notifyReloadListeners)
	}

	private fun runReloadTasksAsync(machineId: ResourceLocation?): CompletableFuture<Int> {
		val tasks = javaDefinitions.values.asSequence()
			.filter { machineId == null || it.key.machineId == machineId }
			.map { definition ->
				CompletableFuture.supplyAsync({ if (runTask(definition)) 1 else 0 }, reloadExecutor)
			}
			.toList()
			.toTypedArray()
		return joinReloadTasks(tasks).thenApply(::notifyReloadListeners)
	}

	private fun joinReloadTasks(tasks: Array<CompletableFuture<Int>>): CompletableFuture<Int> = CompletableFuture.allOf(*tasks).thenApply { tasks.sumOf { it.join() } }

	private fun notifyReloadListeners(refreshed: Int): Int {
		if (refreshed > 0) {
			generationCounter.incrementAndGet()
			reloadListeners.forEach(Runnable::run)
		}
		return refreshed
	}

	private fun matchesSource(definition: JavaDefinition, source: StructureDefinitionSource?): Boolean = source == null || StructureCache.getActiveSource(definition.key) == source

	private fun runTask(definition: JavaDefinition): Boolean = try {
		definition.definition.reloadPattern(definition.structureName)
		true
	} catch (exception: Exception) {
		GTCEu.LOGGER.error("Failed to reload structure pattern for {}", definition.key, exception)
		false
	}

	private data class JavaDefinition(val key: StructurePatternKey, val definition: MultiblockMachineDefinition, val structureName: String)
}
