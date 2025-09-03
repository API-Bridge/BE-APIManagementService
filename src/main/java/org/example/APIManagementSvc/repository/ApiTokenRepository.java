package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * API 토큰 데이터 접근 리포지토리
 * 
 * API 토큰의 CRUD 작업과 토큰 관리를 위한 쿼리 메서드를 제공
 * 
 * 주요 기능:
 * - 자격증명별 토큰 조회
 * - 만료 예정 토큰 검색
 * - 토큰 존재 여부 확인
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    /**
     * 특정 자격증명의 토큰 조회
     * 
     * 자격증명 ID를 기반으로 해당 자격증명의 토큰을 조회
     * SGIS와 같은 특정 API 제공자의 토큰 관리에 사용
     * 
     * @param credentialId 자격증명 ID
     * @return Optional<ApiToken> 해당 자격증명의 토큰 (있는 경우)
     */
    @Query("SELECT t FROM ApiToken t WHERE t.credential.credentialId = :credentialId")
    Optional<ApiToken> findByCredentialId(@Param("credentialId") String credentialId);

    /**
     * 특정 자격증명의 토큰 존재 여부 확인
     * 
     * @param credentialId 자격증명 ID
     * @return boolean 토큰 존재 여부
     */
    @Query("SELECT COUNT(t) > 0 FROM ApiToken t WHERE t.credential.credentialId = :credentialId")
    boolean existsByCredentialId(@Param("credentialId") String credentialId);

    /**
     * 만료 예정 토큰 조회
     * 
     * 지정된 시간 이전에 만료되는 토큰들을 조회
     * 토큰 갱신이 필요한 시점을 판단하기 위해 사용
     * 
     * @param expiryThreshold 만료 기준 시간
     * @return Optional<ApiToken> 만료 예정 토큰 목록
     */
    @Query("SELECT t FROM ApiToken t WHERE t.expiresAt <= :expiryThreshold")
    Optional<ApiToken> findTokensExpiringBefore(@Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * 특정 자격증명의 유효한 토큰 조회
     * 
     * 자격증명 ID와 만료시간을 조건으로 현재 유효한 토큰을 조회
     * 
     * @param credentialId 자격증명 ID
     * @param currentTime 현재 시간
     * @return Optional<ApiToken> 유효한 토큰 (있는 경우)
     */
    @Query("SELECT t FROM ApiToken t WHERE t.credential.credentialId = :credentialId AND t.expiresAt > :currentTime")
    Optional<ApiToken> findValidTokenByCredentialId(@Param("credentialId") String credentialId, 
                                                   @Param("currentTime") LocalDateTime currentTime);
}