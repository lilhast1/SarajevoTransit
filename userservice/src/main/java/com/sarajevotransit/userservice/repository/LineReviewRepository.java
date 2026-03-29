package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.LineReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineReviewRepository extends JpaRepository<LineReview, Long> {

    List<LineReview> findByUserIdOrderByCreatedAtDesc(Long userId);
}
