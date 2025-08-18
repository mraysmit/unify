# Adds Apache License 2.0 headers to Java files in the Unify project (PowerShell)
# Usage: ./add-license-headers.ps1 [-DryRun] [-Force] [-CleanHeaders]

param(
    [switch]$DryRun,
    [switch]$Force,
    [switch]$CleanHeaders
)

$ErrorActionPreference = 'Stop'

$AuthorName = 'Mark Andrew Ray-Smith Cityline Ltd'
$CopyrightYear = (Get-Date).Year

$licenseHeader = @"
/*
 * Copyright $CopyrightYear $AuthorName
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"@

function Has-LicenseHeader([string]$content) {
    return $content -match 'Licensed under the Apache License'
}

function Has-MalformedHeader([string]$content) {
    # Check for the problematic "class: ClassName" pattern
    return $content -match '/\*\*\s*\*\s*class:\s*\w+' -or
           $content -match '/\*\*\s*\*\s*(interface|enum|annotation):\s*\w+'
}

function Remove-MalformedHeaders([string]$content) {
    # Remove malformed JavaDoc headers like "/** *class: ClassName"
    $content = $content -replace '(?s)/\*\*\s*\*\s*(class|interface|enum|annotation):\s*\w+.*?\*/', ''

    # Remove orphaned single-line comments with old file paths
    $content = $content -replace '^\s*//\s*src/main/java/.*\.java\s*$', ''

    # Remove duplicate Apache license headers (keep only the first one)
    $licensePattern = '(?s)/\*\s*\*\s*Copyright.*?Licensed under the Apache License.*?\*/'
    $matches = [regex]::Matches($content, $licensePattern)
    if ($matches.Count -gt 1) {
        # Remove all but the first license header
        for ($i = $matches.Count - 1; $i -gt 0; $i--) {
            $match = $matches[$i]
            $content = $content.Remove($match.Index, $match.Length)
        }
    }

    # Clean up excessive whitespace
    $content = $content -replace '\n\s*\n\s*\n', "`n`n"
    $content = $content.TrimStart()

    return $content
}

function Process-File([string]$filePath) {
    Write-Host "Processing: $filePath" -ForegroundColor Cyan
    if (-not (Test-Path $filePath)) {
        Write-Warning "File not found: $filePath"
        return
    }

    $content = Get-Content -LiteralPath $filePath -Raw
    if ($null -eq $content) { $content = "" }

    $needsCleaning = Has-MalformedHeader $content
    $hasLicense = Has-LicenseHeader $content

    if ($CleanHeaders -and $needsCleaning) {
        Write-Host "  Cleaning malformed headers..." -ForegroundColor Yellow
        $content = Remove-MalformedHeaders $content
        $hasLicense = Has-LicenseHeader $content  # Re-check after cleaning
    }

    if ($hasLicense -and -not $Force) {
        if ($needsCleaning -and -not $CleanHeaders) {
            Write-Host "  Has license but needs cleaning - use -CleanHeaders" -ForegroundColor Yellow
        } else {
            Write-Host "  Skipping - already has license header (use -Force to override)" -ForegroundColor Green
        }
        return
    }

    # Find the insertion point (before package declaration)
    $packageIdx = $content.IndexOf('package ')
    if ($packageIdx -ge 0) {
        $pre = $content.Substring(0, $packageIdx).TrimEnd()
        $post = $content.Substring($packageIdx)

        if (-not [string]::IsNullOrWhiteSpace($pre)) {
            $newContent = $pre + "`n`n" + $licenseHeader + "`n" + $post
        } else {
            $newContent = $licenseHeader + "`n" + $post
        }
    } else {
        $newContent = $licenseHeader + "`n" + $content
    }

    if ($DryRun) {
        Write-Host "  [DryRun] Would update header" -ForegroundColor DarkYellow
        if ($needsCleaning) {
            Write-Host "    - Would clean malformed headers" -ForegroundColor DarkYellow
        }
    } else {
        # Ensure Windows line endings (CRLF) and write without BOM
        $newContent = $newContent -replace "`r`n", "`n" -replace "`n", "`r`n"
        [System.IO.File]::WriteAllText($filePath, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "  Updated" -ForegroundColor Green
        if ($needsCleaning) {
            Write-Host "    - Cleaned malformed headers" -ForegroundColor Green
        }
    }
}

# Find all Java files in repo modules
$javaFiles = Get-ChildItem -Recurse -Include *.java -File | Where-Object { $_.FullName -notmatch "\\target\\" }

Write-Host "Found $($javaFiles.Count) Java files to process" -ForegroundColor Cyan
if ($CleanHeaders) {
    Write-Host "Clean headers mode enabled - will remove malformed headers" -ForegroundColor Yellow
}
if ($DryRun) {
    Write-Host "Dry run mode - no files will be modified" -ForegroundColor Yellow
}
Write-Host ""

foreach ($f in $javaFiles) {
    Process-File -filePath $f.FullName
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green
