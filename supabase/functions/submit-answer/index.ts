import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { adminClient, corsHeaders, json, publicQuestion, requireUser } from '../_shared/challenge.ts';

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  try {
    const user = await requireUser(req);
    if (!user) return json({ error: 'Unauthorized' }, 401);

    const { sessionId, sessionQuestionId, selectedAnswer } = await req.json();
    const supabase = adminClient();
    const { data: session, error: sessionError } = await supabase
      .from('challenge_sessions')
      .select('*')
      .eq('id', sessionId)
      .eq('user_id', user.id)
      .single();
    if (sessionError || !session) return json({ error: 'Session not found' }, 404);

    const { data: question, error: questionError } = await supabase
      .from('session_questions')
      .select('*')
      .eq('id', sessionQuestionId)
      .eq('session_id', sessionId)
      .single();
    if (questionError || !question) return json({ error: 'Question not found' }, 404);

    const { data: previous } = await supabase
      .from('session_answers')
      .select('*')
      .eq('session_question_id', sessionQuestionId)
      .eq('user_id', user.id)
      .maybeSingle();
    if (previous) return json({ error: 'Already answered' }, 409);

    const isCorrect = selectedAnswer === question.correct_answer;
    const scoreEarned = isCorrect ? Math.max(10, question.difficulty_score * 10) : 0;
    const { error: answerError } = await supabase.from('session_answers').insert({
      session_question_id: sessionQuestionId,
      user_id: user.id,
      selected_answer: selectedAnswer || '',
      is_correct: isCorrect,
      time_spent_seconds: 0,
      score_earned: scoreEarned,
    });
    if (answerError) throw answerError;

    const { data: nextQuestion } = await supabase
      .from('session_questions')
      .select('*')
      .eq('session_id', sessionId)
      .eq('order_index', question.order_index + 1)
      .maybeSingle();

    return json({
      isCorrect,
      scoreEarned,
      isComplete: !nextQuestion,
      nextQuestion: nextQuestion ? publicQuestion(nextQuestion) : null,
    });
  } catch (err) {
    return json({ error: err.message }, 400);
  }
});
