-- Constraint 4.4: "A recorded fact is never updated or deleted."
--
-- We enforce this with a trigger rather than by revoking UPDATE/DELETE
-- grants from an application role. A role-based approach is stronger in
-- principle, but most free-tier Postgres offerings only ever give you one,
-- fully-privileged role, so REVOKE would just be revoking from ourselves
-- (and we could always re-grant it back in a later migration). A trigger
-- fires unconditionally for any role and can only be removed via an
-- explicit DDL migration that a reviewer would see in version control -
-- the application code itself has no path around it.

CREATE OR REPLACE FUNCTION forbid_update_or_delete() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'recorded facts are immutable: % on % is not permitted (id=%)',
        TG_OP, TG_TABLE_NAME, COALESCE(OLD.id, NULL);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sent_fact_immutable
    BEFORE UPDATE OR DELETE ON sent_fact
    FOR EACH ROW EXECUTE FUNCTION forbid_update_or_delete();

CREATE TRIGGER reported_fact_immutable
    BEFORE UPDATE OR DELETE ON reported_fact
    FOR EACH ROW EXECUTE FUNCTION forbid_update_or_delete();
