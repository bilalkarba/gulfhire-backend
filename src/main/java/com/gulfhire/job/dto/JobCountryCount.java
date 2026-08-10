package com.gulfhire.job.dto;

/** A market (country) with the number of active jobs posted there. */
public record JobCountryCount(String country, Long count) {
}
