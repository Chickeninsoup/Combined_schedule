#!/bin/bash

# Save Claude session transcripts.
# Works as both:
# 1. A Claude SessionEnd hook (receives JSON on stdin with transcript_path)
# 2. A git pre-commit hook (copies transcripts into .claude-sessions/ and stages them)
#
# Claude stores transcripts at ~/.claude/projects/<encoded-path>/*.jsonl
# where <encoded-path> replaces / with - in the project directory path.

project_dir="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null)}"
if [ -z "$project_dir" ]; then
  exit 0
fi

session_dir="$project_dir/.claude-sessions"

copy_transcripts() {
  # Try multiple path encodings to handle Mac, Linux, and Windows (MSYS2/Git Bash)
  for encoded_path in \
    "$(echo "$project_dir" | sed 's|/|-|g')" \
    "$(cygpath -w "$project_dir" 2>/dev/null | sed 's|[:/\\]|-|g')" \
  ; do
    [ -z "$encoded_path" ] && continue
    claude_dir="$HOME/.claude/projects/$encoded_path"
    if [ -d "$claude_dir" ]; then
      mkdir -p "$session_dir"
      cp "$claude_dir"/*.jsonl "$session_dir/" 2>/dev/null
      break
    fi
  done
}

if [ -n "$CLAUDE_SESSION_ID" ]; then
  # Running as a Claude SessionEnd hook
  cat > /dev/null  # consume stdin
  copy_transcripts
else
  # Running as a git pre-commit hook
  if git check-ignore -q "$session_dir" 2>/dev/null; then
    echo "simple-claude-save: .claude-sessions/ is .gitignored — remove it from .gitignore to save transcripts" >&2
    exit 1
  fi
  copy_transcripts
  if [ -d "$session_dir" ]; then
    git add "$session_dir"/*.jsonl 2>/dev/null || true
  fi
fi
