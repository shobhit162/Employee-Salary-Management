CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE compensations (
    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_compensations_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT chk_compensations_amount
        CHECK (amount > 0),

    CONSTRAINT chk_compensations_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_compensations_dates
        CHECK (
            effective_to IS NULL
            OR effective_to > effective_from
        )
);

CREATE INDEX idx_compensations_employee
    ON compensations(employee_id);

CREATE INDEX idx_compensations_employee_effective_from
    ON compensations(employee_id, effective_from);

ALTER TABLE compensations
ADD CONSTRAINT no_overlapping_compensation_periods
EXCLUDE USING GIST (
    employee_id WITH =,
    daterange(effective_from, effective_to, '[)') WITH &&
);