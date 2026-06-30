create table suggestion (
    id              uuid primary key,
    tenant_id       varchar(255) not null,
    encounter_id    varchar(255) not null,
    assistant_type  varchar(32)  not null,
    prompt_redacted text         not null,
    model_provider  varchar(64)  not null,
    model_output    text         not null,
    tokens_used     integer      not null,
    status          varchar(16)  not null,
    requested_by    varchar(255) not null,
    decided_by      varchar(255),
    decision_reason text,
    created_at      timestamptz  not null,
    decided_at      timestamptz,
    version         bigint       not null default 0
);

create index idx_suggestion_tenant on suggestion (tenant_id, created_at desc);

create table grounding_reference (
    id            uuid primary key,
    suggestion_id uuid not null references suggestion (id) on delete cascade,
    source_type   varchar(32)  not null,
    source_id     varchar(255) not null,
    description   text
);

create index idx_grounding_suggestion on grounding_reference (suggestion_id);

create table audit_event (
    event_id      uuid primary key,
    event_type    varchar(64)  not null,
    tenant_id     varchar(255) not null,
    suggestion_id uuid         not null,
    actor         varchar(255) not null,
    details       text,
    occurred_at   timestamptz  not null,
    received_at   timestamptz  not null
);

create index idx_audit_suggestion on audit_event (suggestion_id, occurred_at);
