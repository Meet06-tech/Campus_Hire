-- Provisional university mapping. Review and approve before execution.
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.department_email_codes') IS NULL THEN
        RAISE EXCEPTION
            'department_email_codes does not exist; apply V2__create_department_email_codes.sql first';
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            VALUES
                ('TECH', 'Faculty of Technology & Engineering (CSPIT & DEPSTAR)'),
                ('MCA', 'Computer Science & Applications (CMPICA)'),
                ('PH', 'Pharmacy (RPCP)'),
                ('MS', 'Management Studies (I2IM)'),
                ('SCI', 'Applied Sciences (PDPIAS)'),
                ('MED', 'Medical Sciences'),
                ('HS', 'Humanities & Social Sciences (CHSS)')
        ) AS expected(department_code, department_name)
        JOIN departments existing
            ON existing.department_name = expected.department_name
        WHERE existing.department_code <> expected.department_code
    ) THEN
        RAISE EXCEPTION
            'A department name is already assigned to a different canonical code';
    END IF;
END
$$;

INSERT INTO departments (department_code, department_name, is_active)
VALUES
    ('TECH', 'Faculty of Technology & Engineering (CSPIT & DEPSTAR)', TRUE),
    ('MCA', 'Computer Science & Applications (CMPICA)', TRUE),
    ('PH', 'Pharmacy (RPCP)', TRUE),
    ('MS', 'Management Studies (I2IM)', TRUE),
    ('SCI', 'Applied Sciences (PDPIAS)', TRUE),
    ('MED', 'Medical Sciences', TRUE),
    ('HS', 'Humanities & Social Sciences (CHSS)', TRUE)
ON CONFLICT (department_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            VALUES
                ('TECH', 'Faculty of Technology & Engineering (CSPIT & DEPSTAR)'),
                ('MCA', 'Computer Science & Applications (CMPICA)'),
                ('PH', 'Pharmacy (RPCP)'),
                ('MS', 'Management Studies (I2IM)'),
                ('SCI', 'Applied Sciences (PDPIAS)'),
                ('MED', 'Medical Sciences'),
                ('HS', 'Humanities & Social Sciences (CHSS)')
        ) AS expected(department_code, department_name)
        LEFT JOIN departments existing
            ON existing.department_code = expected.department_code
        WHERE existing.department_id IS NULL
           OR existing.department_name <> expected.department_name
           OR existing.is_active IS NOT TRUE
    ) THEN
        RAISE EXCEPTION
            'A canonical department code has an unexpected name or inactive configuration';
    END IF;
END
$$;

INSERT INTO department_email_codes (department_id, email_code)
SELECT d.department_id, mapping.email_code
FROM (
    VALUES
        ('TECH', 'CE'), ('TECH', 'IT'), ('TECH', 'CSE'), ('TECH', 'EC'),
        ('TECH', 'EE'), ('TECH', 'ME'), ('TECH', 'CL'),
        ('MCA', 'MCA'),
        ('PH', 'PH'),
        ('MS', 'MS'),
        ('SCI', 'BS'), ('SCI', 'MATH'), ('SCI', 'PS'), ('SCI', 'CH'),
        ('MED', 'NR'), ('MED', 'PT'), ('MED', 'BMLT'), ('MED', 'MLT'), ('MED', 'OP'),
        ('HS', 'HS')
) AS mapping(department_code, email_code)
JOIN departments d ON d.department_code = mapping.department_code
ON CONFLICT (email_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            VALUES
                ('TECH', 'CE'), ('TECH', 'IT'), ('TECH', 'CSE'), ('TECH', 'EC'),
                ('TECH', 'EE'), ('TECH', 'ME'), ('TECH', 'CL'),
                ('MCA', 'MCA'),
                ('PH', 'PH'),
                ('MS', 'MS'),
                ('SCI', 'BS'), ('SCI', 'MATH'), ('SCI', 'PS'), ('SCI', 'CH'),
                ('MED', 'NR'), ('MED', 'PT'), ('MED', 'BMLT'), ('MED', 'MLT'), ('MED', 'OP'),
                ('HS', 'HS')
        ) AS expected(department_code, email_code)
        JOIN departments d
            ON d.department_code = expected.department_code
        LEFT JOIN department_email_codes existing
            ON existing.email_code = expected.email_code
        WHERE existing.department_email_code_id IS NULL
           OR existing.department_id <> d.department_id
           OR existing.is_active IS NOT TRUE
    ) THEN
        RAISE EXCEPTION
            'An email-code mapping has conflicting department ownership or inactive configuration';
    END IF;
END
$$;

COMMIT;
