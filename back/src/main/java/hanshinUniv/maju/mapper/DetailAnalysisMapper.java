package hanshinUniv.maju.mapper;

import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisRequest;
import hanshinUniv.maju.dto.detailanalysis.DetailAnalysisResponse;
import hanshinUniv.maju.entity.DetailAnalysis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DetailAnalysisMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commonAnalysis", ignore = true)
    DetailAnalysis requestToEntity(DetailAnalysisRequest request);

    @Mapping(source = "commonAnalysis.id", target = "commonAnalysisId")
    DetailAnalysisResponse entityToResponse(DetailAnalysis entity);
}
