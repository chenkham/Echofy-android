package com.Chenkham.Echofy.ai

import java.util.Locale

/**
 * Local offline intent classifier and string matching for voice commands.
 */
object VoiceIntentClassifier {
    /**
     * Compute normalized Levenshtein similarity between two strings.
     */
    fun fuzzyMatch(
        s1: String,
        s2: String,
    ): Float {
        val a = s1.lowercase(Locale.getDefault()).trim()
        val b = s2.lowercase(Locale.getDefault()).trim()
        if (a == b) return 1.0f
        if (a.contains(b) || b.contains(a)) return 0.85f

        val costs = IntArray(b.length + 1)
        for (j in 0..b.length) costs[j] = j
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), if (a[i - 1] == b[j - 1]) nw else nw + 1)
                nw = costs[j]
                costs[j] = cj
            }
        }
        val maxLen = Math.max(a.length, b.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (costs[b.length].toFloat() / maxLen.toFloat())
    }

    /**
     * Fast local fallback classifier when cloud inference is unavailable or offline.
     */
    fun fallbackLocalIntentClassifier(userInput: String): AgentAction {
        val text = userInput.lowercase(Locale.getDefault())
        return when {
            text.contains("pause") || text.contains("stop music") ->
                AgentAction("Paused playback!", "PAUSE")
            text.contains("resume") || text.contains("unpause") ->
                AgentAction("Resuming playback!", "RESUME")
            text.contains("next") || text.contains("skip") ->
                AgentAction("Skipping to next track!", "NEXT")
            text.contains("previous") || text.contains("go back") ->
                AgentAction("Going back!", "PREVIOUS")
            text.contains("hey") || text.contains("hi") || text.contains("hello") ->
                AgentAction("Hey there! How's your day going? Want to listen to some music or chat?", "CHAT")
            text.contains("suggest") || text.contains("recommend") ->
                AgentAction("I can suggest songs based on your mood! Tell me what genre or artist you like.", "CHAT")
            else ->
                AgentAction("I'm here! Tell me what song or artist you'd like to play, or ask any question.", "CHAT")
        }
    }
}
