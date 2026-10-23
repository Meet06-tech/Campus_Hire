package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import com.placementmanagement.api.placement.entity.Company;
import com.placementmanagement.api.placement.entity.PlacementDrive;

class PlacementMappingTest {
    @Test
    void mapsExistingCompanyAndDriveTables() throws Exception {
        assertEquals("companies", Company.class.getAnnotation(Table.class).name());
        assertEquals("placement_drives", PlacementDrive.class.getAnnotation(Table.class).name());
        assertColumn(Company.class, "description", "profile");
        assertColumn(Company.class, "website", "website_url");
        assertColumn(PlacementDrive.class, "title", "job_title");
        assertColumn(PlacementDrive.class, "packageSalary", "compensation_package");
        assertColumn(PlacementDrive.class, "applicationDeadline", "application_closes_at");
    }

    private void assertColumn(Class<?> type, String fieldName, String columnName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        assertEquals(columnName, field.getAnnotation(Column.class).name());
    }
}
