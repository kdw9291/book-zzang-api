alter table public.app_users
  add column if not exists age_group smallint;

alter table public.app_users
  add constraint app_users_age_group_check
  check (age_group is null or (age_group between 10 and 90 and age_group % 10 = 0));

comment on column public.app_users.display_name_ciphertext is 'AES-256-GCM으로 암호화한 사용자 닉네임';
comment on column public.app_users.gender is '선택 성별. 미선택은 PREFER_NOT_TO_SAY';
comment on column public.app_users.age_group is '선택 연령대. 10부터 90까지 10 단위, 미선택은 NULL';
comment on table public.email_verifications is '현재 미사용. 이메일 인증 재도입 또는 롤백을 위해 보존한 인증 이력';
