import sys, numpy as np, collections
from compare import chunks, unpack, analyze
a_dir, b_dir = sys.argv[1:3]
A, B = analyze(a_dir), analyze(b_dir)
bad = [(k, int(np.abs(B[k]['top'] - A[k]['top']).max())) for k in A if k in B and (B[k]['top'] != A[k]['top']).any()]
bad.sort(key=lambda t: -t[1]); print('chunks with diffs:', len(bad), bad[:6])
def names(d, want):
    out = {}
    for cx, cz, nbt in chunks(d):
        if (cx, cz) not in want: continue
        st = nbt.get('structures', nbt.get('Structures', {}))
        refs = list(st.get('References', {}).keys()) + list(st.get('starts', {}).keys())
        out[(cx, cz)] = refs
    return out
want = {k for k, _ in bad[:6]}
print('A structures:', names(a_dir, want)); print('B structures:', names(b_dir, want))
allb = names(b_dir, {k for k, _ in bad})
c = collections.Counter('trial_chambers' if any('trial_chambers' in r for r in v) else ','.join(v) or 'none' for v in allb.values())
print('differing chunks by structure:', dict(c))
