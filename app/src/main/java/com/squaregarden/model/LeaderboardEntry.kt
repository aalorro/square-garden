package com.squaregarden.model

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val emoji: String = "",
    val score: Int = 0,
    val level: Int = 0,
    val ts: Long = 0L,
    val rank: Int = 0
)
