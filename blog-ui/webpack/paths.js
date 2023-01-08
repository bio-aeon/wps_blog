import path from 'path';

const root = path.resolve(__dirname, '..');

export default {
  root,
  dist: path.resolve(root, 'dist'),
  public: path.resolve(root, 'dist', 'public'),
  webpack: path.resolve(root, 'webpack')
};
