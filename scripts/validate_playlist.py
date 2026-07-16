from pathlib import Path
p=Path("playlists/all.m3u")
assert p.exists(),"Missing playlist"
assert p.read_text().startswith("#EXTM3U"),"Invalid playlist"
print("Playlist OK")
