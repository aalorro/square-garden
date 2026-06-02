# Square Garden ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.squaregarden.data.LeaderboardEntry { *; }
