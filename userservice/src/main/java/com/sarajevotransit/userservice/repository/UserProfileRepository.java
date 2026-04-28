package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @EntityGraph(attributePaths = { "wallet", "preference" })
    @Query("select u from UserProfile u")
    List<UserProfile> findAllWithWalletAndPreference();

    @EntityGraph(attributePaths = { "wallet", "preference" })
    @Query("select u from UserProfile u")
    Page<UserProfile> findAllWithWalletAndPreference(Pageable pageable);

    Optional<UserProfile> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
