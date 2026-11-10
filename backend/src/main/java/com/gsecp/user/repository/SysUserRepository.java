package com.gsecp.user.repository;

import com.gsecp.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    @Query(value = """
            SELECT r.code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = :userId
               AND ur.is_deleted = 0
               AND r.is_deleted = 0
            """, nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
