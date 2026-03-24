# Password security and incident recovery

This document defines the supported password recovery flow after the production incident in which credentials were changed manually in the database.

## Supported flows

### 1. Authenticated self-service password change

- endpoint: `POST /api/v1/auth/change-password`
- actor: any authenticated user
- request:
  - `currentPassword`
  - `newPassword`
  - `newPasswordConfirmation`
- rules:
  - current password must match
  - new password confirmation must match
  - new password must differ from the current password

### 2. Controlled administrative password reset

- endpoint: `POST /api/v1/admin/users/password-reset`
- actor: `PLATFORM_ADMIN` only
- request:
  - `targetEmail`
  - `newPassword`
  - `newPasswordConfirmation`
- rules:
  - target user must exist and be active
  - `PLATFORM_ADMIN` targets are blocked from this flow
  - platform admins must use the authenticated self-service change flow for their own credentials

## Session invalidation model

After password change or reset:

- all active refresh tokens for the affected user are revoked
- access tokens issued before the password event become invalid because the backend compares token issue time with `credentials_updated_at`
- production may still rotate `APP_SECURITY_TOKEN_SECRET` during incident containment when a wider access-token kill switch is required

## Audit trail

The backend records password-sensitive events without logging passwords, secrets or hashes:

- `auth_password_change_success`
- `auth_password_change_rejected`
- `auth_password_reset_success`
- `auth_password_reset_rejected`

Refresh-token revocation reasons also become explicit:

- `PASSWORD_CHANGED`
- `PASSWORD_RESET`

## Minimal product UI delivered in this repository

The Flutter source repository is not present here, so the incident-safe UI shipped from this repo is a same-origin static console:

- path: `/password-console.html`

It supports:

- login against the same production API
- authenticated self password change
- platform-admin password reset for a specific user e-mail
- local logout and session cleanup

## Operational rule from now on

Do not perform routine password resets directly in the production database.

Allowed operational order:

1. user uses authenticated self-service change whenever possible
2. `PLATFORM_ADMIN` uses controlled reset when the user is locked out
3. database intervention is reserved for incident containment only
4. if a credential or bearer compromise is suspected, revoke refresh tokens and rotate `APP_SECURITY_TOKEN_SECRET`
