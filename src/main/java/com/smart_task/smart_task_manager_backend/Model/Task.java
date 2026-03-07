package com.smart_task.smart_task_manager_backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    private String remark;

    private Integer percentage;

    @Column(name = "task_reference_id")
    private String taskReferenceId;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "delay_reason", length = 500)
    private String delayReason;

    @Column(name = "is_delayed")
    private Boolean isDelayed = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_user_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"tasks", "password", "hibernateLazyInitializer", "handler"})
    private User assignedTo;

    // ✅ NEW: Track which admin assigned this task
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_by_user_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"tasks", "password", "hibernateLazyInitializer", "handler"})
    private User assignedBy;

    // Constructors
    public Task() {
        this.createdDate = LocalDateTime.now();
    }

    public Task(String title, String priority, String status, LocalDate dueDate,
                String remark, Integer percentage, String taskReferenceId) {
        this.title = title;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.remark = remark;
        this.percentage = percentage;
        this.taskReferenceId = taskReferenceId;
        this.createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getPercentage() { return percentage; }
    public void setPercentage(Integer percentage) { this.percentage = percentage; }

    public String getTaskReferenceId() { return taskReferenceId; }
    public void setTaskReferenceId(String taskReferenceId) { this.taskReferenceId = taskReferenceId; }

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    // ✅ NEW: assignedBy getter/setter
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public LocalDate getExpectedCompletionDate() { return expectedCompletionDate; }
    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }

    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String delayReason) { this.delayReason = delayReason; }

    public Boolean getIsDelayed() { return isDelayed; }
    public void setIsDelayed(Boolean isDelayed) { this.isDelayed = isDelayed; }

    public Object getCreatedBy() { return null; }
    public Object getDescription() { return null; }
}
