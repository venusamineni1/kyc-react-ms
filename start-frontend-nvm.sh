#!/bin/bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
cd viewer/frontend
nohup npm run dev > ../../frontend_nvm.log 2>&1 &
echo "Frontend started with NVM"
