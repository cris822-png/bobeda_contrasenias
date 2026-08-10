package com.vault.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "banned_ips", schema = "vault")
public class BannedIp {

    @Id
    @Column(name = "ip_address", columnDefinition = "inet")
    @JdbcTypeCode(SqlTypes.INET)
    private String ipAddress;

    @Column(name = "ban_count", nullable = false)
    private Integer banCount = 0;

    @Column(name = "banned_until", nullable = false)
    private OffsetDateTime bannedUntil;

    @Column(name = "last_banned_at", nullable = false)
    private OffsetDateTime lastBannedAt;

    public BannedIp() {}

    public BannedIp(String ipAddress, Integer banCount, OffsetDateTime bannedUntil, OffsetDateTime lastBannedAt) {
        this.ipAddress = ipAddress;
        this.banCount = banCount;
        this.bannedUntil = bannedUntil;
        this.lastBannedAt = lastBannedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getBanCount() {
        return banCount;
    }

    public void setBanCount(Integer banCount) {
        this.banCount = banCount;
    }

    public OffsetDateTime getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(OffsetDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public OffsetDateTime getLastBannedAt() {
        return lastBannedAt;
    }

    public void setLastBannedAt(OffsetDateTime lastBannedAt) {
        this.lastBannedAt = lastBannedAt;
    }
}
