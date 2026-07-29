const express = require('express');
const { spawn } = require('child_process');
const ffmpegPath = require('ffmpeg-static');

const app = express();
const PORT = process.env.PORT || 3000;

app.get('/proxy', (req, res) => {
  const hlsUrl = req.query.url;
  if (!hlsUrl) return res.status(400).send('Missing ?url=');

  res.setHeader('Content-Type', 'audio/mpeg');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Access-Control-Allow-Origin', '*');

  const ffmpeg = spawn(ffmpegPath, [
    '-i', hlsUrl,
    '-acodec', 'libmp3lame',
    '-ab', '128k',
    '-ar', '44100',
    '-ac', '2',
    '-f', 'mp3',
    '-loglevel', 'error',
    '-'
  ]);

  ffmpeg.stdout.pipe(res);

  ffmpeg.stderr.on('data', (data) => {
    console.error('ffmpeg:', data.toString());
  });

  ffmpeg.on('error', (err) => {
    console.error('Failed to start ffmpeg:', err);
    if (!res.headersSent) res.status(500).send('ffmpeg error');
  });

  req.on('close', () => {
    ffmpeg.kill('SIGTERM');
  });
});

app.listen(PORT, () => console.log(`HLS proxy on port ${PORT}`));
