package kz.youni4st.app.data

import org.json.JSONObject

object CloudMerge {
    /** Incoming server values plus only this device's explicitly pending item edits. */
    fun items(remote:JSONObject?,local:Set<String>,pending:Set<String>,section:String):Set<String> {
        val merged=CloudItems.values(remote).toMutableSet()
        pending.filter {it.startsWith("$section|")}.forEach {key ->
            val id=key.substringAfter('|')
            if(id in local)merged.add(id) else merged.remove(id)
        }
        return merged
    }
}
