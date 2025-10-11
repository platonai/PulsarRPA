module.exports = {
    extends: ['@commitlint/config-conventional'],
    rules: {
        'type-enum': [
            2,
            'always',
            [
                'feat',     // new feature
                'fix',      // bug fix
                'docs',     // documentation only
                'style',    // formatting
                'refactor', // code restructuring
                'perf',     // performance improvements
                'test',     // add or modify tests
                'chore',    // build process, dependencies, configs
                'ci',       // CI/CD related changes
                'revert'    // revert previous commits
            ]
        ],
        'header-max-length': [2, 'always', 200],
        'subject-case': [0], // flexible (lowercase recommended)
    },
};
