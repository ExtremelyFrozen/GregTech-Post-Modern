package com.gregtechceu.gtceu.data.pattern.json

import com.google.gson.*
import com.gregtechceu.gtceu.api.multiblock.pattern.model.*
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.resources.ResourceLocation
import org.jspecify.annotations.NullMarked
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path

@NullMarked
object PatternJsonCodec {
    private val topLevelFields = setOf("machine", "axes", "orientation", "origin", "parameters", "fragments", "predicates", "body", "constraints")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: Path, structure: ResourceLocation): PatternDefinition = Files.newBufferedReader(file).use { decode(structure, JsonParser.parseReader(it)) }

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: Path): PatternDefinition = read(file, ResourceLocation.parse("gtpm:unknown"))

    @JvmStatic
    fun decode(structure: ResourceLocation, input: JsonElement): PatternDefinition {
        val root = obj(input, "$")
        rejectUnknown(root, topLevelFields, "$")
        val machine = if (root.has("machine")) ResourceLocation.parse(str(root["machine"], "$.machine")) else structure
        return PatternDefinition(machine, structure, axes(root["axes"]), orientation(root["orientation"]), origin(root["origin"]),
            parameters(root["parameters"]), fragments(root["fragments"]), predicates(root["predicates"]),
            node(required(root, "body", "$.body"), "$.body"), constraints(root["constraints"]))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(file: Path, definition: PatternDefinition) {
        file.toAbsolutePath().normalize().parent?.let(Files::createDirectories)
            ?: throw IOException("Pattern JSON has no parent directory: $file")
        Files.newBufferedWriter(file).use { writer: Writer -> gson.toJson(encode(definition), writer) }
    }

    @JvmStatic
    fun encode(definition: PatternDefinition): JsonObject = JsonObject().apply {
        addProperty("machine", definition.machine().toString())
        add("axes", encodeAxes(definition.axes()))
        add("orientation", encodeOrientation(definition.orientation()))
        add("origin", encodeOrigin(definition.origin()))
        add("parameters", encodeParameters(definition.parameters()))
        add("fragments", JsonObject().also { out -> definition.fragments().forEach { (id, value) -> out.add(id, encodeFragment(value)) } })
        add("predicates", JsonObject().also { out -> definition.predicates().forEach { (symbol, value) -> out.add(symbol.toString(), encodePredicate(value)) } })
        add("body", encodeNode(definition.body()))
        add("constraints", JsonArray().also { out -> definition.constraints().forEach { out.add(encodeConstraint(it)) } })
    }

    private fun axes(element: JsonElement?): PatternAxes {
        if (element == null) return PatternAxes.defaults()
        val value = obj(element, "$.axes")
        rejectUnknown(value, setOf("depth", "height", "width"), "$.axes")
        return PatternAxes(direction(required(value, "depth", "$.axes.depth"), "$.axes.depth"),
            direction(required(value, "height", "$.axes.height"), "$.axes.height"), direction(required(value, "width", "$.axes.width"), "$.axes.width"))
    }

    private fun orientation(element: JsonElement?): PatternOrientation {
        if (element == null) return PatternOrientation.defaults()
        val value = obj(element, "$.orientation")
        rejectUnknown(value, setOf("allowMirror", "allowExtendedFacing"), "$.orientation")
        return PatternOrientation(booleanValue(value["allowMirror"], "$.orientation.allowMirror"), booleanValue(value["allowExtendedFacing"], "$.orientation.allowExtendedFacing"))
    }

    private fun origin(element: JsonElement?): PatternOrigin {
        if (element == null) return PatternOrigin.controller()
        val value = obj(element, "$.origin")
        rejectUnknown(value, setOf("token", "offset"), "$.origin")
        return PatternOrigin(str(required(value, "token", "$.origin.token"), "$.origin.token"), offset(value["offset"], "$.origin.offset"))
    }

    private fun parameters(element: JsonElement?): Map<String, PatternParameter> {
        if (element == null) return emptyMap()
        val result = Object2ObjectLinkedOpenHashMap<String, PatternParameter>()
        obj(element, "$.parameters").entrySet().forEach { (name, value) -> result[name] = parameter(value, "$.parameters.$name") }
        return result
    }

    private fun parameter(element: JsonElement, path: String): PatternParameter {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) return parameterType(element.asString, null, path)
        val value = obj(element, path)
        rejectUnknown(value, setOf("type", "min", "max"), path)
        return parameterType(str(required(value, "type", "$path.type"), "$path.type"), value, path)
    }

    private fun parameterType(type: String, value: JsonObject?, path: String): PatternParameter = when (type) {
        "token" -> PatternParameter.Token()
        "predicate" -> PatternParameter.Predicate()
        "direction" -> PatternParameter.Direction()
        "fragment" -> PatternParameter.Fragment()
        "integer" -> PatternParameter.IntegerRange(integer(value?.get("min"), "$path.min"), integer(value?.get("max"), "$path.max"))
        else -> fail("Unknown parameter type '$type' at $path")
    }

    private fun fragments(element: JsonElement?): Map<String, PatternFragment> {
        if (element == null) return emptyMap()
        val result = Object2ObjectLinkedOpenHashMap<String, PatternFragment>()
        obj(element, "$.fragments").entrySet().forEach { (id, value) ->
            val path = "$.fragments.$id"; val fragment = obj(value, path)
            rejectUnknown(fragment, setOf("parameters", "anchors", "ports", "body"), path)
            val anchors = Object2ObjectLinkedOpenHashMap<String, PatternOrigin.Offset>()
            fragment["anchors"]?.let { obj(it, "$path.anchors").entrySet().forEach { (name, value) -> anchors[name] = offset(value, "$path.anchors.$name") } }
            val ports = Object2ObjectLinkedOpenHashMap<String, PatternFragment.Port>()
            fragment["ports"]?.let { obj(it, "$path.ports").entrySet().forEach { (name, value) ->
                val portPath = "$path.ports.$name"; val port = obj(value, portPath)
                rejectUnknown(port, setOf("offset", "direction"), portPath)
                ports[name] = PatternFragment.Port(offset(required(port, "offset", "$portPath.offset"), "$portPath.offset"), direction(required(port, "direction", "$portPath.direction"), "$portPath.direction"))
            } }
            result[id] = PatternFragment(parameters(fragment["parameters"]), anchors, ports, node(required(fragment, "body", "$path.body"), "$path.body"))
        }
        return result
    }

    private fun predicates(element: JsonElement?): Map<Char, PatternPredicateDefinition> {
        if (element == null) return emptyMap()
        val result = Object2ObjectLinkedOpenHashMap<Char, PatternPredicateDefinition>()
        obj(element, "$.predicates").entrySet().forEach { (symbol, value) ->
            if (symbol.length != 1) fail("Predicate key must contain one character at $.predicates")
            val path = "$.predicates.$symbol"; val predicate = obj(value, path)
            val type = str(required(predicate, "type", "$path.type"), "$path.type")
            val name = predicate["name"]?.let { str(it, "$path.name") } ?: ""
            val facts = ObjectArrayList<String>(); predicate["facts"]?.let { array(it, "$path.facts").forEach { facts.add(str(it, "$path.facts")) } }
            val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
            predicate.entrySet().forEach { (key, property) -> if (key != "type" && key != "name" && key != "facts") valueOf(property)?.let { properties[key] = it } }
            result[symbol[0]] = PatternPredicateDefinition(type, name, properties, facts)
        }
        return result
    }

    private fun node(element: JsonElement, path: String): PatternNode {
        val value = obj(element, path); val type = str(required(value, "type", "$path.type"), "$path.type")
        return when (type) {
            "fixed" -> { rejectUnknown(value, setOf("type", "layers"), path); PatternNode.Fixed(layers(required(value, "layers", "$path.layers"), "$path.layers")) }
            "sequence" -> { rejectUnknown(value, setOf("type", "axis", "children"), path); PatternNode.Sequence(direction(required(value, "axis", "$path.axis"), "$path.axis"), array(required(value, "children", "$path.children"), "$path.children").mapIndexed { index, child -> node(child, "$path.children[$index]") }) }
            "repeat" -> { rejectUnknown(value, setOf("type", "id", "axis", "direction", "min", "max", "body"), path); PatternNode.Repeat(str(required(value, "id", "$path.id"), "$path.id"), direction(required(value, "axis", "$path.axis"), "$path.axis"), PatternRepeatDirection.valueOf(str(required(value, "direction", "$path.direction"), "$path.direction").uppercase()), integer(required(value, "min", "$path.min"), "$path.min"), integer(required(value, "max", "$path.max"), "$path.max"), node(required(value, "body", "$path.body"), "$path.body")) }
            "choice" -> { rejectUnknown(value, setOf("type", "id", "alternatives"), path); PatternNode.Choice(str(required(value, "id", "$path.id"), "$path.id"), array(required(value, "alternatives", "$path.alternatives"), "$path.alternatives").mapIndexed { index, item -> val alternativePath = "$path.alternatives[$index]"; val alternative = obj(item, alternativePath); rejectUnknown(alternative, setOf("id", "node"), alternativePath); PatternNode.Choice.Alternative(str(required(alternative, "id", "$alternativePath.id"), "$alternativePath.id"), node(required(alternative, "node", "$alternativePath.node"), "$alternativePath.node")) }) }
            "fragment" -> { rejectUnknown(value, setOf("type", "id", "bindings"), path); val bindings = Object2ObjectLinkedOpenHashMap<String, PatternBinding>(); value["bindings"]?.let { obj(it, "$path.bindings").entrySet().forEach { (name, binding) -> bindings[name] = binding(binding, "$path.bindings.$name") } }; PatternNode.Fragment(str(required(value, "id", "$path.id"), "$path.id"), bindings) }
            else -> fail("Unknown pattern node type '$type' at $path")
        }
    }

    private fun binding(element: JsonElement, path: String): PatternBinding {
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) return PatternBinding.Token(element.asString)
        val value = obj(element, path); rejectUnknown(value, setOf("type", "value"), path); val type = str(required(value, "type", "$path.type"), "$path.type"); val item = required(value, "value", "$path.value")
        return when (type) { "token" -> PatternBinding.Token(str(item, "$path.value")); "predicate" -> PatternBinding.Predicate(str(item, "$path.value")); "integer" -> PatternBinding.IntegerValue(integer(item, "$path.value")); "direction" -> PatternBinding.Direction(direction(item, "$path.value")); "fragment" -> PatternBinding.Fragment(str(item, "$path.value")); else -> fail("Unknown binding type '$type' at $path") }
    }

    private fun constraints(element: JsonElement?): List<PatternConstraint> {
        if (element == null) return emptyList()
        return array(element, "$.constraints").mapIndexed { index, item ->
            val path = "$.constraints[$index]"; val value = obj(item, path); rejectUnknown(value, setOf("type", "fact", "scope", "min", "max", "message"), path)
            if (str(required(value, "type", "$path.type"), "$path.type") != "count") fail("Unknown constraint type at $path")
            PatternConstraint.Count(str(required(value, "fact", "$path.fact"), "$path.fact"), scope(required(value, "scope", "$path.scope"), "$path.scope"), integer(required(value, "min", "$path.min"), "$path.min"), integer(required(value, "max", "$path.max"), "$path.max"), value["message"]?.let { str(it, "$path.message") } ?: "")
        }
    }

    private fun scope(element: JsonElement, path: String): PatternConstraint.Scope = str(element, path).let { value -> when { value == "all" -> PatternConstraint.Scope.All(); value.startsWith("fragment:") -> PatternConstraint.Scope.Fragment(value.substring(9)); value.startsWith("node:") -> PatternConstraint.Scope.Node(value.substring(5)); value.startsWith("repeat:") -> PatternConstraint.Scope.Repeat(value.substring(7)); else -> fail("Unknown constraint scope '$value' at $path") } }

    private fun layers(element: JsonElement, path: String): List<List<String>> = array(element, path).mapIndexed { index, layer -> array(layer, "$path[$index]").map { str(it, "$path[$index]") } }
    private fun offset(element: JsonElement?, path: String): PatternOrigin.Offset { if (element == null) return PatternOrigin.Offset(0, 0, 0); val values = array(element, path); if (values.size() != 3) fail("Offset must contain exactly three integers at $path"); return PatternOrigin.Offset(integer(values[0], "$path[0]"), integer(values[1], "$path[1]"), integer(values[2], "$path[2]")) }
    private fun direction(element: JsonElement, path: String): PatternDirection = runCatching { PatternDirection.valueOf(str(element, path).uppercase()) }.getOrElse { fail("Unknown direction at $path") }
    private fun required(obj: JsonObject, name: String, path: String): JsonElement = obj[name] ?: fail("Missing required field at $path")
    private fun obj(element: JsonElement, path: String): JsonObject = element.takeIf { it.isJsonObject }?.asJsonObject ?: fail("Expected object at $path")
    private fun array(element: JsonElement, path: String): JsonArray = element.takeIf { it.isJsonArray }?.asJsonArray ?: fail("Expected array at $path")
    private fun str(element: JsonElement, path: String): String = element.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: fail("Expected string at $path")
    private fun integer(element: JsonElement?, path: String): Int = element?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: fail("Expected integer at $path")
    private fun booleanValue(element: JsonElement?, path: String): Boolean {
        if (element == null) return false
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isBoolean) fail("Expected boolean at $path")
        return element.asBoolean
    }
    private fun rejectUnknown(obj: JsonObject, allowed: Set<String>, path: String) { obj.keySet().firstOrNull { it !in allowed }?.let { fail("Unknown field '$it' at $path") } }
    private fun valueOf(element: JsonElement): Any? = when { element.isJsonNull -> null; element.isJsonPrimitive -> element.asJsonPrimitive.let { if (it.isBoolean) it.asBoolean else if (it.isNumber) it.asNumber else it.asString }; element.isJsonArray -> ObjectArrayList(element.asJsonArray.map(::valueOf)); else -> Object2ObjectLinkedOpenHashMap<String, Any?>().also { element.asJsonObject.entrySet().forEach { (key, value) -> it[key] = valueOf(value) } } }
    private fun encodeAxes(value: PatternAxes) = JsonObject().apply { addProperty("depth", value.depth().name.lowercase()); addProperty("height", value.height().name.lowercase()); addProperty("width", value.width().name.lowercase()) }
    private fun encodeOrientation(value: PatternOrientation) = JsonObject().apply { addProperty("allowMirror", value.allowMirror()); addProperty("allowExtendedFacing", value.allowExtendedFacing()) }
    private fun encodeOrigin(value: PatternOrigin) = JsonObject().apply { addProperty("token", value.token()); add("offset", JsonArray().also { it.add(value.offset().depth()); it.add(value.offset().height()); it.add(value.offset().width()) }) }
    private fun encodeParameters(value: Map<String, PatternParameter>) = JsonObject().also { out -> value.forEach { (name, parameter) -> if (parameter is PatternParameter.IntegerRange) out.add(name, JsonObject().apply { addProperty("type", "integer"); addProperty("min", parameter.minimum()); addProperty("max", parameter.maximum()) }) else out.addProperty(name, parameterName(parameter)) } }
    private fun parameterName(value: PatternParameter) = when (value) { is PatternParameter.Token -> "token"; is PatternParameter.Predicate -> "predicate"; is PatternParameter.Direction -> "direction"; is PatternParameter.Fragment -> "fragment"; else -> "integer" }
    private fun encodeFragment(value: PatternFragment) = JsonObject().apply {
        add("parameters", encodeParameters(value.parameters()))
        add("anchors", JsonObject().also { out -> value.anchors().forEach { (id, offset) -> out.add(id, JsonArray().also { it.add(offset.depth()); it.add(offset.height()); it.add(offset.width()) }) } })
        add("ports", JsonObject().also { out -> value.ports().forEach { (id, port) -> out.add(id, JsonObject().apply { add("offset", JsonArray().also { it.add(port.offset().depth()); it.add(port.offset().height()); it.add(port.offset().width()) }); addProperty("direction", port.direction().name.lowercase()) }) } })
        add("body", encodeNode(value.body()))
    }
    private fun encodePredicate(value: PatternPredicateDefinition) = JsonObject().apply { addProperty("type", value.type()); if (value.name().isNotEmpty()) addProperty("name", value.name()); value.properties().forEach { (name, item) -> add(name, gson.toJsonTree(item)) }; if (value.facts().isNotEmpty()) add("facts", JsonArray().also { out -> value.facts().forEach(out::add) }) }
    private fun encodeNode(node: PatternNode): JsonObject = when (node) {
        is PatternNode.Fixed -> JsonObject().apply { addProperty("type", "fixed"); add("layers", JsonArray().also { out -> node.layers().forEach { layer -> out.add(JsonArray().also { rows -> layer.forEach(rows::add) }) } }) }
        is PatternNode.Sequence -> JsonObject().apply { addProperty("type", "sequence"); addProperty("axis", node.axis().name.lowercase()); add("children", JsonArray().also { out -> node.children().forEach { out.add(encodeNode(it)) } }) }
        is PatternNode.Repeat -> JsonObject().apply { addProperty("type", "repeat"); addProperty("id", node.id()); addProperty("axis", node.axis().name.lowercase()); addProperty("direction", node.direction().name.lowercase()); addProperty("min", node.minimum()); addProperty("max", node.maximum()); add("body", encodeNode(node.body())) }
        is PatternNode.Choice -> JsonObject().apply { addProperty("type", "choice"); addProperty("id", node.id()); add("alternatives", JsonArray().also { out -> node.alternatives().forEach { out.add(JsonObject().apply { addProperty("id", it.id()); add("node", encodeNode(it.node())) }) } }) }
        is PatternNode.Fragment -> JsonObject().apply { addProperty("type", "fragment"); addProperty("id", node.id()); if (node.bindings().isNotEmpty()) add("bindings", JsonObject().also { out -> node.bindings().forEach { (name, value) -> out.add(name, encodeBinding(value)) } }) }
    }

    private fun encodeBinding(binding: PatternBinding): JsonObject = JsonObject().apply {
        when (binding) {
            is PatternBinding.Token -> { addProperty("type", "token"); addProperty("value", binding.value()) }
            is PatternBinding.Predicate -> { addProperty("type", "predicate"); addProperty("value", binding.value()) }
            is PatternBinding.IntegerValue -> { addProperty("type", "integer"); addProperty("value", binding.value()) }
            is PatternBinding.Direction -> { addProperty("type", "direction"); addProperty("value", binding.value().name.lowercase()) }
            is PatternBinding.Fragment -> { addProperty("type", "fragment"); addProperty("value", binding.value()) }
        }
    }
    private fun encodeConstraint(value: PatternConstraint): JsonObject { val count = value as PatternConstraint.Count; return JsonObject().apply { addProperty("type", "count"); addProperty("fact", count.fact()); addProperty("scope", scopeName(count.scope())); addProperty("min", count.minimum()); addProperty("max", count.maximum()); if (count.message().isNotEmpty()) addProperty("message", count.message()) } }
    private fun scopeName(scope: PatternConstraint.Scope) = when (scope) { is PatternConstraint.Scope.All -> "all"; is PatternConstraint.Scope.Fragment -> "fragment:${scope.id()}"; is PatternConstraint.Scope.Node -> "node:${scope.id()}"; is PatternConstraint.Scope.Repeat -> "repeat:${scope.id()}" }
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
}
