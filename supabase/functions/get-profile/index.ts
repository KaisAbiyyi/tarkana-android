import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3';

const corsHeaders = {
	'Access-Control-Allow-Origin': '*',
	'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type'
};

serve(async (req) => {
	if (req.method === 'OPTIONS') {
		return new Response('ok', { headers: corsHeaders });
	}

	try {
		const supabaseClient = createClient(
			Deno.env.get('SUPABASE_URL') ?? '',
			Deno.env.get('SUPABASE_ANON_KEY') ?? '',
			{ global: { headers: { Authorization: req.headers.get('Authorization')! } } }
		);

		const authHeader = req.headers.get('Authorization');
		const token = authHeader ? authHeader.replace('Bearer ', '') : '';
		
		const {
			data: { user },
			error: userError
		} = await supabaseClient.auth.getUser(token);
		if (userError || !user) {
			return new Response(JSON.stringify({ error: 'Unauthorized', details: userError, receivedHeader: authHeader, extractedToken: token }), {
				status: 401,
				headers: { ...corsHeaders, 'Content-Type': 'application/json' }
			});
		}

		const supabaseAdmin = createClient(
			Deno.env.get('SUPABASE_URL') ?? '',
			Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
		);

		let profile;
		const { data: existingProfile, error } = await supabaseAdmin
			.from('users_profile')
			.select('id, display_name, rank, rating, created_at')
			.eq('id', user.id)
			.single();

		if (error && error.code === 'PGRST116') {
			// Profile not found, let's create it automatically
			const { data: newProfile, error: insertError } = await supabaseAdmin
				.from('users_profile')
				.insert([{
					id: user.id,
					display_name: user.user_metadata?.display_name || 'Player',
					rating: 0
				}])
				.select('id, display_name, rank, rating, created_at')
				.single();

			if (insertError) throw new Error('Failed to create profile: ' + insertError.message);
			profile = newProfile;
		} else if (error || !existingProfile) {
			throw new Error('Profile not found: ' + (error ? error.message : 'Unknown'));
		} else {
			profile = existingProfile;
		}

		return new Response(
			JSON.stringify({
				id: profile.id,
				email: user.email,
				displayName: profile.display_name,
				rank: profile.rank,
				rating: profile.rating,
				createdAt: profile.created_at
			}),
			{ headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
		);
	} catch (err: any) {
		return new Response(JSON.stringify({ error: err.message }), {
			status: 400,
			headers: { ...corsHeaders, 'Content-Type': 'application/json' }
		});
	}
});
