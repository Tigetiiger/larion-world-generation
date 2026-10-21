"""Usage: pregen.py <server_dir> <java_home> <seed> <radius_blocks> [centerX centerZ] [--keep]
Creates a fresh world named world-<seed>, pregenerates a square with Chunky, stops the server."""
import subprocess, sys, os, time, shutil, re
d, jh, seed, radius = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
cx, cz = (sys.argv[5], sys.argv[6]) if len(sys.argv) > 6 else ('0', '0')
world = f'world-{seed}-{cx}-{cz}'
if os.path.exists(f'{d}/{world}'): shutil.rmtree(f'{d}/{world}')
props = {'level-seed': seed, 'level-name': world, 'online-mode': 'false', 'server-port': '25599',
         'max-tick-time': '-1', 'sync-chunk-writes': 'false', 'spawn-protection': '0', 'view-distance': '4',
         'simulation-distance': '4', 'level-type': 'minecraft\\:normal', 'generate-structures': 'true'}
open(f'{d}/server.properties', 'w').write(''.join(f'{k}={v}\n' for k, v in props.items()))
log = open(f'{d}/{world}.log', 'w')
p = subprocess.Popen([f'{jh}/bin/java', '-Xmx6G', '-jar', 'fabric-server.jar', 'nogui'], cwd=d,
                     stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
def send(c): p.stdin.write(c + '\n'); p.stdin.flush()
t0 = None; state = 'boot'
for line in p.stdout:
    log.write(line); log.flush()
    if state == 'boot' and re.search(r'Done \([\d.]+s\)!', line):
        for c in ('chunky world minecraft:overworld', f'chunky center {cx} {cz}', f'chunky radius {radius}', 'chunky start'):
            send(c)
        t0 = time.time(); state = 'gen'
    elif state == 'gen' and 'Task finished' in line:
        print(f'{d} seed={seed}: pregen {time.time()-t0:.1f}s :: {line.strip()[-120:]}')
        send('save-all flush'); send('stop'); state = 'stop'
    elif 'Exception' in line or 'ERROR' in line:
        print('!!', line.strip()[:300])
p.wait()
print('exit', p.returncode, 'state', state)
