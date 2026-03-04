package com.calorietracker.ui.theme

import androidx.compose.ui.graphics.Color

// Dark background palette (matching Tailwind neutral-900 through neutral-700)
val Neutral900 = Color(0xFF171717)
val Neutral800 = Color(0xFF262626)
val Neutral700 = Color(0xFF404040)
val Neutral600 = Color(0xFF525252)
val Neutral500 = Color(0xFF737373)
val Neutral300 = Color(0xFFD4D4D4)
val Neutral400 = Color(0xFFA3A3A3)

// Primary accent: rose → orange gradient
val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)
val Rose300 = Color(0xFFFDA4AF)
val Orange500 = Color(0xFFF97316)

// Macro colors
val Blue400 = Color(0xFF60A5FA)   // protein
val Amber400 = Color(0xFFFBBF24)  // carbs
// fat uses Rose400 above

// Status colors
val Emerald400 = Color(0xFF34D399)  // under goal / success
val Emerald500 = Color(0xFF10B981)
val Red400 = Color(0xFFF87171)      // over goal
val Red500 = Color(0xFFEF4444)

// Feature accent colors
val Purple400 = Color(0xFFA78BFA)   // weight
val Indigo400 = Color(0xFF818CF8)   // meal planner

// Meal type colors
val MealBreakfast = Amber400
val MealLunch = Orange500
val MealDinner = Indigo400
val MealSnacks = Rose400
