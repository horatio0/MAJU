package hanshinUniv.maju.mapper;

import hanshinUniv.maju.dto.session.SessionRequest;
import hanshinUniv.maju.dto.session.SessionResponse;
import hanshinUniv.maju.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.SortedMap;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    Session requestToEntity(SessionRequest request);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "sessionQuestionIds", ignore = true)
    SessionResponse entityToResponse(Session entity);
}
