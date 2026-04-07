#!/bin/bash

# Save Claude session transcripts.
# Works as both:
# 1. A Claude SessionEnd hook (receives JSON on stdin with transcript_path)
# 2. A git pre-commit hook (copies transcripts into .claude-sessions/ and stages them)
#
# Claude stores transcripts at ~/.claude/projects/<encoded-path>/*.jsonl
# where <encoded-path> replaces all non-alphanumeric characters with -.
# For paths longer than 200 chars (encoded), Claude truncates and appends a hash suffix.

project_dir="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null)}"
if [ -z "$project_dir" ]; then
  exit 0
fi

session_dir="$project_dir/.claude-sessions"

copy_transcripts() {
  local encoded_path claude_dir
  encoded_path="$(echo "$project_dir" | sed 's|[^a-zA-Z0-9]|-|g')"

  if [ "${#encoded_path}" -le 200 ]; then
    # Short path: exact match
    claude_dir="$HOME/.claude/projects/$encoded_path"
  else
    # Long path (>200 chars): Claude truncates to 200 chars and appends a hash.
    # Use the prefix to find the matching directory.
    local prefix="${encoded_path:0:200}"
    claude_dir="$(find "$HOME/.claude/projects" -maxdepth 1 -type d -name "${prefix}*" | head -1)"
  fi

  if [ -n "$claude_dir" ] && [ -d "$claude_dir" ]; then
    mkdir -p "$session_dir"
    cp "$claude_dir"/*.jsonl "$session_dir/" 2>/dev/null
  fi
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
