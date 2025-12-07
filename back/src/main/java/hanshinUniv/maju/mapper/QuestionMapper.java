package hanshinUniv.maju.mapper;

import hanshinUniv.maju.dto.question.QuestionRequest;
import hanshinUniv.maju.dto.question.QuestionResponse;
import hanshinUniv.maju.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionMapper {
    @Mapping(target = "id", ignore = true)
    Question requestToEntity(QuestionRequest request);

    QuestionResponse entityToResponse(Question entity);
}
