"""Count dropped item entities in a world's entity region files. Usage: items.py <entitiesDir>..."""
import sys, collections
from compare import chunks
for d in sys.argv[1:]:
    c = collections.Counter(); n = 0
    for _, _, nbt in chunks(d):
        n += 1
        for e in nbt.get('Entities', []):
            if e.get('id') == 'minecraft:item':
                c[e.get('Item', {}).get('id', '?')] += e.get('Item', {}).get('count', e.get('Item', {}).get('Count', 1))
    print(f'{d}: {n} entity chunks, {sum(c.values())} dropped items', dict(c.most_common(5)))
