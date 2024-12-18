package cloud.zipbob.edgeservice.global.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeConsumerTest {

    @Test
    @DisplayName("레시피 받기")
    public void testReceiveRecipe() throws InterruptedException {
        RecipeConsumer consumer = new RecipeConsumer();
        String recipeMessage = "{\"title\":\"Pasta\", \"ingredients\":[\"noodles\",\"sauce\"]}";
        consumer.receiveRecipe(recipeMessage);

        String result = consumer.getRecipe();
        assertEquals(recipeMessage, result);
    }
}
