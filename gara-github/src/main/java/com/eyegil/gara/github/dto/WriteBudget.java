package com.eyegil.gara.github.dto;

/**
 * State of the shared GitHub commit budget: commits that can go out right now, the burst size and the hourly rate.
 */
public record WriteBudget(double available, int burst, int perHour) {
}
