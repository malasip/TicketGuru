package fi.rbmk.ticketguru.userGroup;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface UserGroupRepository extends CrudRepository<UserGroup, Long> {

	List<UserGroup> findByName(@Param("name") String name);

}