"""Count gravity blocks (sand/gravel) sitting on air or fluid = blocks that fall on first update."""
import sys, numpy as np
from compare import chunks, unpack
GRAV = {'minecraft:sand', 'minecraft:red_sand', 'minecraft:gravel', 'minecraft:suspicious_sand', 'minecraft:suspicious_gravel'}
FREE = {'minecraft:air', 'minecraft:cave_air', 'minecraft:water', 'minecraft:lava', 'minecraft:bubble_column'}
for d in sys.argv[1:]:
    tot = n = 0
    for _, _, nbt in chunks(d):
        if nbt.get('Status') not in ('minecraft:full', 'full'): continue
        n += 1
        secs = sorted(nbt['sections'], key=lambda s: s['Y'])
        g = np.zeros((40 * 16, 16, 16), bool); f = np.zeros_like(g)
        for s in secs:
            si = s['Y'] + 8
            bs = s.get('block_states')
            if not bs or not 0 <= si < 40: continue
            pal = [p['Name'] for p in bs['palette']]
            gp = np.array([p in GRAV for p in pal]); fp = np.array([p in FREE for p in pal])
            idx = np.zeros(4096, int) if len(pal) == 1 else unpack(bs['data'], max(4, (len(pal) - 1).bit_length()), 4096)
            g[si*16:(si+1)*16] = gp[idx].reshape(16, 16, 16); f[si*16:(si+1)*16] = fp[idx].reshape(16, 16, 16)
        tot += int((g[1:] & f[:-1]).sum())
    print(f'{d.split("/")[0]:13s} {n} chunks, floating sand/gravel: {tot} ({tot/max(n,1):.1f}/chunk)')
