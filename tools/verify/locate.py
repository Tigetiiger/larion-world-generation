"""Usage: locate.py <server_dir> <java_home> <seed> <biome>... : run /locate biome for each on a fresh world."""
import subprocess, sys, re, shutil, os
d, jh, seed, biomes = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]
w = f'world-locate-{seed}'
shutil.rmtree(f'{d}/{w}', ignore_errors=True)
props = open(f'{d}/server.properties').read()
props = re.sub(r'level-name=.*', f'level-name={w}', re.sub(r'level-seed=.*', f'level-seed={seed}', props))
open(f'{d}/server.properties', 'w').write(props)
p = subprocess.Popen([f'{jh}/bin/java', '-Xmx6G', '-jar', 'fabric-server.jar', 'nogui'], cwd=d, stdin=subprocess.PIPE,
                     stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
todo = list(biomes)
def send(c): p.stdin.write(c + '\n'); p.stdin.flush()
for line in p.stdout:
    if re.search(r'Done \([\d.]+s\)!', line): send(f'locate biome {todo[0]}')
    elif 'The nearest' in line or 'Could not find' in line:
        print(seed, line.strip().split(']: ')[-1]); todo.pop(0)
        send(f'locate biome {todo[0]}') if todo else send('stop')
p.wait(); shutil.rmtree(f'{d}/{w}', ignore_errors=True)
