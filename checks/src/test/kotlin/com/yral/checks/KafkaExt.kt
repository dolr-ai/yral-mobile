package com.yral.checks

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.Base64
import java.util.UUID

private val kafkaBootstrap: String = System.getenv("KAFKA_BOOTSTRAP") ?: "kafka.yral.com:9092"

val kafkaPassword: String by lazy {
    System.getenv("KAFKA_PASSWORD") ?: fetchPasswordFromKubectl()
}

fun countSnowplowEvents(since: Long, platformMarker: String): Int {
    val consumer = KafkaConsumer<ByteArray, ByteArray>(kafkaConsumerProps())
    val partitions = consumer
        .partitionsFor("snowplow-raw")
        .map { TopicPartition("snowplow-raw", it.partition()) }
    consumer.assign(partitions)

    // offsetsForTimes seeks to exact test-start ms — no historical scan needed
    consumer.offsetsForTimes(partitions.associateWith { since }).forEach { (tp, oTs) ->
        if (oTs != null) consumer.seek(tp, oTs.offset()) else consumer.seekToEnd(listOf(tp))
    }

    var found = 0
    val deadline = System.currentTimeMillis() + 30_000
    while (System.currentTimeMillis() < deadline) {
        // snowplow-raw is Thrift-encoded binary; "yral-mobile" and "Android"/"iOS"
        // are embedded as readable UTF-8 substrings within the payload
        for (record in consumer.poll(Duration.ofSeconds(1))) {
            val payload = String(record.value(), Charsets.UTF_8)
            if (payload.contains("yral-mobile") && payload.contains(platformMarker)) found++
        }
        if (found > 0) break
    }
    consumer.close()
    return found
}

private fun kafkaConsumerProps() = mapOf(
    "bootstrap.servers" to kafkaBootstrap,
    "security.protocol" to "SASL_SSL",
    "sasl.mechanism" to "SCRAM-SHA-512",
    "sasl.jaas.config" to """org.apache.kafka.common.security.scram.ScramLoginModule required username="ci-e2e-reader" password="$kafkaPassword";""",
    "group.id" to "ci-e2e-assert-${UUID.randomUUID()}",
    "key.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "value.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "enable.auto.commit" to "false",
)

private fun fetchPasswordFromKubectl(): String {
    // Mirror checks.sh _require_kubeconfig(): if KUBECONFIG is not set, fall back to
    // the ci-e2e-reader.kubeconfig file in the repo root (for local dev).
    val kubeconfig = System.getenv("KUBECONFIG")?.takeIf { it.isNotBlank() }
        ?: repoRoot.resolve("ci-e2e-reader.kubeconfig")
            .takeIf { it.exists() }
            ?.absolutePath
        ?: error(
            "KUBECONFIG is not set and ci-e2e-reader.kubeconfig not found at $repoRoot. " +
                "Get the kubeconfig from a team member and place it there.",
        )

    val proc = ProcessBuilder(
        "kubectl", "get", "secret", "ci-e2e-reader",
        "-n", "kafka", "-o", "jsonpath={.data.password}",
    ).also { it.environment()["KUBECONFIG"] = kubeconfig }
     .start()
    val b64 = proc.inputStream.readBytes().toString(Charsets.UTF_8).trim()
    return Base64.getDecoder().decode(b64).toString(Charsets.UTF_8)
}
