package com.webknot.kpi.repository;

import com.webknot.kpi.models.NotificationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    List<NotificationEvent> findByRecipient_EmployeeIdOrderByIdDesc(String recipientEmployeeId, Pageable pageable);

    List<NotificationEvent> findByRecipient_EmployeeIdAndIdLessThanOrderByIdDesc(
            String recipientEmployeeId,
            Long id,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndTypeInOrderByIdDesc(
            String recipientEmployeeId,
            Collection<String> types,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndTypeInAndIdLessThanOrderByIdDesc(
            String recipientEmployeeId,
            Collection<String> types,
            Long id,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndReadFalseOrderByIdDesc(
            String recipientEmployeeId,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndReadFalseAndIdLessThanOrderByIdDesc(
            String recipientEmployeeId,
            Long id,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndReadFalseAndTypeInOrderByIdDesc(
            String recipientEmployeeId,
            Collection<String> types,
            Pageable pageable
    );

    List<NotificationEvent> findByRecipient_EmployeeIdAndReadFalseAndTypeInAndIdLessThanOrderByIdDesc(
            String recipientEmployeeId,
            Collection<String> types,
            Long id,
            Pageable pageable
    );

    long countByRecipient_EmployeeIdAndReadFalse(String recipientEmployeeId);

    long countByRecipient_EmployeeIdAndReadFalseAndTypeIn(String recipientEmployeeId, Collection<String> types);

    Optional<NotificationEvent> findByIdAndRecipient_EmployeeId(Long id, String recipientEmployeeId);

    @Modifying
    @Query("""
        update NotificationEvent n
           set n.read = true,
               n.readAt = :now,
               n.updatedAt = :now
         where n.id = :id
           and n.recipient.employeeId = :recipientEmployeeId
           and n.read = false
    """)
    int markRead(@Param("id") Long id,
                 @Param("recipientEmployeeId") String recipientEmployeeId,
                 @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        update NotificationEvent n
           set n.read = true,
               n.readAt = :now,
               n.updatedAt = :now
         where n.recipient.employeeId = :recipientEmployeeId
           and n.read = false
    """)
    int markAllRead(@Param("recipientEmployeeId") String recipientEmployeeId,
                    @Param("now") LocalDateTime now);
}
