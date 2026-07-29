const express = require('express');
const { spawn } = require('child_process');
const ffmpegPath = require('ffmpeg-static');
const path = require('path');
const fs = require('fs');

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

  let stderr = '';
  ffmpeg.stderr.on('data', (data) => {
    stderr += data.toString();
    console.error('ffmpeg:', data.toString().trim());
  });

  ffmpeg.on('error', (err) => {
    console.error('Failed to start ffmpeg:', err);
    if (!res.headersSent) res.status(500).send('ffmpeg error');
  });

  ffmpeg.on('close', (code) => {
    if (code !== 0 && !res.headersSent) {
      res.status(500).send('ffmpeg exited with code ' + code);
    }
  });

  req.on('close', () => {
    ffmpeg.kill('SIGTERM');
  });
});

app.get('/', (req, res) => {
  res.json({
    name: 'OpenRadio-IN HLS Proxy',
    usage: '/proxy?url=<HLS_URL>',
    port: PORT
  });
});

app.listen(PORT, '0.0.0.0', () => console.log(`OpenRadio-IN HLS proxy on 0.0.0.0:${PORT}`));
