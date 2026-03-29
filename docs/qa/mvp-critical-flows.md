# MVP Critical Flows

## 1. Login and session
- Login valid and invalid.
- Session refresh with active session.
- Expired session returns user to login.
- Protected routes require authentication.

## 2. Tour and onboarding
- First access opens the onboarding/tour.
- Tour can be completed.
- Tour can be reopened manually after completion.
- OWNER and MEMBER paths stay role-aware.

## 3. Dashboard
- OWNER dashboard loads summary data without 500.
- MEMBER dashboard shows the correct reduced surface.
- Home offers clear actions to continue using the product.
- Copy and layout stay readable on web and mobile widths.

## 4. Expense lifecycle
- Create an expense from `/expenses/new`.
- After success, the expense remains visible where expected.
- Open detail, edit, delete, and return without being trapped.
- Payment flow supports total and partial payment.

## 5. Operational data surfaces
- Space references can be listed, filtered, and created without breaking dashboard loading.
- Fixed bills, history import, and reports preserve shell continuity.
- Repeated refresh or deep-link entry does not strand the user.

## 6. Navigation guarantees
- Every critical screen has a visible exit.
- Browser back/forward is not the only way out.
- After submit success, the next action is obvious and usable.

