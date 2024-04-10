package mealplanner

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.io.File
import mealplanner.*

class MealPlanner(private val connection: Connection) {
    private val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val categories = arrayOf("breakfast", "lunch", "dinner")
    private val ingredientsList: MutableList<String> = mutableListOf()
    private var isPlanSaved: Boolean = false

    // Функция для создания таблиц, если они не существуют
    fun createTableIfNotExists() {
        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='meals'")
            if (!resultSet.next()) {
                statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
                statement.executeUpdate("CREATE TABLE ingredients (ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
                statement.executeUpdate("CREATE TABLE plan (option TEXT, category TEXT, meal_id INTEGER)")
            }
        }
    }

    // Функция для вывода блюд из базы данных
    fun show() {
        var operation: String
        connection.createStatement().use { statement ->
            println("Which category do you want to print (breakfast, lunch, dinner)?")
            do {
                operation = readLine().toString()

                if (operation !in categories) {
                    println("Wrong meal category! Choose from: breakfast, lunch, dinner.")
                    continue // Пропускаем остаток цикла и снова запрашиваем категорию
                }

                // Проверяем существует ли введенная пользователем категория в базе данных
                val categoryExistsQuery = "SELECT COUNT(*) AS count FROM meals WHERE category = ?"
                connection.prepareStatement(categoryExistsQuery).use { categoryExistsPreparedStatement ->
                    categoryExistsPreparedStatement.setString(1, operation)
                    val categoryExistsResultSet = categoryExistsPreparedStatement.executeQuery()

                    if (categoryExistsResultSet.next()) {
                        val count = categoryExistsResultSet.getInt("count")
                        if (count == 0) {
                            println("No meals found.")
                            return
                        }
                    }
                }
            } while (operation !in categories)

            val mealIdQuery = "SELECT meal_id, category, meal FROM meals WHERE category = ?"
            connection.prepareStatement(mealIdQuery).use { mealIdPreparedStatement ->
                mealIdPreparedStatement.setString(1, operation)
                val resultSet = mealIdPreparedStatement.executeQuery()
                val category = resultSet.getString("category")
                println("Category: $category")
                println("")
                while (resultSet.next()) {
                    val mealId = resultSet.getInt("meal_id")
                    val name = resultSet.getString("meal")
                    println("Name: $name")

                    val ingredientQuery = "SELECT ingredient FROM ingredients WHERE meal_id = ?"
                    connection.prepareStatement(ingredientQuery).use { ingredientPreparedStatement ->
                        ingredientPreparedStatement.setInt(1, mealId)
                        val ingredientResultSet = ingredientPreparedStatement.executeQuery()

                        val ingredients = mutableListOf<String>()
                        while (ingredientResultSet.next()) {
                            val ingredient = ingredientResultSet.getString("ingredient")
                            ingredients.add(ingredient)
                        }

                        if (ingredients.isNotEmpty()) {
                            println("Ingredients:\n${ingredients.joinToString("\n")}")
                        }
                        println()
                    }
                }
            }
        }
    }

    private fun plan(connection: Connection) {
        // Weekly plan map to store the plan for each day
        val weeklyPlan = mutableMapOf<String, MutableMap<String, String>>()
        connection.use { conn ->
            // Delete existing plan for the week
            val deletePlanQuery = "DELETE FROM plan"
            val preparedDeletePlanStatement = conn.prepareStatement(deletePlanQuery)
            preparedDeletePlanStatement.executeUpdate()

            daysOfWeek.forEach { day ->
                println(day)
                val planMap = mutableMapOf<String, String>()//_
                categories.forEach { category ->
                    val mealsQuery = "SELECT meal_id, meal FROM meals WHERE category = ? ORDER BY meal"
                    val preparedMealsStatement = conn.prepareStatement(mealsQuery)
                    preparedMealsStatement.setString(1, category)
                    val mealsResult = preparedMealsStatement.executeQuery()

                    val mealsList = mutableListOf<Pair<Int, String>>()
                    while (mealsResult.next()) {
                        val mealName = mealsResult.getString("meal")
                        val mealId = mealsResult.getInt("meal_id")
                        mealsList.add(mealId to mealName)
                        println(mealName)
                    }

                    println("Choose the $category for $day from the list above:")
                    var chosenMeal: String? = null
                    var chosenMealId: Int? = null
                    do {
                        val selection = readLine().toString()
                        val foundMeal = mealsList.find { it.second == selection }
                        if (foundMeal == null) {
                            println("This meal doesn’t exist. Choose a meal from the list above.")
                        } else {
                            chosenMeal = selection
                            chosenMealId = foundMeal.first
                        }
                    } while (chosenMeal == null || chosenMealId == null)

                    planMap[category] = chosenMeal

                    // Save the chosen meal and its meal_id to the plan table
                    val insertPlanQuery = "INSERT INTO plan (option, category, meal_id) VALUES (?, ?, ?)"
                    val preparedInsertPlanStatement = conn.prepareStatement(insertPlanQuery)
                    preparedInsertPlanStatement.setString(1, chosenMeal)
                    preparedInsertPlanStatement.setString(2, category)
                    preparedInsertPlanStatement.setInt(3, chosenMealId)
                    preparedInsertPlanStatement.executeUpdate()

                    // Retrieve ingredients for the chosen meal and add them to the ingredientsList
                    val ingredientsQuery = "SELECT ingredient FROM ingredients WHERE meal_id = ?"
                    val preparedIngredientsStatement = conn.prepareStatement(ingredientsQuery)
                    preparedIngredientsStatement.setInt(1, chosenMealId)
                    val ingredientsResult = preparedIngredientsStatement.executeQuery()
                    while (ingredientsResult.next()) {
                        val ingredient = ingredientsResult.getString("ingredient")
                        ingredientsList.add(ingredient)
                    }
                }
                // Print the plan for the day
                println("Yeah! We planned the meals for $day.")

                // Add the daily plan to the weekly plan
                weeklyPlan[day] = planMap
            }
        }
        // Print the whole plan for the week
        weeklyPlan.forEach { (day, plan) ->
            println(day)
            println("Breakfast: ${plan["breakfast"]}")
            println("Lunch: ${plan["lunch"]}")
            println("Dinner: ${plan["dinner"]}")
            println()
        }
        isPlanSaved = true
    }

    fun checkPlan(connection: Connection) {
        try {
            connection.createStatement().use { statement ->
                val result = statement.executeQuery("SELECT COUNT(*) AS count FROM plan")
                if (result.next()) {
                    val count = result.getInt("count")
                    if (count > 3) {
                        isPlanSaved = true
                        val ingredientsQuery =
                            "SELECT ingredient FROM ingredients WHERE meal_id IN (SELECT meal_id FROM plan)"
                        val ingredientsResult = statement.executeQuery(ingredientsQuery)
                        while (ingredientsResult.next()) {
                            val ingredient = ingredientsResult.getString("ingredient")
                            ingredientsList.add(ingredient)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            //println("Error checking plan: ${e.message}")
        }
    }

    private fun save(ingredientsList: List<String>) {
        println("Input a filename:")
        val filename = readLine().toString()
        val myFile = File(filename)
        val ingredientOccurrences = mutableMapOf<String, Int>()
        //повторяющиеся ингридиенты пересмотреть и подсчитать
        for (ingredient in ingredientsList) {
            val count = ingredientOccurrences.getOrDefault(ingredient, 0)
            ingredientOccurrences[ingredient] = count + 1
        }
        ingredientOccurrences.forEach { (ingredient, count) ->
            if (count > 1) {
                myFile.appendText("$ingredient x$count\n")
            } else {
                myFile.appendText("$ingredient\n")
            }
        }
        println("Saved!")
    }

    // Функция для выполнения запросов пользователя
    fun ask(connection: Connection) {
        val statement = connection.createStatement()
        //var isPlanSaved: Boolean = false
        do {
            println("What would you like to do (add, show, plan, save, exit)?")
            val operation = readLine().toString()
            when (operation) {
                "add" -> {
                    add(connection)
                }

                "show" -> {
                    show()
                }

                "plan" -> {
                    plan(connection)
                }

                "save" -> {
                    checkPlan(connection)
                    if (isPlanSaved) {
                        save(ingredientsList)
                    } else println("Unable to save. Plan your meals first.")
                }

                "exit" -> {
                    println("Bye!")
                    statement.close()
                    return
                }

                else -> println("Invalid operation. Please try again.")
            }
        } while (true)
    }
}

fun main() {
    val url = "jdbc:sqlite:meals.db"
    val connection = DriverManager.getConnection(url)
    val mealPlanner = MealPlanner(connection)
    mealPlanner.createTableIfNotExists()

    try {
        mealPlanner.ask(connection)
    } catch (e: SQLException) {
        println("Error: ${e.message}")
    } finally {
        connection.close()
    }
}