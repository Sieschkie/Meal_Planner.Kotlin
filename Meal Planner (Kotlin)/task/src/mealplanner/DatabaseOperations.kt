package mealplanner

import java.sql.Connection
import java.sql.SQLException

// Function to insert or update a record about a meal in the meals table
fun insertOrUpdateMeal(connection: Connection, category: String, name: String): Int {
    val checkMealQuery = "SELECT meal_id FROM meals WHERE category = ? AND meal = ?"
    val checkMealPreparedStatement = connection.prepareStatement(checkMealQuery)
    checkMealPreparedStatement.setString(1, category)
    checkMealPreparedStatement.setString(2, name)
    val resultSet = checkMealPreparedStatement.executeQuery()

    return if (!resultSet.next()) {
        // Inserting the meal into the meals table if it does not exist
        val insertMealQuery = "INSERT INTO meals (category, meal) VALUES (?, ?)"
        val mealPreparedStatement = connection.prepareStatement(insertMealQuery)
        mealPreparedStatement.setString(1, category)
        mealPreparedStatement.setString(2, name)
        mealPreparedStatement.executeUpdate()

        // Getting the meal_id for the newly inserted meal
        mealPreparedStatement.generatedKeys.use { keys ->
            if (keys.next()) {
                keys.getInt(1)
            } else {
                throw SQLException("Failed to get meal_id for the newly inserted meal.")
            }
        }
    } else {
        resultSet.getInt("meal_id")
    }
}

// Function to insert ingredients into the ingredients table
fun insertIngredients(connection: Connection, mealId: Int, ingredients: List<String>) {
    val insertIngredientsQuery = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
    val ingredientsPreparedStatement = connection.prepareStatement(insertIngredientsQuery)
    for (ingredient in ingredients) {
        ingredientsPreparedStatement.setString(1, ingredient)
        ingredientsPreparedStatement.setInt(2, mealId)
        ingredientsPreparedStatement.executeUpdate()
    }
}
