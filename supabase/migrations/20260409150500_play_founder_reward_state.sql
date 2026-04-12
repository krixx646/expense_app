create table if not exists public.play_founder_reward_state (
  firebase_uid text primary key,
  product_id text not null,
  package_name text not null,
  purchase_token text not null,
  purchase_token_hash text not null unique,
  order_id text,
  purchase_time bigint not null,
  acknowledged_at bigint,
  has_founder_reward boolean not null default true,
  founder_badge_granted boolean not null default true,
  founder_trial_activated boolean not null default false,
  founder_trial_started_at bigint not null default 0,
  founder_trial_expires_at bigint not null default 0,
  source text not null,
  updated_at bigint not null
);

create unique index if not exists idx_play_founder_reward_state_purchase_token_hash
  on public.play_founder_reward_state (purchase_token_hash);

alter table public.play_founder_reward_state enable row level security;
