[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")).Path
$planPath = Join-Path $repoRoot "test\ui-test-plan.md"
$sourcePath = Join-Path $repoRoot "src\main\java"
$buildPath = Join-Path $repoRoot "_temp\ui-test-$PID"

function Assert-Java25 {
    $javaVersion = (& java -version 2>&1 | Select-Object -First 1).ToString()
    $javacVersion = (& javac -version 2>&1 | Select-Object -First 1).ToString()

    if ($javaVersion -notmatch 'version "25\.' -or $javacVersion -notmatch '^javac 25\.') {
        throw "Java 25 is required. Found: $javaVersion; $javacVersion"
    }
}

function Normalize-ActualOutput([string] $output) {
    $lines = $output -replace "`r`n", "`n" -replace "`r", "`n" -split "`n"
    $greetingEnd = [Array]::IndexOf($lines, "What can I do for you?")
    if ($greetingEnd -lt 0) {
        return ($lines | ForEach-Object { $_.TrimEnd() } | Where-Object { $_ -ne "" }) -join "`n"
    }

    $responseLines = if ($greetingEnd + 1 -lt $lines.Count) {
        $lines[($greetingEnd + 1)..($lines.Count - 1)]
    } else {
        @()
    }

    return ($responseLines |
        ForEach-Object { $_.TrimEnd() } |
        Where-Object { $_ -ne "" -and $_ -notmatch '^(?:─|\?){80}$' }) -join "`n"
}

function Normalize-ExpectedOutput([string] $output) {
    return (($output -replace "`r`n", "`n" -replace "`r", "`n").Trim("`n") -split "`n" |
        ForEach-Object { $_.TrimEnd() } |
        Where-Object { $_ -ne "" }) -join "`n"
}

function Run-Ekko([string[]] $inputLines) {
    $processInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = "java"
    $processInfo.ArgumentList.Add("-cp")
    $processInfo.ArgumentList.Add($buildPath)
    $processInfo.ArgumentList.Add("Ekko")
    $processInfo.RedirectStandardInput = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $processInfo.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $processInfo
    [void] $process.Start()

    foreach ($line in $inputLines) {
        $actualLine = if ($line -eq "<blank>") { "" } else { $line }
        $process.StandardInput.WriteLine($actualLine)
    }
    $process.StandardInput.Close()

    $standardOutput = $process.StandardOutput.ReadToEnd()
    $standardError = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "Ekko exited with code $($process.ExitCode):`n$standardError"
    }

    return $standardOutput
}

Assert-Java25
New-Item -ItemType Directory -Force -Path $buildPath | Out-Null

$sources = Get-ChildItem -LiteralPath $sourcePath -Filter "*.java" | ForEach-Object { $_.FullName }
& javac -d $buildPath @sources
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with exit code $LASTEXITCODE."
}

$plan = Get-Content -Raw -LiteralPath $planPath
$casePattern = '(?ms)^###\s+(?<id>\S+)\s+.*?^\*\*Input\*\*\s*```text\s*\n(?<input>.*?)^```\s*.*?^\*\*Expected normalized output\*\*\s*```text\s*\n(?<expected>.*?)^```\s*'
$cases = [regex]::Matches($plan, $casePattern)

if ($cases.Count -eq 0) {
    throw "No UI test cases were found in $planPath."
}

$passed = 0
foreach ($case in $cases) {
    $id = $case.Groups["id"].Value
    $inputText = $case.Groups["input"].Value.Trim("`r", "`n")
    $inputLines = $inputText -replace "`r`n", "`n" -replace "`r", "`n" -split "`n"
    $expected = Normalize-ExpectedOutput $case.Groups["expected"].Value
    $actual = Normalize-ActualOutput (Run-Ekko $inputLines)

    if ($actual -ne $expected) {
        Write-Host "[FAIL] $id"
        Write-Host "Input:"
        Write-Host $inputText
        Write-Host "Expected:"
        Write-Host $expected
        Write-Host "Actual:"
        Write-Host $actual
        exit 1
    }

    $passed++
    Write-Host "[PASS] $id"
    Write-Host "Input:"
    Write-Host $inputText
    Write-Host "Output:"
    Write-Host $actual
}

Write-Host "All $passed UI tests passed."
