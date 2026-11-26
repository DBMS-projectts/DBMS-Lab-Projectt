package com.lims.repository;

import com.lims.entity.MemberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberTypeRepository extends JpaRepository<MemberType, Long> {
    Optional<MemberType> findByTypeName(String typeName);
}
