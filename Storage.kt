package com.premiabrygady.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PersonInput(val name: String, val hoursText: String)

data class CalculatorState(
    val name: String,
    val amountText: String,
    val persons: List<PersonInput>
)

data class HistoryPerson(val name: String, val hours: Long, val amountCents: Long)

data class HistoryEntry(
    val id: String,
    val name: String,
    val dateIso: String,
    val amountCents: Long,
    val persons: List<HistoryPerson>
)

/**
 * Zapis danych w SharedPreferences jako JSON (org.json).
 * Bez Room, bez kapt/ksp, bez Hilt i bez dodatkowych bibliotek.
 */
class Storage(context: Context) {

    private val prefs = context.getSharedPreferences("premia_brygady", Context.MODE_PRIVATE)

    fun saveCalculator(state: CalculatorState) {
        val persons = JSONArray()
        for (p in state.persons) {
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("hoursText", p.hoursText)
            persons.put(obj)
        }
        val root = JSONObject()
        root.put("name", state.name)
        root.put("amountText", state.amountText)
        root.put("persons", persons)
        prefs.edit().putString(KEY_CALCULATOR, root.toString()).apply()
    }

    fun loadCalculator(): CalculatorState? {
        val raw = prefs.getString(KEY_CALCULATOR, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("persons") ?: JSONArray()
            val persons = mutableListOf<PersonInput>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                persons.add(
                    PersonInput(
                        obj.optString("name", ""),
                        obj.optString("hoursText", "")
                    )
                )
            }
            if (persons.isEmpty()) {
                persons.add(PersonInput("", ""))
                persons.add(PersonInput("", ""))
            }
            CalculatorState(
                root.optString("name", ""),
                root.optString("amountText", ""),
                persons
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveHistory(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("name", entry.name)
            obj.put("dateIso", entry.dateIso)
            obj.put("amountCents", entry.amountCents)
            val persons = JSONArray()
            for (p in entry.persons) {
                val po = JSONObject()
                po.put("name", p.name)
                po.put("hours", p.hours)
                po.put("amountCents", p.amountCents)
                persons.put(po)
            }
            obj.put("persons", persons)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<HistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val personsArr = obj.optJSONArray("persons") ?: JSONArray()
                val persons = mutableListOf<HistoryPerson>()
                for (j in 0 until personsArr.length()) {
                    val po = personsArr.getJSONObject(j)
                    persons.add(
                        HistoryPerson(
                            po.optString("name", ""),
                            po.optLong("hours", 0L),
                            po.optLong("amountCents", 0L)
                        )
                    )
                }
                result.add(
                    HistoryEntry(
                        obj.optString("id", ""),
                        obj.optString("name", ""),
                        obj.optString("dateIso", ""),
                        obj.optLong("amountCents", 0L),
                        persons
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_CALCULATOR = "calculator_state"
        private const val KEY_HISTORY = "history"
    }
}
