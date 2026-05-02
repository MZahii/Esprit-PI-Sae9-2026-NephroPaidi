create table if not exists patient_dossier (
    id uuid primary key,
    patient_id bigint not null,
    source_consultation_id uuid not null,
    source_appointment_id uuid not null,
    hospitalization_request_id uuid not null unique,
    primary_doctor_id uuid not null,
    assigned_nurse_id uuid,
    admission_reason text not null,
    admission_priority varchar(20) not null,
    status varchar(24) not null,
    admitted_at timestamp not null,
    expected_discharge_at timestamp,
    discharged_at timestamp,
    discharge_summary text,
    archived_to_history boolean not null default false,
    archived_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    version bigint not null,
    constraint chk_patient_dossier_status check (status in ('ACTIVE', 'IN_PROGRESS', 'READY_FOR_DISCHARGE', 'DISCHARGED', 'ARCHIVED')),
    constraint chk_patient_dossier_priority check (admission_priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    constraint chk_patient_dossier_discharge_after_admit check (discharged_at is null or discharged_at >= admitted_at),
    constraint chk_patient_dossier_archive_after_discharge check (archived_at is null or discharged_at is null or archived_at >= discharged_at)
);

create index if not exists idx_dossier_patient on patient_dossier (patient_id);
create index if not exists idx_dossier_nurse_status on patient_dossier (assigned_nurse_id, status);
create index if not exists idx_dossier_consultation on patient_dossier (source_consultation_id);
create index if not exists idx_dossier_created on patient_dossier (created_at desc);

create table if not exists dossier_entry (
    id uuid primary key,
    dossier_id uuid not null references patient_dossier(id),
    entry_type varchar(24) not null,
    actor_role varchar(16) not null,
    actor_id uuid not null,
    actor_display_name varchar(120) not null,
    title varchar(180) not null,
    details text,
    medication_id uuid,
    medication_name varchar(180),
    dose_value numeric(10,2),
    dose_unit varchar(24),
    route varchar(32),
    patient_condition varchar(64),
    vitals_json text,
    occurred_at timestamp not null,
    requires_signature boolean not null default true,
    signed boolean not null default false,
    signed_at timestamp,
    signature_type varchar(20),
    signature_hash varchar(128),
    previous_entry_id uuid,
    created_at timestamp not null,
    updated_at timestamp not null,
    version bigint not null,
    constraint chk_dossier_entry_type check (entry_type in ('MEDICATION_ADMIN', 'CONDITION_UPDATE', 'CARE_NOTE', 'TRANSFER', 'ALERT', 'DISCHARGE_NOTE')),
    constraint chk_dossier_entry_actor_role check (actor_role in ('NURSE', 'DOCTOR', 'SYSTEM')),
    constraint chk_dossier_entry_signature_type check (signature_type is null or signature_type in ('DRAWN', 'TYPED', 'PKI')),
    constraint chk_dossier_entry_signature_coherence check (
        (signed = false and signed_at is null and signature_type is null and signature_hash is null)
        or
        (signed = true and signed_at is not null and signature_type is not null and signature_hash is not null)
    )
);

create index if not exists idx_entry_dossier_occurred on dossier_entry (dossier_id, occurred_at desc);
create index if not exists idx_entry_actor on dossier_entry (actor_id, occurred_at desc);
create index if not exists idx_entry_unsigned on dossier_entry (dossier_id, signed);

create table if not exists dossier_contributor (
    id uuid primary key,
    dossier_id uuid not null references patient_dossier(id),
    contributor_id uuid not null,
    contributor_role varchar(16) not null,
    active boolean not null default true,
    added_at timestamp not null,
    removed_at timestamp,
    added_by uuid not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    version bigint not null,
    constraint chk_dossier_contributor_role check (contributor_role in ('DOCTOR', 'NURSE')),
    constraint chk_dossier_contributor_removed_after_added check (removed_at is null or removed_at >= added_at),
    unique (dossier_id, contributor_id, contributor_role, active)
);

create index if not exists idx_contributor_dossier on dossier_contributor (dossier_id, active);
create index if not exists idx_contributor_user on dossier_contributor (contributor_id, active);
