package com.squaregarden

/**
 * Development-only feature toggles.
 *
 * IMPORTANT: Every flag in this file MUST be set to `false` before any commit.
 * These toggles expose hidden surfaces (e.g. the Challenge Lab at world 0) that
 * are not intended to ship to players. Enable locally only while testing, then
 * flip back to `false` before staging changes.
 */
object DevFlags {
    /**
     * When true, exposes a button on World 10's level select that navigates to
     * the Challenge Lab (world 0). The Challenge Lab is otherwise unreachable
     * through the UI. Keep this `false` on every commit.
     */
    const val CHALLENGE_LAB_ENABLED: Boolean = false
}
