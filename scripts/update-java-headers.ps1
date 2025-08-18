# Cleans and updates Java file headers with proper license headers only
# This script removes malformed headers and ensures clean Apache license headers
# Usage: ./update-java-headers.ps1 [-DryRun] [-Force]

param(
    [switch]$DryRun,
    [switch]$Force
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
    # Check for problematic patterns
    return $content -match '/\*\*\s*\*\s*(class|interface|enum|annotation):\s*\w+' -or
           $content -match '^\s*//\s*src/main/java/' -or
           ($content -match '/\*\s*\*\s*Copyright.*?Licensed under the Apache License.*?\*/' -and
            ([regex]::Matches($content, '/\*\s*\*\s*Copyright.*?Licensed under the Apache License.*?\*/')).Count -gt 1)
}

function Clean-Headers([string]$content) {
    Write-Verbose "Cleaning headers..."

    # Remove malformed JavaDoc headers with "class: ClassName" pattern
    $content = $content -replace '(?s)/\*\*\s*\*\s*(class|interface|enum|annotation):\s*\w+.*?\*/', ''

    # Remove orphaned single-line comments with old file paths
    $content = $content -replace '(?m)^\s*//\s*src/main/java/.*\.java\s*$', ''

    # Handle duplicate Apache license headers
    $licensePattern = '(?s)/\*\s*\*?\s*Copyright.*?Licensed under the Apache License.*?\*/'
    $matches = [regex]::Matches($content, $licensePattern)

    if ($matches.Count -gt 1) {
        Write-Verbose "Found $($matches.Count) license headers, removing duplicates..."
        # Remove all license headers first
        $content = $content -replace $licensePattern, ''
        # We'll add a clean one back later
    } elseif ($matches.Count -eq 1) {
        # Check if the existing license header is properly formatted
        $existingHeader = $matches[0].Value
        if ($existingHeader -notmatch 'Licensed under the Apache License, Version 2\.0') {
            # Remove malformed license header
            $content = $content -replace [regex]::Escape($existingHeader), ''
        }
    }

    # Clean up excessive whitespace and empty lines
    $content = $content -replace '\n\s*\n\s*\n+', "`n`n"
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

    $hasMalformed = Has-MalformedHeader $content
    $hasLicense = Has-LicenseHeader $content
    $needsUpdate = $hasMalformed -or -not $hasLicense -or $Force

    if (-not $needsUpdate) {
        Write-Host "  Skipping - already clean (use -Force to override)" -ForegroundColor Green
        return
    }

    # Clean existing headers
    if ($hasMalformed) {
        Write-Host "  Cleaning malformed headers..." -ForegroundColor Yellow
        $content = Clean-Headers $content
        $hasLicense = Has-LicenseHeader $content  # Re-check after cleaning
    }

    # Add license header if missing
    if (-not $hasLicense -or $Force) {
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
    } else {
        $newContent = $content
    }

    if ($DryRun) {
        Write-Host "  [DryRun] Would clean and update headers" -ForegroundColor DarkYellow
        if ($hasMalformed) {
            Write-Host "    - Would remove malformed headers" -ForegroundColor DarkYellow
        }
        if (-not $hasLicense) {
            Write-Host "    - Would add license header" -ForegroundColor DarkYellow
        }
    } else {
        # Ensure Windows line endings (CRLF) and write without BOM
        $newContent = $newContent -replace "`r`n", "`n" -replace "`n", "`r`n"
        [System.IO.File]::WriteAllText($filePath, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "  Updated - headers cleaned and standardized" -ForegroundColor Green
        if ($hasMalformed) {
            Write-Host "    - Removed malformed headers" -ForegroundColor Green
        }
        if (-not $hasLicense) {
            Write-Host "    - Added license header" -ForegroundColor Green
        }
    }
}

# Find all Java files in repo modules
$javaFiles = Get-ChildItem -Recurse -Include *.java -File | Where-Object { $_.FullName -notmatch "\\target\\" }

Write-Host "Found $($javaFiles.Count) Java files to process" -ForegroundColor Cyan
if ($DryRun) {
    Write-Host "Dry run mode - no files will be modified" -ForegroundColor Yellow
}
Write-Host ""

$processedCount = 0
$cleanedCount = 0

foreach ($f in $javaFiles) {
    $beforeContent = Get-Content -LiteralPath $f.FullName -Raw
    Process-File -filePath $f.FullName

    if (-not $DryRun) {
        $afterContent = Get-Content -LiteralPath $f.FullName -Raw
        if ($beforeContent -ne $afterContent) {
            $cleanedCount++
        }
    }
    $processedCount++
}

Write-Host ""
Write-Host "Processed $processedCount files" -ForegroundColor Cyan
if (-not $DryRun) {
    Write-Host "Cleaned $cleanedCount files" -ForegroundColor Green
}
Write-Host "Done." -ForegroundColor Green
