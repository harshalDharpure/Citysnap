package com.prod.singles_date.util

import com.prod.singles_date.model.AppCity
import java.util.Calendar

object DailyPrompts {
    private val bangalore = listOf(
        "What is one thing nobody admits about Bangalore?",
        "What's frustrating you today?",
        "What's something everyone pretends to like?",
        "What happened at work today?",
        "Bangalore traffic feels worse every week.",
        "Rent is becoming impossible.",
        "What's one thing about Bangalore traffic you'd never say in office?",
        "WFH flexible — what does that really mean here?",
        "One honest thought about rent in your area.",
        "What would you tell someone moving to Bangalore tomorrow?",
    )
    private val pune = listOf(
        "Baner to Hinjewadi — how do you really feel about the commute?",
        "One thought about Pune monsoon life nobody talks about.",
        "What's the most Pune thing that happened to you this week?",
        "PG life vs flat life — what's on your mind?",
        "One honest thought about your office in Pune.",
    )
    private val hyderabad = listOf(
        "Hitec City salary, Gachibowli rent — what's the gap for you?",
        "One thought about Hyderabad office culture.",
        "What's something only Hyderabad people understand?",
        "Traffic on the ORR — what's your honest take?",
        "One thing you'd change about life in Hyderabad.",
    )
    private val chennai = listOf(
        "What's the real story with Chennai rent this year?",
        "One thing about Chennai commute nobody talks about.",
    )
    private val mumbai = listOf(
        "Local train vs cab — what's your honest take?",
        "One thing about Mumbai rent nobody admits.",
    )
    private val delhi = listOf(
        "Metro vs cab in Delhi — what's on your mind?",
        "One honest thought about Delhi office culture.",
    )

    private val suggestions = listOf(
        "What's one thing nobody says about your city?",
        "What's frustrating you today?",
        "What's something everyone pretends to like?",
        "What happened at work today?",
    )

    fun promptFor(cityId: String, dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)): String {
        val prompts = promptsForCity(cityId)
        return prompts[dayOfYear % prompts.size]
    }

    fun suggestionsFor(cityId: String): List<String> {
        val cityPrompts = promptsForCity(cityId)
        return (suggestions + cityPrompts).distinct()
    }

    private fun promptsForCity(cityId: String): List<String> = when (cityId) {
        AppCity.BANGALORE -> bangalore
        AppCity.PUNE -> pune
        AppCity.HYDERABAD -> hyderabad
        AppCity.CHENNAI -> chennai
        AppCity.MUMBAI -> mumbai
        AppCity.DELHI -> delhi
        else -> bangalore
    }
}
