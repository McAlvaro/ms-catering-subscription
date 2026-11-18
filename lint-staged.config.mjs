import { execFileSync } from 'node:child_process';

// Spotless matches each -DspotlessFiles regex against the absolute path and splits the list on ','.
const toSpotlessPattern = (absPath) =>
  absPath.replace(/[\\^$.|?*+()[\]{}]/g, '\\$&').replace(/,/g, '\\x2C');

export default {
  '*.java': {
    title: 'spotless:apply (palantir-java-format)',
    task: (files) => {
      if (files.length === 0) return;
      try {
        execFileSync('mvn', ['-B', '-q', 'spotless:apply',
          `-DspotlessFiles=${files.map(toSpotlessPattern).join(',')}`],
          { cwd: import.meta.dirname, encoding: 'utf8', stdio: 'pipe' });
      } catch (err) {
        throw new Error(`spotless:apply failed\n${err.stdout ?? ''}${err.stderr ?? ''}`);
      }
    },
  },
};
