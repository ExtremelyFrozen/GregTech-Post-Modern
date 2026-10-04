package com.gregtechceu.gtceu.data.pattern.json;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternAxes;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrientation;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternParameter;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternRepeatDirection;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict schema-2 JSON codec for canonical multiblock definitions. */
@NullMarked
public final class PatternJsonCodec {

    public static final int SCHEMA = 2;
    private static final Set<String> TOP_LEVEL_FIELDS = Set.of("schema", "machine", "axes", "orientation", "origin",
            "parameters", "fragments", "predicates", "body", "constraints");

    private PatternJsonCodec() {}

    public static PatternDefinition read(Path file, ResourceLocation structure) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return decode(structure, JsonParser.parseReader(reader));
        }
    }

    public static PatternDefinition decode(ResourceLocation structure, JsonElement input) {
        JsonObject root = object(input, "$");
        rejectUnknown(root, TOP_LEVEL_FIELDS, "$");
        int schema = integer(required(root, "schema", "$"), "$.schema");
        if (schema != SCHEMA) throw error("Unsupported pattern schema " + schema + " at $.schema");
        ResourceLocation machine = root.has("machine") ?
                ResourceLocation.parse(string(root.get("machine"), "$.machine")) : structure;
        return new PatternDefinition(machine, structure, parseAxes(root.get("axes")),
                parseOrientation(root.get("orientation")),
                parseOrigin(root.get("origin")), parseParameters(root.get("parameters")),
                parseFragments(root.get("fragments")),
                parsePredicates(root.get("predicates")), parseNode(required(root, "body", "$.body"), "$.body"),
                parseConstraints(root.get("constraints")));
    }

    public static void write(Path file, PatternDefinition definition) throws IOException {
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("Pattern JSON has no parent directory: " + file);
        Files.createDirectories(parent);
        try (Writer writer = Files.newBufferedWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(encode(definition), writer);
        }
    }

    public static JsonObject encode(PatternDefinition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA);
        root.addProperty("machine", definition.machine().toString());
        root.add("axes", encodeAxes(definition.axes()));
        root.add("orientation", encodeOrientation(definition.orientation()));
        root.add("origin", encodeOrigin(definition.origin()));
        root.add("parameters", encodeParameters(definition.parameters()));
        JsonObject fragments = new JsonObject();
        definition.fragments().forEach((id, fragment) -> fragments.add(id, encodeFragment(fragment)));
        root.add("fragments", fragments);
        JsonObject predicates = new JsonObject();
        definition.predicates()
                .forEach((symbol, predicate) -> predicates.add(String.valueOf(symbol), encodePredicate(predicate)));
        root.add("predicates", predicates);
        root.add("body", encodeNode(definition.body()));
        JsonArray constraints = new JsonArray();
        definition.constraints().forEach(constraint -> constraints.add(encodeConstraint(constraint)));
        root.add("constraints", constraints);
        return root;
    }

    private static PatternAxes parseAxes(JsonElement element) {
        if (element == null) return PatternAxes.defaults();
        JsonObject object = object(element, "$.axes");
        rejectUnknown(object, Set.of("depth", "height", "width"), "$.axes");
        return new PatternAxes(direction(required(object, "depth", "$.axes.depth"), "$.axes.depth"),
                direction(required(object, "height", "$.axes.height"), "$.axes.height"),
                direction(required(object, "width", "$.axes.width"), "$.axes.width"));
    }

    private static PatternOrientation parseOrientation(JsonElement element) {
        if (element == null) return PatternOrientation.defaults();
        JsonObject object = object(element, "$.orientation");
        rejectUnknown(object, Set.of("allowMirror", "allowExtendedFacing"), "$.orientation");
        return new PatternOrientation(booleanValue(object.get("allowMirror"), false, "$.orientation.allowMirror"),
                booleanValue(object.get("allowExtendedFacing"), false, "$.orientation.allowExtendedFacing"));
    }

    private static PatternOrigin parseOrigin(JsonElement element) {
        if (element == null) return PatternOrigin.controller();
        JsonObject object = object(element, "$.origin");
        rejectUnknown(object, Set.of("token", "offset"), "$.origin");
        String token = string(required(object, "token", "$.origin.token"), "$.origin.token");
        return new PatternOrigin(token, parseOffset(object.get("offset"), "$.origin.offset"));
    }

    private static Map<String, PatternParameter> parseParameters(JsonElement element) {
        if (element == null) return Map.of();
        JsonObject object = object(element, "$.parameters");
        var result = new Object2ObjectLinkedOpenHashMap<String, PatternParameter>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(),
                parseParameter(entry.getValue(), "$.parameters." + entry.getKey())));
        return result;
    }

    private static PatternParameter parseParameter(JsonElement element, String path) {
        if (element instanceof JsonPrimitive primitive && primitive.isString())
            return parameterType(primitive.getAsString(), null, path);
        JsonObject object = object(element, path);
        rejectUnknown(object, Set.of("type", "min", "max"), path);
        return parameterType(string(required(object, "type", path + ".type")), object, path);
    }

    private static PatternParameter parameterType(String type, JsonObject object, String path) {
        return switch (type) {
            case "token" -> new PatternParameter.Token();
            case "predicate" -> new PatternParameter.Predicate();
            case "direction" -> new PatternParameter.Direction();
            case "fragment" -> new PatternParameter.Fragment();
            case "integer" -> new PatternParameter.IntegerRange(
                    integer(object == null ? null : object.get("min"), path + ".min"),
                    integer(object == null ? null : object.get("max"), path + ".max"));
            default -> throw error("Unknown parameter type '" + type + "' at " + path);
        };
    }

    private static Map<String, PatternFragment> parseFragments(JsonElement element) {
        if (element == null) return Map.of();
        JsonObject object = object(element, "$.fragments");
        var result = new Object2ObjectLinkedOpenHashMap<String, PatternFragment>();
        object.entrySet().forEach(
                entry -> result.put(entry.getKey(), parseFragment(entry.getValue(), "$.fragments." + entry.getKey())));
        return result;
    }

    private static PatternFragment parseFragment(JsonElement element, String path) {
        JsonObject object = object(element, path);
        rejectUnknown(object, Set.of("parameters", "anchors", "ports", "body"), path);
        var anchors = new Object2ObjectLinkedOpenHashMap<String, PatternOrigin.Offset>();
        if (object.has("anchors")) {
            JsonObject values = object(object.get("anchors"), path + ".anchors");
            values.entrySet().forEach(entry -> anchors.put(entry.getKey(),
                    parseOffset(entry.getValue(), path + ".anchors." + entry.getKey())));
        }
        var ports = new Object2ObjectLinkedOpenHashMap<String, PatternFragment.Port>();
        if (object.has("ports")) {
            JsonObject values = object(object.get("ports"), path + ".ports");
            values.entrySet().forEach(entry -> {
                String portPath = path + ".ports." + entry.getKey();
                JsonObject port = object(entry.getValue(), portPath);
                rejectUnknown(port, Set.of("offset", "direction"), portPath);
                ports.put(entry.getKey(), new PatternFragment.Port(
                        parseOffset(required(port, "offset", portPath + ".offset"), portPath + ".offset"),
                        direction(required(port, "direction", portPath + ".direction"), portPath + ".direction")));
            });
        }
        return new PatternFragment(parseParameters(object.get("parameters")), anchors, ports,
                parseNode(required(object, "body", path + ".body"), path + ".body"));
    }

    private static Map<Character, PatternPredicateDefinition> parsePredicates(JsonElement element) {
        if (element == null) return Map.of();
        JsonObject object = object(element, "$.predicates");
        var result = new Object2ObjectLinkedOpenHashMap<Character, PatternPredicateDefinition>();
        object.entrySet().forEach(entry -> {
            if (entry.getKey().length() != 1) throw error("Predicate key must contain one character at $.predicates");
            String path = "$.predicates." + entry.getKey();
            JsonObject predicate = object(entry.getValue(), path);
            String type = string(required(predicate, "type", path + ".type"));
            String name = predicate.has("name") ? string(predicate.get("name"), path + ".name") : "";
            var facts = new ObjectArrayList<String>();
            if (predicate.has("facts")) array(predicate.get("facts"), path + ".facts")
                    .forEach(value -> facts.add(string(value, path + ".facts")));
            var properties = new Object2ObjectLinkedOpenHashMap<String, Object>();
            predicate.entrySet().forEach(property -> {
                if (!property.getKey().equals("type") && !property.getKey().equals("name") &&
                        !property.getKey().equals("facts")) {
                    properties.put(property.getKey(), toValue(property.getValue()));
                }
            });
            result.put(entry.getKey().charAt(0), new PatternPredicateDefinition(type, name, properties, facts));
        });
        return result;
    }

    private static PatternNode parseNode(JsonElement element, String path) {
        JsonObject object = object(element, path);
        String type = string(required(object, "type", path + ".type"));
        return switch (type) {
            case "fixed" -> {
                rejectUnknown(object, Set.of("type", "layers"), path);
                yield new PatternNode.Fixed(
                        parseLayers(required(object, "layers", path + ".layers"), path + ".layers"));
            }
            case "sequence" -> {
                rejectUnknown(object, Set.of("type", "axis", "children"), path);
                var children = new ObjectArrayList<PatternNode>();
                JsonArray values = array(required(object, "children", path + ".children"), path + ".children");
                for (int index = 0; index < values.size(); index++)
                    children.add(parseNode(values.get(index), path + ".children[" + index + "]"));
                yield new PatternNode.Sequence(direction(required(object, "axis", path + ".axis"), path + ".axis"),
                        children);
            }
            case "repeat" -> {
                rejectUnknown(object, Set.of("type", "id", "axis", "direction", "min", "max", "body"), path);
                yield new PatternNode.Repeat(string(required(object, "id", path + ".id")),
                        direction(required(object, "axis", path + ".axis"), path + ".axis"),
                        PatternRepeatDirection
                                .valueOf(string(required(object, "direction", path + ".direction"), path + ".direction")
                                        .toUpperCase()),
                        integer(required(object, "min", path + ".min"), path + ".min"),
                        integer(required(object, "max", path + ".max"), path + ".max"),
                        parseNode(required(object, "body", path + ".body"), path + ".body"));
            }
            case "choice" -> {
                rejectUnknown(object, Set.of("type", "id", "alternatives"), path);
                JsonArray values = array(required(object, "alternatives", path + ".alternatives"),
                        path + ".alternatives");
                var alternatives = new ObjectArrayList<PatternNode.Choice.Alternative>();
                for (int index = 0; index < values.size(); index++) {
                    String alternativePath = path + ".alternatives[" + index + "]";
                    JsonObject alternative = object(values.get(index), alternativePath);
                    rejectUnknown(alternative, Set.of("id", "node"), alternativePath);
                    alternatives.add(new PatternNode.Choice.Alternative(
                            string(required(alternative, "id", alternativePath + ".id")),
                            parseNode(required(alternative, "node", alternativePath + ".node"),
                                    alternativePath + ".node")));
                }
                yield new PatternNode.Choice(string(required(object, "id", path + ".id")), alternatives);
            }
            case "fragment" -> {
                rejectUnknown(object, Set.of("type", "id", "bindings"), path);
                var bindings = new Object2ObjectLinkedOpenHashMap<String, PatternBinding>();
                if (object.has("bindings")) {
                    JsonObject values = object(object.get("bindings"), path + ".bindings");
                    values.entrySet().forEach(entry -> bindings.put(entry.getKey(),
                            parseBinding(entry.getValue(), path + ".bindings." + entry.getKey())));
                }
                yield new PatternNode.Fragment(string(required(object, "id", path + ".id")), bindings);
            }
            default -> throw error("Unknown pattern node type '" + type + "' at " + path);
        };
    }

    private static PatternBinding parseBinding(JsonElement element, String path) {
        if (element instanceof JsonPrimitive primitive && primitive.isString())
            return new PatternBinding.Token(primitive.getAsString());
        JsonObject object = object(element, path);
        rejectUnknown(object, Set.of("type", "value"), path);
        String type = string(required(object, "type", path + ".type"));
        JsonElement value = required(object, "value", path + ".value");
        return switch (type) {
            case "token" -> new PatternBinding.Token(string(value, path + ".value"));
            case "predicate" -> new PatternBinding.Predicate(string(value, path + ".value"));
            case "integer" -> new PatternBinding.IntegerValue(integer(value, path + ".value"));
            case "direction" -> new PatternBinding.Direction(direction(value, path + ".value"));
            case "fragment" -> new PatternBinding.Fragment(string(value, path + ".value"));
            default -> throw error("Unknown binding type '" + type + "' at " + path);
        };
    }

    private static List<PatternConstraint> parseConstraints(JsonElement element) {
        if (element == null) return List.of();
        JsonArray values = array(element, "$.constraints");
        var result = new ObjectArrayList<PatternConstraint>();
        for (int index = 0; index < values.size(); index++) {
            String path = "$.constraints[" + index + "]";
            JsonObject object = object(values.get(index), path);
            rejectUnknown(object, Set.of("type", "fact", "scope", "min", "max", "message"), path);
            if (!"count".equals(string(required(object, "type", path + ".type"))))
                throw error("Unknown constraint type at " + path);
            result.add(new PatternConstraint.Count(string(required(object, "fact", path + ".fact")),
                    parseScope(required(object, "scope", path + ".scope"), path + ".scope"),
                    integer(required(object, "min", path + ".min"), path + ".min"),
                    integer(required(object, "max", path + ".max"), path + ".max"),
                    object.has("message") ? string(object.get("message"), path + ".message") : ""));
        }
        return result;
    }

    private static PatternConstraint.Scope parseScope(JsonElement element, String path) {
        String scope = string(element, path);
        if (scope.equals("all")) return new PatternConstraint.Scope.All();
        if (scope.startsWith("fragment:")) return new PatternConstraint.Scope.Fragment(scope.substring(9));
        if (scope.startsWith("node:")) return new PatternConstraint.Scope.Node(scope.substring(5));
        if (scope.startsWith("repeat:")) return new PatternConstraint.Scope.Repeat(scope.substring(7));
        throw error("Unknown constraint scope '" + scope + "' at " + path);
    }

    private static List<List<String>> parseLayers(JsonElement element, String path) {
        JsonArray layers = array(element, path);
        var result = new ObjectArrayList<List<String>>(layers.size());
        for (int index = 0; index < layers.size(); index++) {
            JsonArray rows = array(layers.get(index), path + "[" + index + "]");
            var layer = new ObjectArrayList<String>(rows.size());
            rows.forEach(row -> layer.add(string(row, path + " row")));
            result.add(layer);
        }
        return result;
    }

    private static PatternOrigin.Offset parseOffset(JsonElement element, String path) {
        if (element == null) return new PatternOrigin.Offset(0, 0, 0);
        JsonArray values = array(element, path);
        if (values.size() != 3) throw error("Offset must contain exactly three integers at " + path);
        return new PatternOrigin.Offset(integer(values.get(0), path + "[0]"), integer(values.get(1), path + "[1]"),
                integer(values.get(2), path + "[2]"));
    }

    private static PatternDirection direction(JsonElement element, String path) {
        try {
            return PatternDirection.valueOf(string(element, path).toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw error("Unknown direction at " + path, exception);
        }
    }

    private static JsonElement required(JsonObject object, String name, String path) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) throw error("Missing required field at " + path);
        return value;
    }

    private static JsonObject object(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) throw error("Expected object at " + path);
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, String path) {
        if (element == null || !element.isJsonArray()) throw error("Expected array at " + path);
        return element.getAsJsonArray();
    }

    private static String string(JsonElement element, String path) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw error("Expected string at " + path);
        return element.getAsString();
    }

    private static String string(JsonElement element) {
        return string(element, "value");
    }

    private static int integer(JsonElement element, String path) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
            throw error("Expected integer at " + path);
        return element.getAsInt();
    }

    private static boolean booleanValue(JsonElement element, boolean fallback, String path) {
        if (element == null) return fallback;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean())
            throw error("Expected boolean at " + path);
        return element.getAsBoolean();
    }

    private static void rejectUnknown(JsonObject object, Set<String> allowed, String path) {
        object.keySet().stream().filter(key -> !allowed.contains(key)).findFirst()
                .ifPresent(key -> {
                    throw error("Unknown field '" + key + "' at " + path);
                });
    }

    private static Object toValue(JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsNumber();
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            var values = new ObjectArrayList<Object>();
            element.getAsJsonArray().forEach(value -> values.add(toValue(value)));
            return values;
        }
        var values = new Object2ObjectLinkedOpenHashMap<String, Object>();
        element.getAsJsonObject().entrySet().forEach(entry -> values.put(entry.getKey(), toValue(entry.getValue())));
        return values;
    }

    private static JsonObject encodeAxes(PatternAxes axes) {
        JsonObject object = new JsonObject();
        object.addProperty("depth", axes.depth().name().toLowerCase());
        object.addProperty("height", axes.height().name().toLowerCase());
        object.addProperty("width", axes.width().name().toLowerCase());
        return object;
    }

    private static JsonObject encodeOrientation(PatternOrientation orientation) {
        JsonObject object = new JsonObject();
        object.addProperty("allowMirror", orientation.allowMirror());
        object.addProperty("allowExtendedFacing", orientation.allowExtendedFacing());
        return object;
    }

    private static JsonObject encodeOrigin(PatternOrigin origin) {
        JsonObject object = new JsonObject();
        object.addProperty("token", origin.token());
        JsonArray offset = new JsonArray();
        offset.add(origin.offset().depth());
        offset.add(origin.offset().height());
        offset.add(origin.offset().width());
        object.add("offset", offset);
        return object;
    }

    private static JsonObject encodeParameters(Map<String, PatternParameter> parameters) {
        JsonObject result = new JsonObject();
        parameters.forEach((name, parameter) -> result.addProperty(name, parameterName(parameter)));
        return result;
    }

    private static String parameterName(PatternParameter parameter) {
        if (parameter instanceof PatternParameter.Token) return "token";
        if (parameter instanceof PatternParameter.Predicate) return "predicate";
        if (parameter instanceof PatternParameter.Direction) return "direction";
        if (parameter instanceof PatternParameter.Fragment) return "fragment";
        return "integer";
    }

    private static JsonObject encodeFragment(PatternFragment fragment) {
        JsonObject object = new JsonObject();
        object.add("parameters", encodeParameters(fragment.parameters()));
        JsonObject anchors = new JsonObject();
        fragment.anchors().forEach((id, offset) -> anchors.add(id, encodeOffset(offset)));
        object.add("anchors", anchors);
        JsonObject ports = new JsonObject();
        fragment.ports().forEach((id, port) -> {
            JsonObject value = new JsonObject();
            value.add("offset", encodeOffset(port.offset()));
            value.addProperty("direction", port.direction().name().toLowerCase());
            ports.add(id, value);
        });
        object.add("ports", ports);
        object.add("body", encodeNode(fragment.body()));
        return object;
    }

    private static JsonArray encodeOffset(PatternOrigin.Offset offset) {
        JsonArray value = new JsonArray();
        value.add(offset.depth());
        value.add(offset.height());
        value.add(offset.width());
        return value;
    }

    private static JsonObject encodePredicate(PatternPredicateDefinition predicate) {
        JsonObject object = new JsonObject();
        object.addProperty("type", predicate.type());
        if (!predicate.name().isEmpty()) object.addProperty("name", predicate.name());
        predicate.properties().forEach((name, value) -> object.add(name, new GsonBuilder().create().toJsonTree(value)));
        if (!predicate.facts().isEmpty()) {
            JsonArray facts = new JsonArray();
            predicate.facts().forEach(facts::add);
            object.add("facts", facts);
        }
        return object;
    }

    private static JsonObject encodeNode(PatternNode node) {
        JsonObject object = new JsonObject();
        if (node instanceof PatternNode.Fixed fixed) {
            object.addProperty("type", "fixed");
            JsonArray layers = new JsonArray();
            fixed.layers().forEach(layer -> {
                JsonArray rows = new JsonArray();
                layer.forEach(rows::add);
                layers.add(rows);
            });
            object.add("layers", layers);
            return object;
        }
        if (node instanceof PatternNode.Sequence sequence) {
            object.addProperty("type", "sequence");
            object.addProperty("axis", sequence.axis().name().toLowerCase());
            JsonArray children = new JsonArray();
            sequence.children().forEach(child -> children.add(encodeNode(child)));
            object.add("children", children);
            return object;
        }
        if (node instanceof PatternNode.Repeat repeat) {
            object.addProperty("type", "repeat");
            object.addProperty("id", repeat.id());
            object.addProperty("axis", repeat.axis().name().toLowerCase());
            object.addProperty("direction", repeat.direction().name().toLowerCase());
            object.addProperty("min", repeat.minimum());
            object.addProperty("max", repeat.maximum());
            object.add("body", encodeNode(repeat.body()));
            return object;
        }
        if (node instanceof PatternNode.Choice choice) {
            object.addProperty("type", "choice");
            object.addProperty("id", choice.id());
            JsonArray alternatives = new JsonArray();
            choice.alternatives().forEach(alternative -> {
                JsonObject value = new JsonObject();
                value.addProperty("id", alternative.id());
                value.add("node", encodeNode(alternative.node()));
                alternatives.add(value);
            });
            object.add("alternatives", alternatives);
            return object;
        }
        PatternNode.Fragment fragment = (PatternNode.Fragment) node;
        object.addProperty("type", "fragment");
        object.addProperty("id", fragment.id());
        return object;
    }

    private static JsonObject encodeConstraint(PatternConstraint constraint) {
        PatternConstraint.Count count = (PatternConstraint.Count) constraint;
        JsonObject object = new JsonObject();
        object.addProperty("type", "count");
        object.addProperty("fact", count.fact());
        object.addProperty("scope", scopeName(count.scope()));
        object.addProperty("min", count.minimum());
        object.addProperty("max", count.maximum());
        if (!count.message().isEmpty()) object.addProperty("message", count.message());
        return object;
    }

    private static String scopeName(PatternConstraint.Scope scope) {
        if (scope instanceof PatternConstraint.Scope.All) return "all";
        if (scope instanceof PatternConstraint.Scope.Fragment value) return "fragment:" + value.id();
        if (scope instanceof PatternConstraint.Scope.Node value) return "node:" + value.id();
        return "repeat:" + ((PatternConstraint.Scope.Repeat) scope).id();
    }

    private static IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException error(String message, Throwable cause) {
        return new IllegalArgumentException(message, cause);
    }
}
