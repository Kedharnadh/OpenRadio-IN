import json
from pathlib import Path
db=json.loads(Path('database/stations.json').read_text())
lines=['#EXTM3U']
for s in db:
    for st in s.get('streams',[]):
        lines.append(f'#EXTINF:-1 group-title="{s["category"]}",{s["name"]}')
        lines.append(st['url'])
Path('output/playlists/all.m3u').write_text("\n".join(lines),encoding='utf-8')
print('Playlist generated')
