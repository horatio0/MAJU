package hanshinUniv.maju.controller;

import hanshinUniv.maju.dto.ApiResponse;
import hanshinUniv.maju.dto.question.QuestionResponse;
import hanshinUniv.maju.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuestionControllerTest {

    @Test
    public void getQuestions_returnsListWrappedInApiResponse_unit() {
        QuestionService questionService = mock(QuestionService.class);

        QuestionResponse q1 = QuestionResponse.builder().id(1L).category("general").question("What is your name?").build();
        QuestionResponse q2 = QuestionResponse.builder().id(2L).category("behavior").question("Tell me about a challenge.").build();

        when(questionService.findAll()).thenReturn(List.of(q1, q2));

        QuestionController controller = new QuestionController(questionService);
        var responseEntity = controller.getQuestions();

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ApiResponse<List<QuestionResponse>> apiResponse = responseEntity.getBody();
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertNotNull(apiResponse.getData());
        assertEquals(2, apiResponse.getData().size());
        assertEquals(1L, apiResponse.getData().get(0).getId());
        assertEquals("general", apiResponse.getData().get(0).getCategory());
        assertTrue(apiResponse.getData().get(1).getQuestion().contains("challenge"));
    }
}
