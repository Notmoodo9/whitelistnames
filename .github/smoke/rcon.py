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

# A floating nametag left over from an older version of the mod: must be deleted when it loads.
LEGACY = 'summon minecraft:text_display 0 -60 0 {UUID:[I;5,6,7,8],text:"old",Tags:["wn_nametag"]}'
LEGACY_UUID = '00000005-0000-0006-0000-000700000008'
CRAFTER = ('setblock 2 -60 2 minecraft:crafter[orientation=up_east]{Items:['
           '{Slot:0b,id:"minecraft:ender_pearl",count:1},{Slot:1b,id:"minecraft:blaze_powder",count:1}]} replace')
EYE = 'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]'
INVALID = 'Nicknames must be 1-16 characters'


def craft(expect):
    """Loads a crafter with Eye of Ender ingredients, powers it, and checks whether an eye came out."""
    return ['setblock 2 -60 2 minecraft:air', CRAFTER, 'setblock 2 -60 3 minecraft:redstone_block', 'SLEEP',
            (EYE, expect), 'setblock 2 -60 3 minecraft:air', 'kill @e[type=minecraft:item]']


cmds = [
    'forceload add 0 0',
    # Nicknames must be valid account names
    ('whitelist add Notch Mr Notch', INVALID),
    ('whitelist add Notch MrNotch', 'Notch will now go by "MrNotch"'),
    ('whitelist list', 'Notch'),
    ('changename Notch Mr_N', 'to "Mr_N"'),
    ('changename Mr_N "Mr N 2"', INVALID),
    ('changename Mr_N "MrNotch"', 'to "MrNotch"'),
    ('whitelist add jeb_ MrNotch', "already someone else's name"),
    ('whitelist add jeb_ Jebby', 'jeb_ will now go by "Jebby"'),
    # Lookups
    ('namecheck MrNotch', 'MrNotch is Notch'),
    ('namecheck notch', 'Notch goes by MrNotch'),
    ('namecheck Nobody', 'Nobody has the nickname'),
    ('nicks', 'Jebby = jeb_'),
    # Eye of Ender recipe
    ('eyerecipe', 'is enabled'),
    'eyerecipe disable',
    *craft('Test failed'),
    'eyerecipe enable',
    *craft('Test passed'),
    # Disabling again must also beat the crafter's recipe cache, which now holds the recipe.
    'eyerecipe disable',
    *craft('Test failed'),
    'eyerecipe enable',
    # Leftovers from older versions
    LEGACY,
    'SLEEP',
    ('data get entity ' + LEGACY_UUID, 'No entity was found'),
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
