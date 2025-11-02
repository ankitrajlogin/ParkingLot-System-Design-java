# 🅿️ Parking Lot Management System

A command-line based smart parking-lot system that supports multiple floors, different vehicle types, and automatic ticketing. Implemented in Java as a Maven project with a clean, extensible design.

---

## 📘 Overview

This project is a machine-coding style implementation of a Parking Lot Management System. It accepts commands (from an `input.txt` or stdin), manages parking across multiple floors and slots, issues and validates tickets, and prints results (to `output.txt` or stdout).

Key capabilities:
- Multi-floor parking
- Per-slot vehicle type restrictions (CAR / BIKE / TRUCK)
- Automatic ticket generation
- Display of free/occupied slots and counts per floor
- Graceful handling of invalid or unavailable operations
- Simple, extensible architecture for future features (new vehicle types, different allocation strategies)

### Ticket format

Ticket ID format: `<parking_lot_id>_<floor_no>_<slot_no>`
Example: `PR1234_2_5`

---

## 🧩 Input / Output

This project reads commands and writes results. By default the repository includes `input.txt` and `output.txt`. The application can be driven by piping commands or by providing file paths.

Supported commands (one per line):

- `create_parking_lot <parking_lot_id> <no_of_floors> <no_of_slots_per_floor>`
- `park_vehicle <vehicle_type> <reg_no> <color>`
- `unpark_vehicle <ticket_id>`
- `display <display_type> <vehicle_type>`
- `exit`

Display types:
- `free_count` : prints count of free slots for the vehicle type on every floor
- `free_slots` : prints list of free slot numbers for the vehicle type on every floor
- `occupied_slots` : prints list of occupied slot numbers for the vehicle type on every floor

Example input (in `input.txt`):

---

## 🗄️ Database Design

Below is a complete relational design for the Parking Lot Management System suitable for MySQL or PostgreSQL. It includes core entities, relationships, and example DDL, sample data, and operational queries. The SQL in the next fenced block is ready to copy/paste into Postgres (with MySQL notes included).

### 1) Core entities

- `vehicle_type` — master list of vehicle categories (CAR, BIKE, TRUCK, ...)
- `parking_lot` — a parking facility (e.g., PR1234)
- `floor` — a numbered floor within a parking lot
- `slot` — an individual slot on a floor; each slot is typed for vehicle categories
- `vehicle` — registered vehicle (reg_no, color, type)
- `ticket` — a parking event linking a vehicle to a slot and timestamps

### 2) Relationships (ER style)

- parking_lot 1 — N floor
- floor 1 — N slot
- slot 0..1 — 1 ticket (active) (historical tickets reference slots as well)
- vehicle 1 — N ticket (parking history)
- vehicle_type referenced by both vehicle and slot

Textual ER fragment:

[PARKING_LOT]──<contains>──[FLOOR]──<contains>──[SLOT]──<occupied_by>──[TICKET]──<belongs_to>──[VEHICLE]
					 │                                        │
					 └────────────<type>────────────[VEHICLE_TYPE]───────────────┘

### 3) Production-ready SQL (Postgres-focused, MySQL notes)

Copy the entire block below into a Postgres client. For MySQL change `SERIAL` to `INT AUTO_INCREMENT`, replace `ON CONFLICT` upserts with `INSERT IGNORE` or `ON DUPLICATE KEY UPDATE`, and change `TIMESTAMP WITH TIME ZONE` to `TIMESTAMP`.

```sql
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
```

### 4) Example inserts & sample data (quick reference)

- Add vehicle categories:

```sql
INSERT INTO vehicle_type (type_name) VALUES ('CAR'), ('BIKE'), ('TRUCK');
```

- Add a parking lot and floors:

```sql
INSERT INTO parking_lot (id, name, address) VALUES ('PR1234', 'Main City Parking', 'Downtown Road');
INSERT INTO floor (floor_number, parking_lot_id) VALUES (1, 'PR1234'), (2, 'PR1234');
```

- Add slots (example pattern):

```sql
INSERT INTO slot (slot_number, floor_id, vehicle_type_id)
VALUES
  (1, 1, (SELECT id FROM vehicle_type WHERE type_name='TRUCK')),
  (2, 1, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (3, 1, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
  (4, 1, (SELECT id FROM vehicle_type WHERE type_name='CAR'));
```

### 5) Notes: normalization, indexing & extensions

- Schema is normalized to 3NF; master data is isolated (`vehicle_type`).
- Indexes: composite index on `slot` and index on `ticket(is_active)` help allocation and lookup.
- Concurrency: use transactions + `SELECT ... FOR UPDATE` or DB locks to avoid race conditions when allocating slots.
- Extensions: payments, rate cards, reservations, user accounts and audit logs are straightforward to add.

---

I created a companion SQL file `parking_lot_schema.sql` in the project root with the full DDL + sample inserts and comments. Would you like me to:

- Generate an ER diagram image (PNG/SVG)?
- Generate a single importable SQL file tuned for MySQL or for PostgreSQL specifically (I can add engine/charset or serial sequences accordingly)?
- Add a small Java DB helper class (JDBC) demonstrating the transaction flow for park/unpark using this schema?

Tell me which next action you want and I will implement it.

### 3) Production-ready SQL (MySQL / PostgreSQL compatible)

Below SQL is compatible with both MySQL (5.7+) and PostgreSQL with minimal tweaks; where relevant I note differences.


-- parking_lot_schema.sql (created alongside this README)

-- VEHICLE TYPE (master data)
```
CREATE TABLE vehicle_type (
	id SERIAL PRIMARY KEY,
	type_name VARCHAR(30) UNIQUE NOT NULL
);
```

-- PARKING LOT
CREATE TABLE parking_lot (
	id VARCHAR(50) PRIMARY KEY,            -- e.g., PR1234
	name VARCHAR(200),
	address VARCHAR(500)
);

-- FLOOR
CREATE TABLE floor (
	id SERIAL PRIMARY KEY,
	floor_number INT NOT NULL,
	parking_lot_id VARCHAR(50) NOT NULL,
	CONSTRAINT fk_floor_parkinglot FOREIGN KEY (parking_lot_id)
		REFERENCES parking_lot(id) ON DELETE CASCADE
);

-- SLOT
CREATE TABLE slot (
	id SERIAL PRIMARY KEY,
	slot_number INT NOT NULL,
	floor_id INT NOT NULL,
	vehicle_type_id INT NOT NULL,
	is_occupied BOOLEAN DEFAULT FALSE,
	CONSTRAINT fk_slot_floor FOREIGN KEY (floor_id) REFERENCES floor(id) ON DELETE CASCADE,
	CONSTRAINT fk_slot_vehicletype FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(id)
);

CREATE INDEX idx_slot_floor_vehicle ON slot(floor_id, vehicle_type_id, is_occupied, slot_number);

-- VEHICLE
CREATE TABLE vehicle (
	id SERIAL PRIMARY KEY,
	registration_no VARCHAR(50) UNIQUE NOT NULL,
	color VARCHAR(50),
	vehicle_type_id INT NOT NULL,
	CONSTRAINT fk_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(id)
);

CREATE INDEX idx_vehicle_reg ON vehicle(registration_no);

-- TICKET
-- id uses the logical ticket format: <parking_lot_id>_<floor_number>_<slot_number>
CREATE TABLE ticket (
	id VARCHAR(100) PRIMARY KEY,
	parking_lot_id VARCHAR(50) NOT NULL,
	slot_id INT NOT NULL,
	vehicle_id INT NOT NULL,
	issue_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	exit_time TIMESTAMP WITH TIME ZONE NULL,
	is_active BOOLEAN DEFAULT TRUE,
	CONSTRAINT fk_ticket_slot FOREIGN KEY (slot_id) REFERENCES slot(id),
	CONSTRAINT fk_ticket_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
	CONSTRAINT fk_ticket_parkinglot FOREIGN KEY (parking_lot_id) REFERENCES parking_lot(id)
);

CREATE INDEX idx_ticket_active ON ticket(is_active);

-- Optional: payment and audit tables are included in the separate SQL file

### 4) Example inserts & sample data

-- add vehicle categories
INSERT INTO vehicle_type (type_name) VALUES ('CAR'), ('BIKE'), ('TRUCK');

-- add a parking lot
INSERT INTO parking_lot (id, name, address) VALUES ('PR1234', 'Main City Parking', 'Downtown Road');

-- add floors
INSERT INTO floor (floor_number, parking_lot_id) VALUES (1, 'PR1234'), (2, 'PR1234');

-- NOTE: adjust the floor_id values returned by your DB (SERIAL) when inserting slots; below assumes floor id 1 and 2 were created
-- add slots (example layout)
INSERT INTO slot (slot_number, floor_id, vehicle_type_id) VALUES
(1, 1, (SELECT id FROM vehicle_type WHERE type_name='TRUCK')),
(2, 1, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
(3, 1, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
(4, 1, (SELECT id FROM vehicle_type WHERE type_name='CAR')),
(5, 1, (SELECT id FROM vehicle_type WHERE type_name='CAR')),
(6, 1, (SELECT id FROM vehicle_type WHERE type_name='CAR')),
(1, 2, (SELECT id FROM vehicle_type WHERE type_name='TRUCK')),
(2, 2, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
(3, 2, (SELECT id FROM vehicle_type WHERE type_name='BIKE')),
(4, 2, (SELECT id FROM vehicle_type WHERE type_name='CAR')),
(5, 2, (SELECT id FROM vehicle_type WHERE type_name='CAR')),
(6, 2, (SELECT id FROM vehicle_type WHERE type_name='CAR'));

-- add a vehicle
INSERT INTO vehicle (registration_no, color, vehicle_type_id)
VALUES ('KA-01-HH-1234', 'White', (SELECT id FROM vehicle_type WHERE type_name='CAR'));

### 5) Parking operation (example SQL flow)

-- 1) Find first free slot for the vehicle type (use transaction or SELECT ... FOR UPDATE in high-concurrency setup)
-- replace 'CAR' and 'PR1234' with parameters
SELECT s.id AS slot_id, f.floor_number, s.slot_number
FROM slot s
JOIN floor f ON s.floor_id = f.id
JOIN vehicle_type vt ON s.vehicle_type_id = vt.id
WHERE vt.type_name = 'CAR' AND s.is_occupied = FALSE
ORDER BY f.floor_number, s.slot_number
LIMIT 1;

-- 2) Create or find vehicle record
-- INSERT INTO vehicle ... ON CONFLICT/ON DUPLICATE KEY handling differs between Postgres and MySQL

-- 3) Create ticket and mark slot occupied (wrap in transaction)
BEGIN;
-- assume found slot_id = :slot_id, floor_number=:floor and slot_number=:slot_number
INSERT INTO ticket (id, parking_lot_id, slot_id, vehicle_id) VALUES ('PR1234_1_4', 'PR1234', :slot_id, :vehicle_id);
UPDATE slot SET is_occupied = TRUE WHERE id = :slot_id;
COMMIT;

### 6) Unpark operation (example)

-- Mark ticket inactive and free slot
BEGIN;
UPDATE ticket SET exit_time = CURRENT_TIMESTAMP, is_active = FALSE WHERE id = 'PR1234_1_4' AND is_active = TRUE;
UPDATE slot SET is_occupied = FALSE WHERE id = :slot_id;
COMMIT;

### 7) Normalization & constraints notes

- The schema is normalized to 3NF: master data (vehicle_type) is stored separately; slot references floor and vehicle_type; ticket references slot and vehicle.
- Use transactions on park/unpark to ensure slot occupancy consistency (avoid race conditions).
- Add UNIQUE constraints where applicable (e.g., `vehicle.registration_no`).
- Consider adding check constraints for slot_number/floor_number ranges if your application enforces them.

### 8) Indexing & performance

- Index on `slot(floor_id, vehicle_type_id, is_occupied, slot_number)` helps the allocation query.
- Index on `ticket(is_active)` helps finding active tickets for reports and unpark operations.
- For high throughput, use SELECT ... FOR UPDATE or application-level locking when selecting free slots.

### 9) Extension ideas (DB-level)

- payment table (ticket_id -> amount, paid_at, payment_method)
- rate_card table (vehicle_type_id, base_rate, per_hour_rate)
- user_account table and mapping tickets to users
- reservation table for pre-booked slots
- audit_log table capturing DML changes (slot occupancy and ticket changes)

---

I created a companion SQL file `parking_lot_schema.sql` in the project root with the full DDL + sample inserts and comments. Would you like me to:

- Generate an ER diagram image (PNG/SVG)?
- Generate a single importable SQL file tuned for MySQL or for PostgreSQL specifically (I can add engine/charset or serial sequences accordingly)?
- Add a small Java DB helper class (JDBC) demonstrating the transaction flow for park/unpark using this schema?

Tell me which next action you want and I will implement it.

