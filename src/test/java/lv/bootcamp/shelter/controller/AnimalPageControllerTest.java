package lv.bootcamp.shelter.controller;

import lv.bootcamp.shelter.config.SecurityConfig;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnimalPageController.class)
@Import(SecurityConfig.class)
class AnimalPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @Test
    void listAnimals_shouldRenderAnimalsView() throws Exception {
        when(animalService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/animals"))
                .andExpect(status().isOk())
                .andExpect(view().name("animals"));
    }

    @Test
    void listAnimals_shouldAddAnimalsToModel() throws Exception {
        var animals = List.of(new AnimalResponse(
                1L, "Rex", AnimalType.DOG, "Labrador", 3, "Friendly", AnimalStatus.AVAILABLE));
        when(animalService.findAll()).thenReturn(animals);

        mockMvc.perform(get("/animals"))
                .andExpect(model().attribute("animals", animals));
    }

    @Test
    void listAnimals_shouldRenderAnimalNameInHtml() throws Exception {
        var animals = List.of(new AnimalResponse(
                1L, "Rex", AnimalType.DOG, "Labrador", 3, "Friendly", AnimalStatus.AVAILABLE));
        when(animalService.findAll()).thenReturn(animals);

        mockMvc.perform(get("/animals"))
                .andExpect(content().string(containsString("Rex")));
    }
}
