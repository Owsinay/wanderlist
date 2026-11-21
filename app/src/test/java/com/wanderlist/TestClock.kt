package com.wanderlist

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Stały zegar dla testów: 2026-06-15 12:00 UTC. */
val testClock: Clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC)
