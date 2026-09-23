-- Runs once on first boot of the compose db (empty volume).
-- DATABASE PER SERVICE: four isolated databases in one Postgres container.
-- One container is a DEMO economy — production gives each service its own
-- instance/cluster (that isolation is the point of the pattern), but the
-- LOGICAL boundary is identical: a service only ever connects to its own db.
CREATE DATABASE authdb;          -- auth-service:      app_user
CREATE DATABASE accountdb;       -- account-service:   customer, account, transactions
CREATE DATABASE paymentdb;       -- payment-service:   transfer (the saga state machine)
CREATE DATABASE loandb;          -- loan-service:      loan
-- notification-service owns NO database (in-memory store) — a service's data
-- footprint is a design choice, not a requirement.
-- Tables are created by Hibernate (ddl-auto=update via config-repo); in
-- production each service would run its own Flyway migrations at boot.
