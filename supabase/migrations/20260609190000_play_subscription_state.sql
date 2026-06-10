create table if not exists public.play_subscription_state (
  firebase_uid text primary key,
  product_id text not null,
  package_name text not null,
  purchase_token text not null,
  purchase_token_hash text not null unique,
  order_id text,
  purchase_time bigint not null,
  expiry_time bigint not null,
  is_auto_renewing boolean not null,
  acknowledged_at bigint,
  is_premium boolean not null,
  source text not null,
  updated_at bigint not null
);

create unique index if not exists idx_play_subscription_state_purchase_token_hash
  on public.play_subscription_state (purchase_token_hash);

alter table public.play_subscription_state enable row level security;
