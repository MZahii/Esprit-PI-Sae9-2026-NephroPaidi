create table if not exists staff_conversations (
    id uuid primary key,
    type varchar(32) not null,
    title varchar(255),
    direct_conversation_key varchar(512),
    created_by_user_id varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_message_at timestamptz
);

create unique index if not exists uq_staff_conversations_direct_key
    on staff_conversations (direct_conversation_key)
    where direct_conversation_key is not null;

create index if not exists idx_staff_conversations_last_message_at
    on staff_conversations (last_message_at desc nulls last);

create table if not exists staff_conversation_participants (
    id uuid primary key,
    conversation_id uuid not null references staff_conversations (id) on delete cascade,
    user_id varchar(255) not null,
    user_role varchar(64) not null,
    display_name varchar(255),
    joined_at timestamptz not null,
    last_read_at timestamptz,
    is_archived boolean not null default false,
    is_muted boolean not null default false
);

create unique index if not exists uq_staff_conversation_participants_conversation_user
    on staff_conversation_participants (conversation_id, user_id);

create index if not exists idx_staff_conversation_participants_user
    on staff_conversation_participants (user_id, is_archived);

create table if not exists staff_messages (
    id uuid primary key,
    conversation_id uuid not null references staff_conversations (id) on delete cascade,
    sender_id varchar(255) not null,
    sender_role varchar(64) not null,
    sender_display_name varchar(255) not null,
    content varchar(4000) not null,
    message_type varchar(32) not null,
    created_at timestamptz not null,
    edited_at timestamptz,
    deleted_at timestamptz
);

create index if not exists idx_staff_messages_conversation_created_at
    on staff_messages (conversation_id, created_at asc);

create index if not exists idx_staff_messages_sender
    on staff_messages (sender_id, created_at desc);
