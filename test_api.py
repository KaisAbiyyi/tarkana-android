import urllib.request
import json

req = urllib.request.Request('https://nlhqorniufcdbyzuoqgq.supabase.co/auth/v1/token?grant_type=password', data=b'{"email":"kaisabiyyistudio@gmail.com","password":"password"}', headers={'apikey': 'sb_publishable_BLAs3vJhGLTNATDj0FKRvQ_4mp_EBl2', 'Content-Type': 'application/json'})
try:
    res = json.loads(urllib.request.urlopen(req).read().decode())
    token = res['access_token']
    req2 = urllib.request.Request('https://nlhqorniufcdbyzuoqgq.supabase.co/functions/v1/get-leaderboard', headers={'apikey': 'sb_publishable_BLAs3vJhGLTNATDj0FKRvQ_4mp_EBl2', 'Authorization': 'Bearer ' + token})
    print(urllib.request.urlopen(req2).read().decode())
except urllib.error.HTTPError as e:
    print('HTTP ERROR', e.code)
    print(e.read().decode())
