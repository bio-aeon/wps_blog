import path from 'path';
import Config from 'webpack-config';
import PATHS from './webpack/paths';

const load = file => {
  // Resolve the config file
  let wp;

  try {
    wp = require(path.resolve(PATHS.webpack, file)).default;
  } catch (e) {
    console.error(`Error: ${file}.js not found or has errors:`);
    console.error(e);
    process.exit();
  }

  // If the config isn't already an array, add it to a new one, map over each
  // `webpack-config`, and create a 'regular' Webpack-compatible object
  return (Array.isArray(wp) ? wp : [wp]).map(config => (
    new Config().merge(config).toObject()
  ));
};

const toExport = [];

toExport.push(...load('browser'));
toExport.push(...load('server'));

if (!toExport.length) {
  console.error('Error: WEBPACK_CONFIG files not given');
  process.exit();
}

export default toExport;
