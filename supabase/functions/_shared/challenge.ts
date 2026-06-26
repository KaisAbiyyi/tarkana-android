import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3';

export const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

export function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
  });
}

export function adminClient() {
  return createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
  );
}

export async function requireUser(req: Request) {
  const authHeader = req.headers.get('Authorization');
  const token = authHeader?.replace('Bearer ', '') ?? '';
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_ANON_KEY') ?? '',
    { global: { headers: { Authorization: authHeader ?? '' } } },
  );
  const { data: { user }, error } = await supabase.auth.getUser(token);
  if (error || !user) return null;
  return user;
}

export async function ensureProfile(supabase: ReturnType<typeof createClient>, user: any) {
  const { data } = await supabase.from('users_profile').select('*').eq('id', user.id).single();
  if (data) return data;
  const { data: profile, error } = await supabase.from('users_profile').insert({
    id: user.id,
    display_name: user.user_metadata?.display_name || user.email?.split('@')[0] || 'Player',
    rating: 0,
    rank: 'Bronze Mind',
  }).select('*').single();
  if (error) throw error;
  return profile;
}

export function rankFor(rating: number) {
  if (rating >= 3000) return 'Mastermind';
  if (rating >= 2400) return 'Diamond Reasoner';
  if (rating >= 1800) return 'Platinum Strategist';
  if (rating >= 1200) return 'Gold Analyst';
  if (rating >= 500) return 'Silver Solver';
  return 'Bronze Mind';
}

export function rankProgress(rating: number) {
  const bands = [
    ['Bronze Mind', 0],
    ['Silver Solver', 500],
    ['Gold Analyst', 1200],
    ['Platinum Strategist', 1800],
    ['Diamond Reasoner', 2400],
    ['Mastermind', 3000],
  ] as const;
  const idx = Math.max(0, bands.findIndex(([, min], i) => rating < (bands[i + 1]?.[1] ?? Infinity)));
  const current = bands[idx];
  const next = bands[idx + 1];
  if (!next) return { currentRank: current[0], nextRank: current[0], pointsToNextRank: 0, progressPercent: 1 };
  return {
    currentRank: current[0],
    nextRank: next[0],
    pointsToNextRank: next[1] - rating,
    progressPercent: Math.max(0, Math.min(1, (rating - current[1]) / (next[1] - current[1]))),
  };
}

const categories: Record<string, string> = {
  number_sequence: '10000000-0000-4000-8000-000000000001',
  symbol_pattern: '10000000-0000-4000-8000-000000000002',
  mini_deduction: '10000000-0000-4000-8000-000000000003',
  memory_pattern: '10000000-0000-4000-8000-000000000004',
};

function rng(seed: number) {
  return () => {
    seed = (seed * 1664525 + 1013904223) >>> 0;
    return seed / 4294967296;
  };
}

function shuffle<T>(items: T[], rand: () => number) {
  for (let i = items.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [items[i], items[j]] = [items[j], items[i]];
  }
  return items;
}

function questionType(selectedMode: string | null, i: number) {
  if (selectedMode === 'symbol_pattern' || selectedMode === 'mini_deduction' || selectedMode === 'memory_pattern') return selectedMode;
  if (selectedMode === 'number_sequence') return 'number_sequence';
  return ['number_sequence', 'symbol_pattern', 'mini_deduction', 'memory_pattern'][i % 4];
}

export function buildQuestions(sessionId: string, count: number, selectedMode: string | null) {
  const now = Date.now();
  return Array.from({ length: count }, (_, i) => {
    const type = questionType(selectedMode, i);
    const rand = rng(now + i * 97);
    const generated_seed = `${now}-${i}`;
    if (type === 'symbol_pattern') {
      const symbols = ['circle', 'star', 'diamond', 'square'];
      const start = Math.floor(rand() * symbols.length);
      const seq = Array.from({ length: 5 }, (_, n) => symbols[(start + n) % symbols.length]);
      const answer = symbols[(start + 5) % symbols.length];
      return {
        session_id: sessionId,
        question_type: type,
        category_id: categories[type],
        prompt: `${seq.join(' | ')} | ?`,
        choices: shuffle([...symbols], rand),
        correct_answer: answer,
        explanation: 'The symbols repeat in the same order.',
        difficulty_score: 2,
        time_limit_seconds: 30,
        metadata: {},
        generated_seed,
        order_index: i,
      };
    }
    if (type === 'mini_deduction') {
      const answer = ['Ari', 'Bima', 'Citra', 'Dina'][Math.floor(rand() * 4)];
      return {
        session_id: sessionId,
        question_type: type,
        category_id: categories[type],
        prompt: `${answer} is the only person who has both clues. Who matches the clues?`,
        choices: shuffle(['Ari', 'Bima', 'Citra', 'Dina'], rand),
        correct_answer: answer,
        explanation: `${answer} satisfies both clues.`,
        difficulty_score: 2,
        time_limit_seconds: 40,
        metadata: {},
        generated_seed,
        order_index: i,
      };
    }
    if (type === 'memory_pattern') {
      const symbols = ['circle', 'star', 'diamond', 'square'];
      const seq = Array.from({ length: 5 }, () => symbols[Math.floor(rand() * symbols.length)]);
      const answer = seq.join(',');
      const choices = shuffle([answer, seq.slice().reverse().join(','), shuffle([...seq], rand).join(','), seq.slice(1).concat(seq[0]).join(',')], rand);
      return {
        session_id: sessionId,
        question_type: type,
        category_id: categories[type],
        prompt: 'Which sequence did you see?',
        choices,
        correct_answer: answer,
        explanation: 'This matches the sequence shown during memorization.',
        difficulty_score: 2,
        time_limit_seconds: 30,
        metadata: { memorize: seq, revealSeconds: 4 },
        generated_seed,
        order_index: i,
      };
    }
    const start = 2 + Math.floor(rand() * 8);
    const step = 2 + Math.floor(rand() * 6);
    const seq = Array.from({ length: 5 }, (_, n) => start + n * step);
    const answer = String(start + 5 * step);
    return {
      session_id: sessionId,
      question_type: 'number_sequence',
      category_id: categories.number_sequence,
      prompt: `${seq.join(', ')}, ?`,
      choices: shuffle([answer, String(Number(answer) + step), String(Number(answer) - step), String(Number(answer) + 1)], rand),
      correct_answer: answer,
      explanation: `Each number increases by ${step}.`,
      difficulty_score: 1,
      time_limit_seconds: 30,
      metadata: {},
      generated_seed,
      order_index: i,
    };
  });
}

export function publicQuestion(q: any) {
  return {
    id: q.id,
    questionType: q.question_type,
    prompt: q.prompt,
    choices: q.choices,
    correctAnswer: q.correct_answer,
    explanation: q.explanation,
    timeLimitSeconds: q.time_limit_seconds,
    metadata: q.metadata,
    orderIndex: q.order_index,
  };
}
