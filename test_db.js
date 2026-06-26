const { Client } = require('pg');

const client = new Client({
  connectionString: 'postgresql://postgres.nlhqorniufcdbyzuoqgq:syJYNv9ep6027cfU@aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres'
});

async function run() {
  await client.connect();
  const res = await client.query('SELECT id, display_name FROM users_profile');
  console.log(res.rows);
  await client.end();
}

run();
