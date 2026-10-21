package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.service.UniversityEmailParser;

class UniversityEmailParserTest {
    private final UniversityEmailParser parser = new UniversityEmailParser();

    @Test
    void extractsUppercaseCodeFromApprovedEmailPattern() {
        assertEquals("MCA", parser.extractCode("25MCA001@charusat.edu.in"));
    }

    @Test
    void acceptsDomainCaseInsensitivelyAndNormalizesCode() {
        assertEquals("MCA", parser.extractCode("25mca001@CHARUSAT.EDU.IN"));
    }

    @Test
    void rejectsInvalidDomain() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> parser.extractCode("25MCA001@example.edu"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rejectsMalformedLocalPart() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> parser.extractCode("MCA001@charusat.edu.in"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
