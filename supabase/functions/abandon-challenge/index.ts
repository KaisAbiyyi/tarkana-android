import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { adminClient, corsHeaders, json, requireUser } from '../_shared/challenge.ts';

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  try {
    const user = await requireUser(req);
    if (!user) return json({ error: 'Unauthorized' }, 401);
    const { sessionId } = await req.json();
    const { error } = await adminClient()
      .from('challenge_sessions')
      .update({ status: 'abandoned' })
      .eq('id', sessionId)
      .eq('user_id', user.id)
      .in('status', ['created', 'in_progress']);
    if (error) throw error;
    return json({ ok: true });
  } catch (err) {
    return json({ error: err.message }, 400);
  }
});
