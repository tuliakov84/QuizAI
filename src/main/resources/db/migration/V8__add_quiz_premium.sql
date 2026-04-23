alter table users
  add column if not exists premium_until timestamp;
