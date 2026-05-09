package tn.esprit.spring.procedureservice.dialysis.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "dialysis_plans")
public class DialysisPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String patientId;
    private String firstName;
    private String lastName;
    private String doctorId;
    private String dialysisType;
    private Integer sessionsPerWeek;
    private Integer sessionDurationMinutes;
    private LocalDate startDate;
    private LocalDate endDate;
    private String daysOfWeek;
    private Integer bloodFlowRate;
    private Integer dialysateFlowRate;
    private Integer ultrafiltrationGoal;
    private String dialysisCenterId;
    private String roomNumber;
    private String machineId;
    private String status;

    public DialysisPlan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDialysisType() { return dialysisType; }
    public void setDialysisType(String dialysisType) { this.dialysisType = dialysisType; }
    public Integer getSessionsPerWeek() { return sessionsPerWeek; }
    public void setSessionsPerWeek(Integer sessionsPerWeek) { this.sessionsPerWeek = sessionsPerWeek; }
    public Integer getSessionDurationMinutes() { return sessionDurationMinutes; }
    public void setSessionDurationMinutes(Integer sessionDurationMinutes) { this.sessionDurationMinutes = sessionDurationMinutes; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public Integer getBloodFlowRate() { return bloodFlowRate; }
    public void setBloodFlowRate(Integer bloodFlowRate) { this.bloodFlowRate = bloodFlowRate; }
    public Integer getDialysateFlowRate() { return dialysateFlowRate; }
    public void setDialysateFlowRate(Integer dialysateFlowRate) { this.dialysateFlowRate = dialysateFlowRate; }
    public Integer getUltrafiltrationGoal() { return ultrafiltrationGoal; }
    public void setUltrafiltrationGoal(Integer ultrafiltrationGoal) { this.ultrafiltrationGoal = ultrafiltrationGoal; }
    public String getDialysisCenterId() { return dialysisCenterId; }
    public void setDialysisCenterId(String dialysisCenterId) { this.dialysisCenterId = dialysisCenterId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
