# SerializableMap for Kotlin Multiplatform

A serializable wrapper for `Map<String, Any?>` that works with Kotlin's kotlinx.serialization. This provides a way to handle maps with heterogeneous value types, which is not directly supported by the standard serialization library.

## Features

- Serializes and deserializes `Map<String, Any?>` objects
- Supports primitive values (String, Number, Boolean)
- Handles nested maps and lists
- Supports null values
- Attempts to serialize any other objects using the standard kotlinx.serialization
- Provides helpful error messages when serialization fails

## Usage

### Basic Usage with SerializableMap Class

The simplest way to use the serializer is with the `SerializableMap` class, which implements the `Map` interface:

```kotlin
import com.yral.shared.core.utils.SerializableMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Create a SerializableMap with mixed value types
val map = SerializableMap(
    "stringValue" to "Hello, World!",
    "intValue" to 42,
    "boolValue" to true,
    "listValue" to listOf(1, 2, 3),
    "nestedMap" to mapOf("key" to "value")
)

// Serialize to JSON
val json = Json.encodeToString(map)
println(json)

// Deserialize from JSON
val deserializedMap = Json.decodeFromString<SerializableMap>(json)
```

### Converting Regular Maps to SerializableMap

You can convert a regular `Map<String, Any?>` to a `SerializableMap` using the extension function:

```kotlin
import com.yral.shared.core.utils.toSerializableMap

val regularMap = mapOf(
    "key1" to "value1",
    "key2" to 123,
    "key3" to true
)

// Convert to SerializableMap
val serializableMap = regularMap.toSerializableMap()

// Serialize to JSON
val json = Json.encodeToString(serializableMap)
```

### Converting Back to Regular Map

You can convert a `SerializableMap` back to a regular `Map<String, Any?>`:

```kotlin
val serializableMap = SerializableMap("name" to "John", "age" to 30)

// Convert to regular Map
val regularMap: Map<String, Any?> = serializableMap.toMap()
```

### Direct Serializer Usage

You can also use the `MapSerializer` directly if needed:

```kotlin
import com.yral.shared.core.utils.MapSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val map = mapOf(
    "key1" to "value1",
    "key2" to 100
)

// Custom JSON configuration
val customJson = Json { 
    prettyPrint = true 
    isLenient = true
}

// Serialize using the serializer directly
val json = customJson.encodeToString(MapSerializer(), map)

// Deserialize using the serializer directly
val deserializedMap: Map<String, Any?> = customJson.decodeFromString(MapSerializer(), json)
```

### Working with Deserialized Values

When accessing values from a deserialized map, you'll need to cast them to the appropriate types:

```kotlin
val map = Json.decodeFromString<SerializableMap>(jsonString)

// Cast to appropriate types when accessing values
val stringValue: String = map["stringValue"] as String
val intValue: Long = map["intValue"] as Long  // Note: Numbers are deserialized as Long or Double
val boolValue: Boolean = map["boolValue"] as Boolean
val listValue: List<*> = map["listValue"] as List<*>
val nestedMap: Map<String, Any?> = map["nestedMap"] as Map<String, Any?>
```

## Important Notes

1. When deserializing, numeric values will be represented as either `Long` (for integers) or `Double` (for floating-point numbers).
2. Nested maps will be deserialized as `Map<String, Any?>`.
3. Lists will be deserialized as `List<*>` with mixed element types if the original list had mixed types.
4. For serializable objects, the serializer will attempt to use kotlinx.serialization's standard mechanism.
5. The `SerializableMap` class implements the `Map` interface, so it can be used anywhere a regular `Map` is expected.

## Implementation Details

The `MapSerializer` implements the `KSerializer<Map<String, Any?>>` interface and uses JSON objects for serialization under the hood. It handles complex types by recursively serializing them to JSON elements.

See `MapSerializerExample.kt` for more detailed examples of usage. 