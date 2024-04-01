package mealplanner

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

data class Meal(val category: String, val name: String, val ingredients: MutableSet<String>)

class MealPlanner(val connection: Connection) {
    init {
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        val statement = connection.createStatement()

        // Check if tables exist
        val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='meals'")
        
        if (!resultSet.next()) {
            // Create tables if they don't exist
            statement.executeUpdate("CREATE TABLE meals (meal_id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, meal TEXT)")
            statement.executeUpdate("CREATE TABLE ingredients(ingredient TEXT, ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER)")
            statement.executeUpdate("CREATE TABLE plan (option TEXT, category TEXT, meal_id INTEGER)")
        }

        resultSet.close()
        statement.close()
    }

    fun addMeal() {
        // Your addMeal function implementation here
    }

    fun showMeals() {
        // Your showMeals function implementation here
    }

    fun planMeals() {
        daysOfWeek.forEach {
            println(it)
        }
    }

    fun askUser() {
        // Your askUser function implementation here
    }
}

fun main() {
    val url = "jdbc:sqlite:meals.db"
    val connection = DriverManager.getConnection(url)

    val mealPlanner = MealPlanner(connection)
    mealPlanner.askUser()

    connection.close()
}
