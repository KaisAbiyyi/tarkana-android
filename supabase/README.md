Canonical Supabase Edge Functions live in `../tarkana/supabase/functions`.

The Android app must call deployed Edge Functions over HTTPS and must not carry
challenge generation, answer validation, scoring, or rating logic in this
project. Keeping server functions out of the Android project avoids accidental
deployment of stale mobile-side challenge logic and keeps reverse engineering of
the APK from exposing game rules or answer keys.
