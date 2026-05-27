#!/usr/bin/env bash
source "$HOME/.nvm/nvm.sh" 2>/dev/null
nvm use 20 --silent 2>/dev/null
npm show next dist-tags.latest 2>/dev/null
npm show next@">=15.3.0 <16.0.0" version 2>/dev/null | tail -5
