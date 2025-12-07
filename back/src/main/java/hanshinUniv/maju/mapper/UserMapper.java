package hanshinUniv.maju.mapper;

import hanshinUniv.maju.dto.user.UserRequest;
import hanshinUniv.maju.dto.user.UserResponse;
import hanshinUniv.maju.entity.Session;
import hanshinUniv.maju.entity.User;
import jdk.jfr.Name;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "regDate", ignore = true)
    @Mapping(target = "updDate", ignore = true)
    @Mapping(target = "sessions", ignore = true)
    User requestToEntity(UserRequest request);

    @Mapping(source = "sessions", target = "sessionIds", qualifiedByName = "sessionListIdList")
    UserResponse entityToResponse(User user);

    @Named("sessionListIdList")
    default List<Long> sessionListIdList(List<Session> sessions) {
        return sessions != null ? sessions.stream().map(Session::getId).toList() : null;
    }
}

