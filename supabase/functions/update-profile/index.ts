import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

serve(async (req) => {
	if (req.method === 'OPTIONS') {
		return new Response('ok', {
			headers: {
				'Access-Control-Allow-Origin': '*',
				'Access-Control-Allow-Methods': 'POST, OPTIONS',
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

		const body = await req.json();
		const newDisplayName = body.displayName;

		if (!newDisplayName || newDisplayName.trim() === '') {
			return new Response(JSON.stringify({ error: 'Display name cannot be empty' }), {
				status: 400,
				headers: { 'Content-Type': 'application/json' }
			});
		}

		const supabaseAdmin = createClient(
			Deno.env.get('SUPABASE_URL') ?? '',
			Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
		);

		const { data, error } = await supabaseAdmin
			.from('users_profile')
			.update({ display_name: newDisplayName.trim() })
			.eq('id', user.id)
			.select('id, display_name');

		if (error) {
			throw new Error(error.message);
		}

		return new Response(
			JSON.stringify({ message: 'Profile updated successfully', data }),
			{ headers: { 'Content-Type': 'application/json' }, status: 200 }
		);
	} catch (err) {
		return new Response(JSON.stringify({ error: err.message }), {
			headers: { 'Content-Type': 'application/json' },
			status: 500,
		});
	}
});
