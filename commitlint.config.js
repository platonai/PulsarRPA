// Commitlint configuration (CJS) picked up by @commitlint/cli
// Mirrors the intent from .commitlintrc.js but in the supported filename
// so it takes precedence over package.json config.
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'perf',
        'test',
        'chore',
        'ci',
        'revert'
      ]
    ],
    // Allow longer headers for descriptive refs
    'header-max-length': [2, 'always', 200],
    // Keep subject-case flexible
    'subject-case': [0],
  },
};

