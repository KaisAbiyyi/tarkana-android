import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { adminClient, corsHeaders, ensureProfile, json, rankFor, rankProgress, requireUser } from '../_shared/challenge.ts';

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  try {
    const user = await requireUser(req);
    if (!user) return json({ error: 'Unauthorized' }, 401);

    const { sessionId } = await req.json();
    const supabase = adminClient();
    const profile = await ensureProfile(supabase, user);
    const { data: session, error: sessionError } = await supabase
      .from('challenge_sessions')
      .select('*')
      .eq('id', sessionId)
      .eq('user_id', user.id)
      .single();
    if (sessionError || !session) return json({ error: 'Session not found' }, 404);

    const { data: answers, error: answerError } = await supabase
      .from('session_answers')
      .select('*')
      .eq('user_id', user.id)
      .in('session_question_id',
        (await supabase.from('session_questions').select('id').eq('session_id', sessionId)).data?.map((q: any) => q.id) ?? []);
    if (answerError) throw answerError;

    const totalScore = (answers ?? []).reduce((sum: number, a: any) => sum + (a.score_earned ?? 0), 0);
    const correctAnswers = (answers ?? []).filter((a: any) => a.is_correct).length;
    const accuracy = session.total_questions ? (correctAnswers / session.total_questions) * 100 : 0;
    const ratingDelta = Math.round((accuracy - 50) / 5 + totalScore / 20);
    const ratingAfter = Math.max(0, (profile.rating ?? 0) + ratingDelta);
    const rankAfter = rankFor(ratingAfter);

    const { error: updateSessionError } = await supabase.from('challenge_sessions').update({
      status: 'completed',
      total_score: totalScore,
      accuracy,
      total_time_seconds: 0,
      average_time_seconds: 0,
      rating_before: profile.rating ?? 0,
      rating_after: ratingAfter,
      rating_delta: ratingDelta,
      rank_before: profile.rank ?? 'Bronze Mind',
      rank_after: rankAfter,
      completed_at: new Date().toISOString(),
    }).eq('id', sessionId).eq('user_id', user.id);
    if (updateSessionError) throw updateSessionError;

    await supabase.from('users_profile').update({ rating: ratingAfter, rank: rankAfter }).eq('id', user.id);

    return json({
      totalScore,
      accuracy,
      correctAnswers,
      wrongAnswers: Math.max(0, session.total_questions - correctAnswers),
      averageTimeSeconds: 0,
      ratingDelta,
      rankAfter,
      rankProgress: rankProgress(ratingAfter),
    });
  } catch (err) {
    return json({ error: err.message }, 400);
  }
});
