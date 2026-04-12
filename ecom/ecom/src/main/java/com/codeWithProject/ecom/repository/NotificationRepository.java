package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByEmployeIdAndLuFalseOrderByDateCreationDesc(Long employeId);

    List<Notification> findByEmployeIdOrderByDateCreationDesc(Long employeId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.employeId = :employeId AND n.lu = false")
    void markAllAsReadByEmploye(@Param("employeId") Long employeId);
}