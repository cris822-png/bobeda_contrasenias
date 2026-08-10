package com.vault.api.repository;

import com.vault.api.model.BannedIp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannedIpRepository extends JpaRepository<BannedIp, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BannedIp b WHERE b.ipAddress = :ipAddress")
    Optional<BannedIp> findByIpAddressForUpdate(@Param("ipAddress") String ipAddress);
}
