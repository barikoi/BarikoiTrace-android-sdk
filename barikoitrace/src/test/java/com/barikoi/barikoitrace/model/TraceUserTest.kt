package com.barikoi.barikoitrace.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TraceUserTest {

    @Test
    fun defaultValues_areAppliedWhenOmitted() {
        val user = TraceUser(userId = "u-123")

        assertThat(user.userId).isEqualTo("u-123")
        assertThat(user.phone).isNull()
        assertThat(user.companyId).isNull()
        assertThat(user.group).isNull()
        assertThat(user.lastLat).isEqualTo(0.0)
        assertThat(user.lastLon).isEqualTo(0.0)
        assertThat(user.updatedAt).isGreaterThan(0L)
    }

    @Test
    fun fullConstructor_populatesAllFields() {
        val timestamp = 1700000000000L
        val user = TraceUser(
            userId = "u-456",
            phone = "01700032522",
            companyId = "comp-1",
            group = "group-2",
            lastLat = 23.8103,
            lastLon = 90.4125,
            updatedAt = timestamp
        )

        assertThat(user.userId).isEqualTo("u-456")
        assertThat(user.phone).isEqualTo("01700032522")
        assertThat(user.companyId).isEqualTo("comp-1")
        assertThat(user.group).isEqualTo("group-2")
        assertThat(user.lastLat).isEqualTo(23.8103)
        assertThat(user.lastLon).isEqualTo(90.4125)
        assertThat(user.updatedAt).isEqualTo(timestamp)
    }

    @Test
    fun equalityAndHashCode_matchOnEqualInstances() {
        val user1 = TraceUser(
            userId = "u-1",
            phone = "01700000000",
            companyId = "c-1",
            group = "g-1",
            lastLat = 10.0,
            lastLon = 20.0,
            updatedAt = 5000L
        )
        val user2 = TraceUser(
            userId = "u-1",
            phone = "01700000000",
            companyId = "c-1",
            group = "g-1",
            lastLat = 10.0,
            lastLon = 20.0,
            updatedAt = 5000L
        )
        val user3 = user1.copy(phone = "01800000000")

        assertThat(user1).isEqualTo(user2)
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode())
        assertThat(user1).isNotEqualTo(user3)
    }

    @Test
    fun copy_allowsSelectiveUpdates() {
        val original = TraceUser(
            userId = "u-orig",
            phone = "01711111111",
            companyId = "c-orig"
        )
        val updated = original.copy(companyId = "c-new", group = "g-new")

        assertThat(updated.userId).isEqualTo("u-orig")
        assertThat(updated.phone).isEqualTo("01711111111")
        assertThat(updated.companyId).isEqualTo("c-new")
        assertThat(updated.group).isEqualTo("g-new")
    }

    @Test
    fun name_populatesAndCopiesCorrectly() {
        val user = TraceUser(userId = "u-789", name = "Alice")
        assertThat(user.userId).isEqualTo("u-789")
        assertThat(user.name).isEqualTo("Alice")

        val updated = user.copy(name = "Bob")
        assertThat(updated.name).isEqualTo("Bob")
        assertThat(updated.userId).isEqualTo("u-789")
    }
}
