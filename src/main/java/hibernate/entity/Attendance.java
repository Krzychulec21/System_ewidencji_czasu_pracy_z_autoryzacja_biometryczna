package hibernate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Attendance() {
    }

    public Attendance(Employee employee, LocalDateTime entryTime, LocalDateTime exitTime) {
        this.employee = employee;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", employee=" + employee +
                ", entryTime=" + entryTime +
                ", exitTime=" + exitTime +
                '}';
    }
}