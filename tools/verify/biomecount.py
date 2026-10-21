import sys, collections
from compare import chunks, unpack
def count(d):
    c = collections.Counter(); n = 0
    for _, _, nbt in chunks(d):
        for s in nbt.get('sections', []):
            bm = s.get('biomes')
            if not bm: continue
            pal = bm['palette']
            if len(pal) == 1: c[pal[0]] += 64
            else:
                for i in unpack(bm['data'], (len(pal) - 1).bit_length(), 64): c[pal[i]] += 1
            n += 64
    return c, n
for d in sys.argv[1:]:
    c, n = count(d)
    print(f'{d.split("/")[0]:13s} {d.split("/")[1]:28s} sulfur={c["minecraft:sulfur_caves"]/n*100:.3f}%  pale_garden={c["minecraft:pale_garden"]/n*100:.3f}%  dark_forest={c["minecraft:dark_forest"]/n*100:.3f}%')
