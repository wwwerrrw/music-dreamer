module.exports = {
  devServer: {
    host: '127.0.0.1',
    port: 5174,
    proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } }
  },
  outputDir: 'dist',
  productionSourceMap: false
};
