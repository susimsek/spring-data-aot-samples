module.exports = {
  '{src/main/webapp/**/}*.{js,jsx,mjs,cjs,ts,tsx}': ['eslint --fix', 'prettier --write'],
  '{,src/main/webapp/**/,docs/**/}*.{css,scss,html,md,json,yml,yaml}': ['prettier --write'],
};
