CREATE TABLE employees (
    id UUID PRIMARY KEY,

    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    department VARCHAR(100) NOT NULL,
    job_title VARCHAR(150) NOT NULL,

    employment_status VARCHAR(20) NOT NULL,
    termination_date DATE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_employees_employee_code
        UNIQUE (employee_code),

    CONSTRAINT uk_employees_email
        UNIQUE (email),

    CONSTRAINT chk_employees_status
        CHECK (employment_status IN ('ACTIVE', 'TERMINATED')),

    CONSTRAINT chk_employees_termination_date
        CHECK (
            (employment_status = 'ACTIVE' AND termination_date IS NULL)
            OR
            (employment_status = 'TERMINATED' AND termination_date IS NOT NULL)
        )
);

CREATE INDEX idx_employees_country
    ON employees (country_code);

CREATE INDEX idx_employees_department
    ON employees (department);

CREATE INDEX idx_employees_status
    ON employees (employment_status);