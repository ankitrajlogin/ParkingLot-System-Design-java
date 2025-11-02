-- parking_lot_schema.sql
-- =====================================================================
-- parking_lot_schema.sql
--
-- Clean, well-formatted SQL schema for the Parking Lot Management System.
-- Primary target: PostgreSQL (recommended). Minimal tweaks are noted for MySQL.
-- =====================================================================

-- DROP existing objects (safe for re-run during development). Order matters due to FKs.
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS vehicle;
DROP TABLE IF EXISTS slot;
DROP TABLE IF EXISTS floor;
DROP TABLE IF EXISTS parking_lot;
DROP TABLE IF EXISTS vehicle_type;

-- =====================================================================
-- 1) Master data: vehicle_type
-- =====================================================================
CREATE TABLE IF NOT EXISTS vehicle_type (
    id SERIAL PRIMARY KEY,
    type_name VARCHAR(30) NOT NULL UNIQUE -- e.g. CAR, BIKE, TRUCK
);

-- =====================================================================
-- 2) Parking lot and floors
-- =====================================================================
CREATE TABLE IF NOT EXISTS parking_lot (
    id VARCHAR(50) PRIMARY KEY, -- logical ID like PR1234
    name VARCHAR(200),
    address VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS floor (
    id SERIAL PRIMARY KEY,
    floor_number INT NOT NULL,
    parking_lot_id VARCHAR(50) NOT NULL,
    CONSTRAINT fk_floor_parkinglot FOREIGN KEY (parking_lot_id)
        REFERENCES parking_lot (id) ON DELETE CASCADE
);

-- =====================================================================
-- 3) Slots
-- =====================================================================
CREATE TABLE IF NOT EXISTS slot (
    id SERIAL PRIMARY KEY,
    slot_number INT NOT NULL,
    floor_id INT NOT NULL,
    vehicle_type_id INT NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_slot_floor FOREIGN KEY (floor_id) REFERENCES floor (id) ON DELETE CASCADE,
    CONSTRAINT fk_slot_vehicletype FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (id)
);

-- Composite index to accelerate allocation queries (floor, vehicle type, availability, slot)
CREATE INDEX IF NOT EXISTS idx_slot_floor_vehicle ON slot (floor_id, vehicle_type_id, is_occupied, slot_number);

-- =====================================================================
-- 4) Vehicles
-- =====================================================================
CREATE TABLE IF NOT EXISTS vehicle (
    id SERIAL PRIMARY KEY,
    registration_no VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(50),
    vehicle_type_id INT NOT NULL,
    CONSTRAINT fk_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (id)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_reg ON vehicle (registration_no);

-- =====================================================================
-- 5) Tickets (parking events)
--    Logical ticket id format: <parking_lot_id>_<floor_number>_<slot_number>
-- =====================================================================
CREATE TABLE IF NOT EXISTS ticket (
    id VARCHAR(100) PRIMARY KEY,
    parking_lot_id VARCHAR(50) NOT NULL,
    slot_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    issue_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP WITH TIME ZONE NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ticket_slot FOREIGN KEY (slot_id) REFERENCES slot (id),
    CONSTRAINT fk_ticket_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    CONSTRAINT fk_ticket_parkinglot FOREIGN KEY (parking_lot_id) REFERENCES parking_lot (id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_active ON ticket (is_active);

-- =====================================================================
-- Sample data (wrap in a transaction so partial failures roll back)
-- NOTE: If using MySQL, replace `SERIAL` with `INT AUTO_INCREMENT` and
-- adjust `TIMESTAMP WITH TIME ZONE` to `TIMESTAMP` (MySQL doesn't store TZ).
-- =====================================================================
BEGIN;

-- vehicle categories
INSERT INTO vehicle_type (type_name)
VALUES ('CAR'), ('BIKE'), ('TRUCK')
ON CONFLICT (type_name) DO NOTHING; -- Postgres upsert; for MySQL use INSERT IGNORE

-- parking lot
INSERT INTO parking_lot (id, name, address)
VALUES ('PR1234', 'Main City Parking', 'Downtown Road')
ON CONFLICT (id) DO NOTHING;

-- floors (assumes parking_lot PR1234 exists)
INSERT INTO floor (floor_number, parking_lot_id)
VALUES (1, 'PR1234'), (2, 'PR1234');

-- Add example slots. We use subselects for vehicle_type ids so this is resilient.
INSERT INTO slot (slot_number, floor_id, vehicle_type_id)
VALUES
  (1, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='TRUCK')),
  (2, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (3, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (4, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='CAR')),
  (5, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='CAR')),
  (6, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=1), (SELECT id FROM vehicle_type WHERE type_name='CAR')),
  (1, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='TRUCK')),
  (2, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (3, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (4, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='CAR')),
  (5, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='CAR')),
  (6, (SELECT id FROM floor WHERE parking_lot_id='PR1234' AND floor_number=2), (SELECT id FROM vehicle_type WHERE type_name='CAR'));

-- sample vehicle
INSERT INTO vehicle (registration_no, color, vehicle_type_id)
VALUES ('KA-01-HH-1234', 'White', (SELECT id FROM vehicle_type WHERE type_name='CAR'))
ON CONFLICT (registration_no) DO NOTHING;

COMMIT;

-- =====================================================================
-- Example operational queries (use in application logic)
-- 1) Find first free slot for vehicle type 'CAR' in the whole parking lot
--    (for concurrency use SELECT ... FOR UPDATE in transaction)
-- =====================================================================
-- Parameters: :vehicle_type_name, :parking_lot_id (optional)
SELECT s.id AS slot_id,
       f.floor_number,
       s.slot_number
FROM slot s
JOIN floor f ON s.floor_id = f.id
JOIN vehicle_type vt ON s.vehicle_type_id = vt.id
WHERE vt.type_name = 'CAR'
  AND s.is_occupied = FALSE
  AND f.parking_lot_id = 'PR1234' -- remove or parameterize as needed
ORDER BY f.floor_number, s.slot_number
LIMIT 1;

-- =====================================================================
-- 2) Reserve (park) flow: wrap in a transaction
--   - Insert/find vehicle
--   - Insert ticket (id format: <parking_lot>_<floor>_<slot>)
--   - Mark slot is_occupied = true
-- =====================================================================
-- Example (pseudo-parameters shown as :param)
-- BEGIN;
--   -- ensure vehicle exists (insert or get id)
--   -- insert ticket with id = :ticket_id
--   INSERT INTO ticket (id, parking_lot_id, slot_id, vehicle_id)
--   VALUES (:ticket_id, :parking_lot_id, :slot_id, :vehicle_id);
--   UPDATE slot SET is_occupied = TRUE WHERE id = :slot_id;
-- COMMIT;

-- =====================================================================
-- 3) Unpark flow: mark ticket inactive and free slot
-- =====================================================================
-- BEGIN;
--   UPDATE ticket SET exit_time = CURRENT_TIMESTAMP, is_active = FALSE
--     WHERE id = :ticket_id AND is_active = TRUE;
--   UPDATE slot SET is_occupied = FALSE WHERE id = :slot_id;
-- COMMIT;

-- =====================================================================
-- Notes:
--  - For MySQL: replace SERIAL with INT AUTO_INCREMENT, and use
--    INSERT IGNORE or ON DUPLICATE KEY UPDATE for upserts.
--  - Consider SELECT ... FOR UPDATE or advisory locks to avoid race
--    conditions when multiple processes try to allocate slots.
-- =====================================================================

