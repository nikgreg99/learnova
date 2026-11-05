package com.learnova.repository;

import com.learnova.model.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByNameIn(Collection<String> names);
}
