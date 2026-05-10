alter table follow_up_messages
    add column if not exists ai_urgency_level varchar(32),
    add column if not exists ai_confidence double precision,
    add column if not exists ai_triage_status varchar(32),
    add column if not exists ai_explanation varchar(1000),
    add column if not exists ai_model_version varchar(100),
    add column if not exists ai_evaluated_at timestamptz;

create index if not exists idx_follow_up_messages_ai_urgency_level
    on follow_up_messages (ai_urgency_level);

create index if not exists idx_follow_up_messages_ai_triage_status
    on follow_up_messages (ai_triage_status);
