import psycopg2
import os

url = "postgresql://postgres.nlhqorniufcdbyzuoqgq:syJYNv9ep6027cfU@aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres"
conn = psycopg2.connect(url)
cur = conn.cursor()
cur.execute("SELECT id, display_name FROM users_profile")
print(cur.fetchall())
