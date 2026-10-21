"""Compare two generated worlds column by column.

Usage: compare.py <regionDirA> <regionDirB> <outPrefix>
Prints terrain-height, fluid, biome and cave stats; writes <outPrefix>-height.png (diff heatmap).
"""
import os, re, sys, struct, zlib, gzip, collections, json
import numpy as np

# ---------- minimal NBT ----------
def read_nbt(b):
    pos = 0
    def u(fmt, n):
        nonlocal pos
        v = struct.unpack_from(fmt, b, pos); pos += n; return v[0]
    def s():
        nonlocal pos
        n = u('>H', 2); v = b[pos:pos + n].decode('utf-8', 'replace'); pos += n; return v
    def payload(t):
        nonlocal pos
        if t == 1: return u('>b', 1)
        if t == 2: return u('>h', 2)
        if t == 3: return u('>i', 4)
        if t == 4: return u('>q', 8)
        if t == 5: return u('>f', 4)
        if t == 6: return u('>d', 8)
        if t == 7:
            n = u('>i', 4); v = b[pos:pos + n]; pos += n; return v
        if t == 8: return s()
        if t == 9:
            et = u('>b', 1); n = u('>i', 4); return [payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = u('>b', 1)
                if tt == 0: return d
                k = s(); d[k] = payload(tt)
        if t == 11:
            n = u('>i', 4); v = np.frombuffer(b, '>i4', n, pos); pos += 4 * n; return v
        if t == 12:
            n = u('>i', 4); v = np.frombuffer(b, '>i8', n, pos); pos += 8 * n; return v
        raise ValueError(t)
    t = u('>b', 1); s(); return payload(t)

def chunks(region_dir):
    for f in os.listdir(region_dir):
        m = re.match(r'r\.(-?\d+)\.(-?\d+)\.mca$', f)
        if not m: continue
        rx, rz = int(m[1]), int(m[2])
        data = open(os.path.join(region_dir, f), 'rb').read()
        if len(data) < 8192: continue
        for i in range(1024):
            off = int.from_bytes(data[i * 4:i * 4 + 3], 'big') * 4096
            if not off: continue
            ln = int.from_bytes(data[off:off + 4], 'big'); ct = data[off + 4]
            raw = data[off + 5:off + 4 + ln]
            raw = zlib.decompress(raw) if ct == 2 else gzip.decompress(raw) if ct == 1 else raw
            yield rx * 32 + i % 32, rz * 32 + i // 32, read_nbt(raw)

# ---------- block classes ----------
AIR, WATER, LAVA, TERRAIN, OTHER = 0, 1, 2, 3, 4
TERRAIN_RE = re.compile(r'^minecraft:(stone|deepslate|granite|diorite|andesite|tuff|calcite|dirt|coarse_dirt|rooted_dirt|'
    r'grass_block|podzol|mycelium|mud|clay|sand|red_sand|sandstone|red_sandstone|gravel|suspicious_gravel|suspicious_sand|'
    r'.*terracotta|snow_block|powder_snow|ice|packed_ice|blue_ice|moss_block|pale_moss_block|dripstone_block|.*_ore|bedrock|'
    r'obsidian|magma_block|cinnabar|sulfur|potent_sulfur|smooth_basalt|amethyst_block|budding_amethyst|raw_iron_block|raw_copper_block)$')
def classify(name):
    if name in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'): return AIR
    if name in ('minecraft:water', 'minecraft:bubble_column', 'minecraft:kelp', 'minecraft:kelp_plant',
                'minecraft:seagrass', 'minecraft:tall_seagrass'): return WATER
    if name == 'minecraft:lava': return LAVA
    return TERRAIN if TERRAIN_RE.match(name) else OTHER

def unpack(longs, bits, n):
    k = 64 // bits
    v = longs.astype(np.int64).view(np.uint64)
    sh = (np.arange(k, dtype=np.uint64) * np.uint64(bits))
    out = (v[:, None] >> sh[None, :]) & np.uint64((1 << bits) - 1)
    return out.reshape(-1)[:n].astype(np.int64)

def analyze(region_dir, min_y=-128):
    res = {}
    for cx, cz, nbt in chunks(region_dir):
        if nbt.get('Status') not in ('minecraft:full', 'full'): continue
        secs = sorted(nbt['sections'], key=lambda s: s['Y'])
        ys = [s['Y'] for s in secs]
        nsec = 40
        cls = np.zeros((nsec * 16, 16, 16), np.int8)
        biomes = []
        for s in secs:
            si = s['Y'] - min_y // 16
            if not (0 <= si < nsec): continue
            bs = s.get('block_states')
            if bs:
                pal = [classify(p['Name']) for p in bs['palette']]
                if len(pal) == 1:
                    cls[si * 16:(si + 1) * 16] = pal[0]
                else:
                    bits = max(4, (len(pal) - 1).bit_length())
                    idx = unpack(bs['data'], bits, 4096)
                    cls[si * 16:(si + 1) * 16] = np.array(pal, np.int8)[idx].reshape(16, 16, 16)
            bm = s.get('biomes')
            if bm:
                pal = bm['palette']
                if len(pal) == 1: b = [pal[0]] * 64
                else:
                    bits = (len(pal) - 1).bit_length()
                    b = [pal[i] for i in unpack(bm['data'], bits, 64)]
                biomes.append((si, b))
        ter = cls == TERRAIN
        has = ter.any(axis=0)
        top = np.where(has, cls.shape[0] - 1 - np.argmax(ter[::-1], axis=0), -1)  # [z,x] index (y-min_y)
        fl = (cls == WATER) | (cls == LAVA)
        ycol = np.arange(cls.shape[0])[:, None, None]
        above = fl & (ycol > top[None])
        ftop = np.where(above.any(axis=0), cls.shape[0] - 1 - np.argmax(above[::-1], axis=0), -1)
        below = (ycol < top[None]) & (cls == AIR)
        res[(cx, cz)] = {'top': top + min_y, 'ftop': np.where(ftop >= 0, ftop + min_y, -9999),
                         'cave': int(below.sum()), 'biomes': dict(biomes)}
    return res

def main():
    a_dir, b_dir, out = sys.argv[1:4]
    A, B = analyze(a_dir), analyze(b_dir)
    keys = sorted(set(A) & set(B))
    print(f'chunks: A={len(A)} B={len(B)} common={len(keys)}')
    if not keys: return
    dh = np.stack([B[k]['top'] - A[k]['top'] for k in keys])
    eq = (dh == 0).mean() * 100
    print(f'terrain height: {eq:.3f}% columns equal, |diff|>2: {(abs(dh) > 2).mean() * 100:.3f}%, max |diff| {abs(dh).max()}')
    df = np.stack([B[k]['ftop'] - A[k]['ftop'] for k in keys])
    print(f'fluid surface: {(df == 0).mean() * 100:.3f}% columns equal')
    ca = np.array([A[k]['cave'] for k in keys], float); cb = np.array([B[k]['cave'] for k in keys], float)
    corr = np.corrcoef(ca, cb)[0, 1] if ca.std() and cb.std() else float('nan')
    print(f'cave air below terrain: A={ca.sum():.0f} B={cb.sum():.0f} ratio={cb.sum() / max(ca.sum(), 1):.3f} per-chunk corr={corr:.3f}')
    tot = same = 0; pairs = collections.Counter(); cntA = collections.Counter(); cntB = collections.Counter()
    for k in keys:
        for si, bb in B[k]['biomes'].items():
            ba = A[k]['biomes'].get(si)
            cntB.update(bb)
            if ba is None: continue
            cntA.update(ba)
            for x, y in zip(ba, bb):
                tot += 1; same += x == y
                if x != y: pairs[(x, y)] += 1
    print(f'biome cells: {same / max(tot, 1) * 100:.3f}% equal ({tot} cells)')
    for (x, y), n in pairs.most_common(8): print(f'   {x} -> {y}: {n}')
    for name in ('minecraft:sulfur_caves', 'minecraft:pale_garden'):
        print(f'   {name}: A={cntA[name]} B={cntB[name]}')
    # heatmap
    try:
        from PIL import Image
        xs = [k[0] for k in keys]; zs = [k[1] for k in keys]
        x0, z0 = min(xs), min(zs); W, H = (max(xs) - x0 + 1) * 16, (max(zs) - z0 + 1) * 16
        img = np.full((H, W, 3), 40, np.uint8)
        for k, d in zip(keys, dh):
            px, pz = (k[0] - x0) * 16, (k[1] - z0) * 16
            c = np.zeros((16, 16, 3), np.uint8); c[..., 1] = 160
            c[d > 0] = [220, 60, 60]; c[d < 0] = [60, 60, 220]
            img[pz:pz + 16, px:px + 16] = c
        Image.fromarray(img).save(out + '-height.png')
        print('wrote', out + '-height.png')
    except Exception as e:
        print('no heatmap:', e)
    json.dump({'height_equal_pct': eq}, open(out + '.json', 'w'))

if __name__ == '__main__':
    main()
