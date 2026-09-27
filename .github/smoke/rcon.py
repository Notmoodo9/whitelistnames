"""Smoke test: sends the mod's commands to a running server over RCON and prints the replies."""
import socket, struct, sys, time

def send(s, i, t, body):
    data = struct.pack('<ii', i, t) + body.encode() + b'\0\0'
    s.sendall(struct.pack('<i', len(data)) + data)

def recv(s):
    n = struct.unpack('<i', s.recv(4))[0]
    d = b''
    while len(d) < n:
        d += s.recv(n - len(d))
    return d[8:-2].decode('utf-8', 'replace')

TAGS = '[I;1,2,3,4]'
UUID = '00000001-0000-0002-0000-000300000004'
# Same data the mod summons nametags with (tagged wn_test so the mod's cleanup leaves it alone).
SUMMON = ('summon minecraft:text_display 0 -60 0 {UUID:' + TAGS + ',text:"Mr \\"Q\\" Notch",billboard:"center",see_through:1b,'
          'Tags:["wn_test"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],'
          'translation:[0f,0.35f,0f],scale:[1f,1f,1f]}}')
# A nametag riding nobody, like one left behind by a logoff: the mod must delete it within ~5 seconds.
STRAY = 'summon minecraft:text_display 0 -60 0 {UUID:[I;5,6,7,8],text:"stray",Tags:["wn_nametag"]}'
STRAY_UUID = '00000005-0000-0006-0000-000700000008'
CRAFTER = ('setblock 2 -60 2 minecraft:crafter[orientation=up_east]{Items:['
           '{Slot:0b,id:"minecraft:ender_pearl",count:1},{Slot:1b,id:"minecraft:blaze_powder",count:1}]} replace')
cmds = [
    'forceload add 0 0',
    'whitelist add Notch Mr Notch',
    'whitelist list',
    'changename Notch Mr_N',
    'changename Mr_N "Mr N 2"',
    'changename "Mr N 2" Notch',
    ('whitelist add jeb_ Notch', "already someone else's name"),
    'whitelist add jeb_ Jebby',
    'eyerecipe',
    # A crafter uses the recipe lookup too: power it and see if an Eye of Ender pops out.
    'eyerecipe disable',
    'setblock 2 -60 2 minecraft:air',
    CRAFTER,
    'data get block 2 -60 2 Items',
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    ('execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]', 'Test failed'),
    'setblock 2 -60 3 minecraft:air',
    'kill @e[type=minecraft:item]',
    'eyerecipe enable',
    'setblock 2 -60 2 minecraft:air',
    CRAFTER,
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    ('execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]', 'Test passed'),
    'setblock 2 -60 3 minecraft:air',
    'kill @e[type=minecraft:item]',
    # Disabling again must also beat the crafter's recipe cache, which now holds the recipe.
    'eyerecipe disable',
    'setblock 2 -60 2 minecraft:air',
    CRAFTER,
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    ('execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]', 'Test failed'),
    'setblock 2 -60 3 minecraft:air',
    'kill @e[type=minecraft:item]',
    'eyerecipe enable',
    'team list',
    'team add wn_test',
    'team modify wn_test nametagVisibility never',
    SUMMON,
    'data get entity ' + UUID + ' text',
    'data get entity ' + UUID + ' billboard',
    'data get entity ' + UUID + ' see_through',
    'data get entity ' + UUID + ' transformation',
    'data get entity ' + UUID + ' Tags',
    'kill @e[type=minecraft:text_display,tag=wn_test]',
    STRAY,
    'data get entity ' + STRAY_UUID + ' text',
    'SLEEP6',
    ('data get entity ' + STRAY_UUID + ' text', 'No entity was found'),
]
s = socket.create_connection(('127.0.0.1', 25575), timeout=20)
send(s, 1, 3, 'test'); print('auth:', recv(s))
failures = []
for i, c in enumerate(cmds):
    expect = None
    if isinstance(c, tuple):
        c, expect = c
    if c.startswith('SLEEP'):
        time.sleep(int(c[5:] or 2))
        continue
    send(s, 10 + i, 2, c)
    print('>', c, flush=True)
    try:
        reply = recv(s)
    except Exception as ex:
        reply = '<no reply: %s>' % ex
    print('<', reply, flush=True)
    if expect is not None and expect not in reply:
        failures.append('%s -> %r (expected %r)' % (c, reply, expect))
    time.sleep(0.3)

if failures:
    print('SMOKE TEST FAILURES:')
    for f in failures:
        print('  ' + f)
    sys.exit(1)
print('All smoke checks passed.')
