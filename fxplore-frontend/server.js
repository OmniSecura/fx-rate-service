const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const apiUrl = process.env.API_URL || 'http://app:8080';
const port = process.env.PORT || 3000;

app.use('/api', createProxyMiddleware({
  target: apiUrl,
  changeOrigin: true,
  pathRewrite: { '^/api': '/api' },
  proxyTimeout: 30000,
  timeout: 30000,
}));

app.use(express.static(path.join(__dirname, 'build')));

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build', 'index.html'));
});

app.listen(port, () => {
  console.log(`Frontend Node server listening on port ${port}`);
});
