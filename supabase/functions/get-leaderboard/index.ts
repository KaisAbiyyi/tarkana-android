import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

serve(async (req) => {
	if (req.method === 'OPTIONS') {
		return new Response('ok', {
			headers: {
				'Access-Control-Allow-Origin': '*',
				'Access-Control-Allow-Methods': 'GET, OPTIONS',
				'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
			}
		});
	}

	try {
		const authHeader = req.headers.get('Authorization');
		if (!authHeader) {
			return new Response(JSON.stringify({ error: 'Missing Authorization header' }), {
				status: 401,
				headers: { 'Content-Type': 'application/json' }
			});
		}
		const token = authHeader.replace('Bearer ', '');

		const supabaseClient = createClient(
			Deno.env.get('SUPABASE_URL') ?? '',
			Deno.env.get('SUPABASE_ANON_KEY') ?? '',
			{ global: { headers: { Authorization: authHeader } } }
		);

		const { data: { user }, error: userError } = await supabaseClient.auth.getUser(token);
		if (userError || !user) {
			return new Response(JSON.stringify({ error: 'Unauthorized', details: userError }), {
				status: 401,
				headers: { 'Content-Type': 'application/json' }
			});
		}

		const supabaseAdmin = createClient(
			Deno.env.get('SUPABASE_URL') ?? '',
			Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
		);

		// Get top 50 users by rating
		const { data: topUsers, error: usersError } = await supabaseAdmin
			.from('users_profile')
			.select('id, display_name, rank, rating')
			.order('rating', { ascending: false });

		if (usersError) throw new Error(usersError.message);

		// Get challenge sessions for these users
		const userIds = topUsers.map((u: any) => u.id);
		let sessions: any[] = [];
		if (userIds.length > 0) {
			const { data: sess, error: sessionsError } = await supabaseAdmin
				.from('challenge_sessions')
				.select('user_id, accuracy, status')
				.in('user_id', userIds)
				.eq('status', 'completed');
			if (sessionsError) throw new Error(sessionsError.message);
			sessions = sess || [];
		}

		// Aggregate stats per user
		const statsByUser: Record<string, { totalAcc: number, count: number }> = {};
		userIds.forEach((id: string) => statsByUser[id] = { totalAcc: 0, count: 0 });
		
		sessions?.forEach((s: any) => {
			if (statsByUser[s.user_id]) {
				statsByUser[s.user_id].totalAcc += (s.accuracy || 0);
				statsByUser[s.user_id].count += 1;
			}
		});

		const leaderboard = topUsers.map((u: any, index: number) => {
			const stats = statsByUser[u.id];
			const avgAcc = stats.count > 0 ? (stats.totalAcc / stats.count).toFixed(1) + '%' : '0.0%';
			return {
				position: index + 1,
				playerName: u.display_name || 'Player',
				rank: u.rank || 'BRONZE',
				logicRating: u.rating || 0,
				accuracy: avgAcc,
				completedRounds: stats.count,
				isCurrentUser: u.id === user.id
			};
		});

		return new Response(
			JSON.stringify({ leaderboard }),
			{ headers: { 'Content-Type': 'application/json' }, status: 200 }
		);
	} catch (err) {
		return new Response(JSON.stringify({ error: err.message }), {
			headers: { 'Content-Type': 'application/json' },
			status: 500,
		});
	}
});
