# Unify Git Hooks Setup Script (PowerShell)
# This script sets up recommended Git hooks for the project on Windows

$ErrorActionPreference = 'Stop'

$hooksDir = Join-Path -Path (Get-Location) -ChildPath ".git/hooks"

Write-Host "Setting up Git hooks for Unify..."

if (-not (Test-Path $hooksDir)) {
    New-Item -ItemType Directory -Path $hooksDir | Out-Null
}

# Content for pre-commit (bash hook content so Git can run it via Git Bash)
$preCommit = @'
#!/bin/bash

echo "Running pre-commit checks..."

# Check for merge conflict markers
if grep -r "<<<<<<< \|======= \|>>>>>>> " --include="*.java" --include="*.xml" --include="*.md" --include="*.json" --include="*.yml" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx" .; then
    echo "❌ Merge conflict markers found. Please resolve conflicts before committing."
    exit 1
fi

# Check for TODO/FIXME comments in staged files
staged_files=$(git diff --cached --name-only --diff-filter=ACM | grep -E "\.(java|ts|tsx|js|jsx)$" || true)
if [ -n "$staged_files" ]; then
    if echo "$staged_files" | xargs grep -l "TODO\|FIXME" 2>/dev/null; then
        echo "⚠️  Warning: TODO/FIXME comments found in staged files."
        echo "Consider addressing these before committing."
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
fi

# Check for sensitive data patterns
if git diff --cached --name-only | xargs grep -l "password\|secret\|key\|token" --include="*.java" --include="*.properties" --include="*.yml" --include="*.json" 2>/dev/null; then
    echo "ERROR: Potential sensitive data found in staged files."
    echo "Please review and remove any hardcoded secrets."
    exit 1
fi

# Run Java code formatting check (if checkstyle is available)
if command -v mvn >/dev/null 2>&1; then
    echo "Checking Java code style..."
    if ! mvn checkstyle:check -q 2>/dev/null; then
        echo "WARNING: Code style issues found. Run 'mvn checkstyle:check' for details."
    fi
fi

# Run quick tests on changed modules
if command -v mvn >/dev/null 2>&1; then
    echo "Running quick tests..."
    
    # Determine which modules have changes
    changed_modules=""
    if git diff --cached --name-only | grep -q "unify-core/"; then
        changed_modules="$changed_modules unify-core"
    fi
    if git diff --cached --name-only | grep -q "unify-io-common/"; then
        changed_modules="$changed_modules unify-io-common"
    fi
    if git diff --cached --name-only | grep -q "unify-io-files/"; then
        changed_modules="$changed_modules unify-io-files"
    fi
    if git diff --cached --name-only | grep -q "unify-integration/"; then
        changed_modules="$changed_modules unify-integration"
    fi
    if git diff --cached --name-only | grep -q "unify-coverage/"; then
        changed_modules="$changed_modules unify-coverage"
    fi
    
    # Run tests for changed modules only
    if [ -n "$changed_modules" ]; then
        for module in $changed_modules; do
            echo "Testing $module..."
            if ! mvn test -pl "$module" -q -Dtest=*UnitTest,*SimpleTest 2>/dev/null; then
                echo "❌ Unit tests failed in $module. Please fix before committing."
                exit 1
            fi
        done
    fi
fi

echo "✅ Pre-commit checks passed!"
'@

$prePush = @'
#!/bin/bash

echo "Running pre-push checks..."

# Get the remote and branch being pushed to
remote="$1"
url="$2"

# Run full test suite before pushing to main branches
current_branch=$(git rev-parse --abbrev-ref HEAD)
if [[ "$current_branch" == "master" || "$current_branch" == "main" || "$current_branch" == "develop" ]]; then
    echo "Pushing to protected branch \"$current_branch\". Running full test suite..."
    
    if command -v mvn >/dev/null 2>&1; then
        if ! mvn test -q; then
            echo "❌ Tests failed. Push aborted."
            exit 1
        fi
        
        # Run integration tests if available
        if ! mvn verify -q -Dtest.profile=integration 2>/dev/null; then
            echo "⚠️  Integration tests failed or not available."
        fi
    fi
fi

# Check that we're not pushing directly to master from a feature branch
if [[ "$current_branch" =~ ^(feature|bugfix|hotfix)/ ]]; then
    echo "⚠️  Pushing feature branch \"$current_branch\" directly."
    echo "Consider creating a Pull Request instead."
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "✅ Pre-push checks passed!"
'@

$commitMsg = @'
#!/bin/bash

# Check commit message format (Conventional Commits)
commit_regex='^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .{1,50}'

if ! grep -qE "$commit_regex" "$1"; then
    echo "❌ Invalid commit message format!"
    echo ""
    echo "Commit messages should follow the Conventional Commits specification:"
    echo "  <type>[optional scope]: <description>"
    echo ""
    echo "Examples:"
    echo "  feat(api): add consumer group management"
    echo "  fix(db): resolve connection pool leak"
    echo "  docs: update installation guide"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, test, chore"
    exit 1
fi

# Check commit message length
if [ $(head -n1 "$1" | wc -c) -gt 72 ]; then
    echo "❌ Commit message too long (max 72 characters for first line)"
    exit 1
fi
'@

Set-Content -LiteralPath (Join-Path $hooksDir 'pre-commit') -Value $preCommit -NoNewline
Set-Content -LiteralPath (Join-Path $hooksDir 'pre-push') -Value $prePush -NoNewline
Set-Content -LiteralPath (Join-Path $hooksDir 'commit-msg') -Value $commitMsg -NoNewline

# Try to set executable bit where supported (on Windows, Git honors hooks without +x if core.filemode=false)
try {
    & git update-index --chmod=+x .git/hooks/pre-commit | Out-Null
    & git update-index --chmod=+x .git/hooks/pre-push | Out-Null
    & git update-index --chmod=+x .git/hooks/commit-msg | Out-Null
} catch {}

Write-Host "✅ Git hooks installed successfully!" 
Write-Host "" 
Write-Host "Installed hooks:" 
Write-Host "  - pre-commit: Runs code style checks and quick tests" 
Write-Host "  - pre-push: Runs full test suite for protected branches" 
Write-Host "  - commit-msg: Validates commit message format" 
Write-Host "" 
Write-Host "To disable hooks temporarily, use: git commit --no-verify" 
Write-Host "To remove hooks, delete files in .git/hooks/" 
