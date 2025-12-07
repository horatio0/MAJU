package hanshinUniv.maju.mapper;

import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisRequest;
import hanshinUniv.maju.dto.commonanalysis.CommonAnalysisResponse;
import hanshinUniv.maju.entity.CommonAnalysis;
import hanshinUniv.maju.entity.DetailAnalysis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = DetailAnalysisMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommonAnalysisMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "answer", ignore = true)
    @Mapping(target = "detailAnalyses", ignore = true)
    CommonAnalysis requestToEntity(CommonAnalysisRequest request);

    @Mapping(source = "answer.id", target = "answerId")
    @Mapping(source = "detailAnalyses", target = "detailAnalysisIds", qualifiedByName = "detailAnalysisListIdList")
    CommonAnalysisResponse entityToResponse(CommonAnalysis commonAnalysis);

    @Named("detailAnalysisListIdList")
    default List<Long> detailAnalysisListIdList(List<DetailAnalysis> detailAnalyses) {
        return detailAnalyses != null ? detailAnalyses.stream().map(DetailAnalysis::getId).toList() : null;
    }
}
