package com.squaregarden.model

data class UserProfile(
    val username: String = "",
    val avatarId: Int = 0,
    val yearOfBirth: Int = 2000,
    val gender: String = "prefer_not_to_say",
    val themeId: String = "light",
    val difficulty: String = "medium",
    val playerLevel: Int = 0
) {
    val isSetUp: Boolean get() = username.isNotBlank()
}

enum class Gender(val id: String, val label: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say");

    companion object {
        fun fromId(id: String): Gender = entries.firstOrNull { it.id == id } ?: PREFER_NOT_TO_SAY
    }
}

enum class Difficulty(val id: String, val label: String, val moveMultiplier: Float, val starMultiplier: Int, val startingLevel: Int) {
    EASY("easy", "Casual", 1.5f, 1, 1),
    MEDIUM("medium", "Standard", 1.0f, 2, 10),
    HARD("hard", "Pro", 0.7f, 3, 19);

    val startingWorld: Int get() = ((startingLevel - 1) / 9) + 1

    companion object {
        fun fromId(id: String): Difficulty = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}
