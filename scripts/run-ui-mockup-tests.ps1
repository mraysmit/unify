<#
  Runs the Unify UI mockup tests in a headless browser. Nothing to install.

  The tests live in ui-mockup/tests.js and also run by opening ui-mockup/index.html?tests in any
  browser. This script is for the terminal and CI: it opens that page in headless Microsoft Edge
  (Google Chrome when Edge is missing) with a throwaway browser profile under
  %TEMP%\unify-mockup-tests, waits for the tagged result line the page writes to the browser log,
  prints a summary and exits with 0 when every test passes, 1 when any test fails, and 2 when the
  run could not complete. The project folder and your own browser data are not touched.

  The tests run on real time. (Virtual time makes the clock jump ahead while the browser is still
  reading a sample file, which fails the file-reload tests for the wrong reason.)

  Usage (from the repository root, in PowerShell):
    .\scripts\run-ui-mockup-tests.ps1                 all tests
    .\scripts\run-ui-mockup-tests.ps1 logic           only the logic tests
    .\scripts\run-ui-mockup-tests.ps1 ui              only the UI workflow tests
    .\scripts\run-ui-mockup-tests.ps1 -All            list every test, not only failures
    .\scripts\run-ui-mockup-tests.ps1 -Keep           keep the browser profile and its log
    .\scripts\run-ui-mockup-tests.ps1 -TimeoutSeconds 600
    .\scripts\run-ui-mockup-tests.ps1 -Browser "C:\path\to\chrome.exe"

  If Windows blocks the script, run it once with
    powershell -ExecutionPolicy Bypass -File .\scripts\run-ui-mockup-tests.ps1
#>
param(
  [ValidateSet('all', 'logic', 'ui')][string]$Group = 'all',
  [switch]$All,
  [switch]$Keep,
  [int]$TimeoutSeconds = 300,
  [string]$Browser
)

$marker = '@@UNIFY-TEST-RESULT@@'
$groupNames = @{ logic = 'Logic'; ui = 'UI workflows' }

$page = Join-Path (Split-Path $PSScriptRoot) 'ui-mockup\index.html'
if (-not (Test-Path $page)) { Write-Error "The mockup was not found at $page"; exit 2 }
$url = ([Uri](Resolve-Path $page).Path).AbsoluteUri + $(if ($Group -eq 'all') { '?tests' } else { "?tests=$Group" })

if (-not $Browser) {
  $Browser = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
  ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
}
if (-not $Browser -or -not (Test-Path $Browser)) { Write-Error 'Neither Microsoft Edge nor Google Chrome was found. Pass -Browser <path>.'; exit 2 }
$browserName = if ($Browser -match 'msedge') { 'Microsoft Edge' } else { 'Google Chrome' }

$work = Join-Path $env:TEMP 'unify-mockup-tests'
New-Item -ItemType Directory -Force $work | Out-Null
$profileDir = Join-Path $work ('profile-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$log = Join-Path $profileDir 'chrome_debug.log'

# The browser keeps its log open while writing, so read it without locking it.
function Read-SharedText([string]$path) {
  try {
    $stream = [IO.File]::Open($path, 'Open', 'Read', 'ReadWrite')
    try { return (New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8)).ReadToEnd() }
    finally { $stream.Dispose() }
  } catch { return '' }
}
function Get-Result {
  if (-not (Test-Path $log)) { return $null }
  $text = Read-SharedText $log
  $at = $text.LastIndexOf($marker)
  if ($at -lt 0) { return $null }
  # The log line is:  ...CONSOLE(n)] "@@UNIFY-TEST-RESULT@@{json}", source: file:///... (n)
  $line = $text.Substring($at + $marker.Length)
  $end = $line.IndexOf('", source:')
  if ($end -lt 0) { return $null }
  try { return $line.Substring(0, $end) | ConvertFrom-Json } catch { return $null }
}
function Stop-TestBrowser {
  Get-CimInstance Win32_Process -Filter "Name='msedge.exe' OR Name='chrome.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine.Contains($profileDir) } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}

Write-Host ("Unify UI mockup tests - {0} - {1}" -f $browserName, $(if ($Group -eq 'all') { 'all tests' } else { $groupNames[$Group] + ' only' }))
$started = Get-Date
$process = Start-Process -FilePath $Browser -WindowStyle Hidden -PassThru -ArgumentList @(
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--disable-background-timer-throttling', '--disable-renderer-backgrounding',
  '--disable-backgrounding-occluded-windows', '--window-size=1600,1000',
  '--enable-logging', '--v=0', "--user-data-dir=`"$profileDir`"", "`"$url`"")

$result = $null
try {
  $deadline = $started.AddSeconds($TimeoutSeconds)
  while (-not $result -and (Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 500
    $result = Get-Result
    if (-not $result -and $process.HasExited -and -not (Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and $_.CommandLine.Contains($profileDir) })) { break }
  }
} finally {
  Stop-TestBrowser
  Start-Sleep -Milliseconds 500
  if (-not $Keep) { Remove-Item -Recurse -Force $profileDir -ErrorAction SilentlyContinue }
}
$elapsed = [int]((Get-Date) - $started).TotalSeconds

if (-not $result) {
  Write-Host ("NO RESULT after {0}s: the page did not finish (it crashed, could not load, or ran out of time; see -TimeoutSeconds)." -f $elapsed) -ForegroundColor Red
  if ($Keep) { Write-Host "Browser log kept at $log" }
  exit 2
}

# ForEach-Object unrolls the array (Windows PowerShell 5.1 passes it on as a single object).
$results = @($result.results | ForEach-Object { $_ })
foreach ($key in @('logic', 'ui')) {
  $inGroup = @($results | Where-Object { $_.group -eq $key })
  if (-not $inGroup.Count) { continue }
  $bad = @($inGroup | Where-Object { -not $_.ok })
  $label = '{0,-14}' -f $groupNames[$key]
  if ($bad.Count) { Write-Host ("{0}FAIL  {1} of {2} passed" -f $label, ($inGroup.Count - $bad.Count), $inGroup.Count) -ForegroundColor Red }
  else { Write-Host ("{0}ok    {1} passed" -f $label, $inGroup.Count) -ForegroundColor Green }
  foreach ($test in $(if ($All) { $inGroup } else { $bad })) {
    if ($test.ok) { Write-Host ("      PASS {0}  ({1} ms)" -f $test.name, $test.ms) -ForegroundColor DarkGray }
    else {
      Write-Host ("      FAIL {0}" -f $test.name) -ForegroundColor Yellow
      foreach ($messageLine in ($test.message -split "`n" | Select-Object -First 6)) {
        Write-Host ("           {0}" -f $messageLine) -ForegroundColor Yellow
      }
    }
  }
}

Write-Host ''
Write-Host ("{0} passed, {1} failed of {2}  ({3}s in the browser, {4}s in all)" -f $result.passed, $result.failed, $result.total, $result.seconds, $elapsed) -ForegroundColor $(if ($result.failed) { 'Red' } else { 'Green' })
if ($Keep) { Write-Host "Browser profile and log kept in $profileDir" }
exit $(if ($result.failed) { 1 } else { 0 })
