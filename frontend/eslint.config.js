import tseslint from 'typescript-eslint';

const aviso = 'Una feature no importa el interior de otra.';

export default [
  {
    files: ['src/**/*.ts'],
    languageOptions: { parser: tseslint.parser },
  },
  {
    files: ['src/app/features/tickets/**/*.ts'],
    rules: {
      'no-restricted-imports': ['error', { patterns: [{ group: ['**/features/sesion', '**/features/sesion/**'], message: aviso }] }],
    },
  },
  {
    files: ['src/app/features/sesion/**/*.ts'],
    rules: {
      'no-restricted-imports': ['error', { patterns: [{ group: ['**/features/tickets', '**/features/tickets/**'], message: aviso }] }],
    },
  },
];
