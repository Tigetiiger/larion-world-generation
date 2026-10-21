#!/bin/bash
# Pregenerate test areas on reference, port and vanilla servers, then compare.
J21=$HOME/.local/jdks/jdk-21.0.12.1+1; J25=$HOME/.local/jdks/jdk-25.0.4.1+1
cd "$(dirname "$0")"
R=${RADIUS:-512}
for spec in "12345 0 0" "-987654321 0 0" "42 0 0" "12345 20000 -15000"; do
  set -- $spec; s=$1; x=$2; z=$3; w=world-$s-$x-$z
  python3 -I pregen.py ref-1.21.1 $J21 $s $R $x $z
  python3 -I pregen.py port-26.2 $J25 $s $R $x $z
  [ -n "$VANILLA" ] && python3 -I pregen.py vanilla-26.2 $J25 $s $R $x $z
  python3 -I compare.py ref-1.21.1/$w/region port-26.2/$w/dimensions/minecraft/overworld/region results/$w
done
