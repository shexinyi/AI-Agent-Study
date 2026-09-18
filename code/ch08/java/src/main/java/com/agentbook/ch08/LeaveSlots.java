package com.agentbook.ch08;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public record LeaveSlots(
        String employee,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        int days,
        @JsonProperty("leave_type") String leaveType
) {
    private static final Set<String> TYPES = Set.of("年假", "事假", "病假");

    public void validate() {
        if (employee == null || employee.length() < 2) {
            throw new IllegalArgumentException("employee 太短");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("日期缺失");
        }
        if (days < 1 || days > 30) {
            throw new IllegalArgumentException("days 超范围");
        }
        int delta = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (delta != days) {
            throw new IllegalArgumentException("days=" + days + " 与日期区间 " + delta + " 天不一致");
        }
        if (leaveType != null && !TYPES.contains(leaveType)) {
            throw new IllegalArgumentException("leave_type 非法: " + leaveType);
        }
    }
}
