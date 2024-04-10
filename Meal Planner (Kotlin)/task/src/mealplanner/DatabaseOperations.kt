package mealplanner

import java.sql.Connection

// Функция для вставки или обновления записи о блюде в таблице meals
fun insertOrUpdateMeal(connection: Connection, category: String, name: String): Int {
    val checkMealQuery = "SELECT meal_id FROM meals WHERE category = ? AND meal = ?"
    val checkMealPreparedStatement = connection.prepareStatement(checkMealQuery)
    checkMealPreparedStatement.setString(1, category)
    checkMealPreparedStatement.setString(2, name)
    val resultSet = checkMealPreparedStatement.executeQuery()

    return if (!resultSet.next()) {
        // Вставка блюда в таблицу meals, если оно не существует
        val insertMealQuery = "INSERT INTO meals (category, meal) VALUES (?, ?)"
        val mealPreparedStatement = connection.prepareStatement(insertMealQuery)
        mealPreparedStatement.setString(1, category)
        mealPreparedStatement.setString(2, name)
        mealPreparedStatement.executeUpdate()

        // Получение meal_id вставленного блюда
        val mealIdQuery = "SELECT meal_id FROM meals WHERE meal = ?"
        val mealIdPreparedStatement = connection.prepareStatement(mealIdQuery)
        mealIdPreparedStatement.setString(1, name)
        val mealIdResultSet = mealIdPreparedStatement.executeQuery()
        mealIdResultSet.getInt("meal_id")
    } else {
        resultSet.getInt("meal_id")
    }
}

// Функция для вставки ингредиентов в таблицу ingredients
fun insertIngredients(connection: Connection, mealId: Int, ingredients: List<String>) {
    val insertIngredientsQuery = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)"
    val ingredientsPreparedStatement = connection.prepareStatement(insertIngredientsQuery)
    for (ingredient in ingredients) {
        ingredientsPreparedStatement.setString(1, ingredient)
        ingredientsPreparedStatement.setInt(2, mealId)
        ingredientsPreparedStatement.executeUpdate()
    }
}