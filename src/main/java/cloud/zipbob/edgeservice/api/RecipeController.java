package cloud.zipbob.edgeservice.api;

import cloud.zipbob.edgeservice.global.recipe.RecipeConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeConsumer recipeConsumer;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRecipes() {
        SseEmitter sseEmitter = new SseEmitter();
        new Thread(() -> {
            try {
                while (true) {
                    String recipe = recipeConsumer.getRecipe();
                    if (recipe != null) {
                        sseEmitter.send(recipe);
                    }
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        }).start();
        return sseEmitter;
    }
}
