import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { adminClient, buildQuestions, corsHeaders, ensureProfile, json, publicQuestion, requireUser } from '../_shared/challenge.ts';

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  try {
    const user = await requireUser(req);
    if (!user) return json({ error: 'Unauthorized' }, 401);

    const body = await req.json().catch(() => ({}));
    const challengeType = ['quick', 'standard', 'long'].includes(body.challengeType) ? body.challengeType : 'standard';
    const selectedMode = body.selectedMode || null;
    const count = challengeType === 'quick' ? 5 : challengeType === 'long' ? 20 : 10;
    const supabase = adminClient();
    const profile = await ensureProfile(supabase, user);

    const { data: session, error: sessionError } = await supabase.from('challenge_sessions').insert({
      user_id: user.id,
      challenge_type: challengeType,
      status: 'in_progress',
      total_questions: count,
      rating_before: profile.rating ?? 0,
      rating_after: profile.rating ?? 0,
      rank_before: profile.rank ?? 'Bronze Mind',
      rank_after: profile.rank ?? 'Bronze Mind',
    }).select('*').single();
    if (sessionError) throw sessionError;

    const { data: questions, error: questionsError } = await supabase
      .from('session_questions')
      .insert(buildQuestions(session.id, count, selectedMode))
      .select('*')
      .order('order_index');
    if (questionsError) throw questionsError;

    return json({ sessionId: session.id, totalQuestions: count, currentQuestion: publicQuestion(questions[0]) });
  } catch (err) {
    return json({ error: err.message }, 400);
  }
});
