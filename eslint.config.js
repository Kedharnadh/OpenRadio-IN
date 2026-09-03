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
        OpenRadioCast: "readonly",
      },
    },
    rules: {
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      "no-console": "warn",
      eqeqeq: "error",
      "no-var": "error",
      "prefer-const": "error",
      semi: ["error", "always"],
      quotes: ["warn", "single", { avoidEscape: true }],
    },
  },
  {
    ignores: ["node_modules/", "wrangler/", "output/", ".wrangler/"],
  },
];
