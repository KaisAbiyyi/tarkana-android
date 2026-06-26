import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const supabaseAdmin = createClient(
    'https://nlhqorniufcdbyzuoqgq.supabase.co',
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5saHFvcm5pdWZjZGJ5enVvcWdxIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTcxOTIzNDg4OCwiZXhwIjoyMDM0ODEwODg4fQ.---'
);
// wait, I don't have the service_role_key. I need to get it from .env of web project
