package com.personalfinance.app.repository;

import com.personalfinance.app.model.Category;
import com.personalfinance.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserId(Long userId);
}
