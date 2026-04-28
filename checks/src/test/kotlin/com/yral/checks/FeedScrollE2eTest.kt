package com.yral.checks

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.time.Duration
import java.util.Base64
import java.util.UUID

@Order(2)
class FeedScrollE2eTest {
    @BeforeEach
    fun requireLiveDevice() {
        assertTrue(isDeviceAvailable()) {
            "E2E requires a live $platform device but none was detected"
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "E2E_PLATFORM", matches = "android|ios")
    fun `feed scroll delivers events to snowplow-raw`() {
        val testStartMs = System.currentTimeMillis()

        runMaestroFlow()

        println("Waiting 15s for collector → snowplow-raw delivery...")
        Thread.sleep(15_000)

        val count = countSnowplowEvents(testStartMs)
        assertTrue(count > 0) {
            "No snowplow-raw events for platform=$platform since $testStartMs"
        }
        println("PASS: $count event(s) for platform=$platform")
    }

    private fun isDeviceAvailable(): Boolean =
        runCatching {
            when (platform) {
                "android" -> captureOutput("adb", "devices").lines().count { it.contains("\tdevice") } > 0
                "ios" -> captureOutput("xcrun", "simctl", "list", "devices").contains("(Booted)")
                else -> false
            }
        }.getOrDefault(false)

    private fun runMaestroFlow() {
        val exit = ProcessBuilder(
            "maestro", "test",
            "-e", "APP_ID=$appId",
            "maestro/flows/feed-scroll.yaml",
        ).directory(repoRoot).inheritIO().start().waitFor()
        assertEquals(0, exit) { "Maestro flow failed with exit code $exit" }
    }

    private fun countSnowplowEvents(since: Long): Int {
        val platformMarker = if (platform == "android") "Android" else "iOS"
        val consumer = KafkaConsumer<ByteArray, ByteArray>(consumerProps())
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

    private fun consumerProps() = mapOf(
        "bootstrap.servers" to kafkaBootstrap,
        "security.protocol" to "SASL_SSL",
        "sasl.mechanism" to "SCRAM-SHA-512",
        "sasl.jaas.config" to jaasConfig(),
        "group.id" to "ci-e2e-assert-${UUID.randomUUID()}",
        "key.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
        "enable.auto.commit" to "false",
    )

    private fun jaasConfig() =
        """org.apache.kafka.common.security.scram.ScramLoginModule required username="ci-e2e-reader" password="$kafkaPassword";"""

    companion object {
        val platform: String = System.getenv("E2E_PLATFORM") ?: "android"
        val appId: String = System.getenv("E2E_APP_ID") ?: "com.yral.android"
        private val kafkaBootstrap: String = System.getenv("KAFKA_BOOTSTRAP") ?: "kafka.yral.com:9092"

        // Reads KAFKA_PASSWORD env var (CI), else fetches live from kubectl (local dev)
        val kafkaPassword: String by lazy {
            System.getenv("KAFKA_PASSWORD") ?: fetchPasswordFromKubectl()
        }

        @BeforeAll
        @JvmStatic
        fun installApp() {
            val platformEnv = System.getenv("E2E_PLATFORM") ?: return // skip when not running e2e
            when (platformEnv) {
                "android" -> execOrFail(
                    "adb", "install", "-r",
                    "androidApp/build/outputs/apk/staging/debug/androidApp-staging-debug.apk",
                )
                "ios" -> {
                    val udid = Checks.firstIphoneSimulatorUdid()
                    exec("xcrun", "simctl", "boot", udid) // ignore error if already booted
                    execOrFail("xcrun", "simctl", "bootstatus", udid, "-b")
                    execOrFail(
                        "xcrun", "simctl", "install", "booted",
                        File(repoRoot, "build/DerivedData/Build/Products/Debug-iphonesimulator/iosApp.app").absolutePath,
                    )
                }
            }
        }

        private fun fetchPasswordFromKubectl(): String {
            val proc = ProcessBuilder(
                "kubectl", "get", "secret", "ci-e2e-reader",
                "-n", "kafka", "-o", "jsonpath={.data.password}",
            ).start()
            val b64 = proc.inputStream.readBytes().toString(Charsets.UTF_8).trim()
            return Base64.getDecoder().decode(b64).toString(Charsets.UTF_8)
        }
    }
}
