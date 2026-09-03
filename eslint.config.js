import js from "@eslint/js";
import globals from "globals";

export default [
  js.configs.recommended,
  {
    files: ["website/**/*.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.worker,
        cast: "readonly",
        chrome: "readonly",
        clients: "readonly",
      },
    },
    rules: {
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_", caughtErrors: "none" }],
      "no-console": "off",
      "no-empty": ["error", { allowEmptyCatch: true }],
      eqeqeq: "error",
      "no-var": "error",
      "prefer-const": "warn",
      semi: ["error", "always"],
      quotes: ["warn", "single", { avoidEscape: true }],
    },
  },
  {
    ignores: [
      "node_modules/",
      "wrangler/",
      "output/",
      "build/",
      "public/",
      "android/",
    ],
  },
];
