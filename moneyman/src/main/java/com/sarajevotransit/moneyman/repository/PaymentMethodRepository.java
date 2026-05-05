package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.List;

public interface PaymentMethodRepository
        extends JpaRepository<PaymentMethod, Long>, PagingAndSortingRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserId(Long userId);

    Page<PaymentMethod> findByUserId(Long userId, Pageable pageable);
}