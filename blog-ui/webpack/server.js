import path from 'path';
import nodeModules from 'webpack-node-externals';
import PATHS from './paths';

export default {
  entry: {
    main: path.resolve(PATHS.root, 'server.js')
  },
  output: {
    path: PATHS.dist,
    publicPath: '/',
    filename: 'server.js',
  },
  resolve: {
    extensions: [".js", ".jsx"],
    modules: [
      PATHS.root,
      'node_modules',
    ],
  },
  target: 'node',
  node: {
    __dirname: false,
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader",
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
      }
    ]
  },
  externals: nodeModules()
}
