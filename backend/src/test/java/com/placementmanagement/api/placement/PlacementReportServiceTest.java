package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.placementmanagement.api.placement.service.PlacementReportService;

class PlacementReportServiceTest {
    @Test
    void formatsExistingLakhConventionAsLpa() {
        assertEquals("12 LPA", PlacementReportService.formatCtc(new BigDecimal("12.00"), "INR"));
        assertEquals("8.5 LPA", PlacementReportService.formatCtc(new BigDecimal("8.50"), "INR"));
    }

    @Test
    void convertsClearlyRupeeScaleInrWithoutChangingStoredData() {
        assertEquals("12 LPA", PlacementReportService.formatCtc(new BigDecimal("1200000"), "INR"));
    }

    @Test
    void formatsExportDatesWithoutTimeOrTimezone() {
        assertEquals("2026-11-04", PlacementReportService.formatDate(Instant.parse("2026-11-04T09:43:53.408207Z")));
        assertEquals("", PlacementReportService.formatDate(null));
    }

}
