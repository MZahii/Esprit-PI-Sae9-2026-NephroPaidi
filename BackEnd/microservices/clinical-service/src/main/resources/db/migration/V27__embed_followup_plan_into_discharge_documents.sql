-- V27: Embed follow-up plan columns into discharge_documents to match current entity mapping

ALTER TABLE discharge_documents
    ADD COLUMN IF NOT EXISTS followup_general_practitioner TEXT,
    ADD COLUMN IF NOT EXISTS followup_timeline TEXT,
    ADD COLUMN IF NOT EXISTS followup_objectives TEXT,
    ADD COLUMN IF NOT EXISTS followup_specialist_referrals TEXT,
    ADD COLUMN IF NOT EXISTS followup_additional_notes TEXT;

UPDATE discharge_documents d
SET
    followup_general_practitioner = COALESCE(d.followup_general_practitioner, f.general_practitioner_name),
    followup_timeline = COALESCE(d.followup_timeline, f.followup_timeline),
    followup_objectives = COALESCE(d.followup_objectives, f.followup_objectives),
    followup_specialist_referrals = COALESCE(d.followup_specialist_referrals, f.specialist_referrals),
    followup_additional_notes = COALESCE(d.followup_additional_notes, f.additional_notes)
FROM follow_up_plans f
WHERE f.discharge_id = d.id;
