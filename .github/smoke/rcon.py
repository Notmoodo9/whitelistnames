"""Smoke test: sends the mod's commands to a running server over RCON and prints the replies."""
import socket, struct, time

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
SUMMON = ('summon minecraft:text_display 0 -60 0 {UUID:' + TAGS + ',text:"Mr \\"Q\\" Notch",billboard:"center",see_through:1b,'
          'Tags:["wn_nametag","wn_test"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],'
          'translation:[0f,0.35f,0f],scale:[1f,1f,1f]}}')
CRAFTER = ('setblock 2 -60 2 minecraft:crafter[orientation=up_east]{Items:['
           '{Slot:0b,id:"minecraft:ender_pearl",count:1},{Slot:1b,id:"minecraft:blaze_powder",count:1}]} replace')
cmds = [
    'forceload add 0 0',
    'whitelist add Notch Mr Notch',
    'whitelist list',
    'changename Notch Mr_N',
    'changename Mr_N "Mr N 2"',
    'changename "Mr N 2" Notch',
    'whitelist add jeb_ Notch',
    'whitelist add jeb_ Jebby',
    'eyerecipe',
    # A crafter uses the recipe lookup too: power it and see if an Eye of Ender pops out.
    'eyerecipe disable',
    CRAFTER,
    'data get block 2 -60 2 Items',
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]',
    'setblock 2 -60 3 minecraft:air',
    'kill @e[type=minecraft:item]',
    'eyerecipe enable',
    CRAFTER,
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]',
    'setblock 2 -60 3 minecraft:air',
    'kill @e[type=minecraft:item]',
    # Disabling again must also beat the crafter's recipe cache, which now holds the recipe.
    'eyerecipe disable',
    CRAFTER,
    'setblock 2 -60 3 minecraft:redstone_block',
    'SLEEP',
    'data get block 2 -60 2 Items',
    'execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:ender_eye"}}]',
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
    'tag @e[type=minecraft:text_display,tag=wn_nametag] add wn_orphan',
    'execute as @e[type=minecraft:text_display,tag=wn_nametag] on vehicle on passengers run tag @s remove wn_orphan',
    'kill @e[type=minecraft:text_display,tag=wn_orphan]',
    'data get entity ' + UUID,
    'kill @e[type=minecraft:text_display,tag=wn_test]',
]
s = socket.create_connection(('127.0.0.1', 25575), timeout=20)
send(s, 1, 3, 'test'); print('auth:', recv(s))
for i, c in enumerate(cmds):
    if c == 'SLEEP':
        time.sleep(2)
        continue
    send(s, 10 + i, 2, c)
    print('>', c, flush=True)
    try:
        print('<', recv(s), flush=True)
    except Exception as ex:
        print('<! no reply:', ex, flush=True)
    time.sleep(0.3)
