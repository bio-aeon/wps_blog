import path from 'path';
import WebpackCleanupPlugin from 'webpack-cleanup-plugin';
import PATHS from './paths';

export default {
  entry: {
    browser: path.join(PATHS.root, 'browser.js'),
  },
  output: {
    path: PATHS.public,
    filename: '[name].[chunkhash].js'
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        query: {
          // Ignore the .babelrc at the root of our project-- that's only
          // used to compile our webpack settings, NOT for bundling
          babelrc: false,
          presets: [
            ['env', {
              // Enable tree-shaking by disabling commonJS transformation
              modules: false,
              // Exclude default regenerator-- we want to enable async/await
              // so we'll do that with a dedicated plugin
              exclude: ['transform-regenerator'],
            }],
            // Transpile JSX code
            'react',
          ]
        },
      }
    ]
  },
  plugins: [
    new WebpackCleanupPlugin()
  ],
  stats: {
    colors: true
  },
  devtool: 'source-map'
};
