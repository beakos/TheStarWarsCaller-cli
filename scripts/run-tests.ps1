param(
    [switch]$Verbose
)

# This script recompiles sources and runs lightweight tests.
# Beginner hint: run it whenever you change JSON files or Java code.
# Easter egg: imagine this as preflight checks before jumping to hyperspace.

$ErrorActionPreference = 'Stop'

Write-Host 'Compiling main sources...'
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null
$sources = Get-ChildItem -Path src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

Write-Host 'Compiling test sources...'
$testOut = Join-Path out 'tests'
New-Item -ItemType Directory -Path $testOut | Out-Null
$testSources = Get-ChildItem -Path src/test/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if ($testSources.Count -gt 0) {
    javac -cp out -d $testOut $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }

    $classPath = ($testOut, (Resolve-Path out)) -join ([IO.Path]::PathSeparator)
    Write-Host 'Running tests...'
    Write-Host "Classpath: $classPath"
    java -cp $classPath com.thestarwarscaller.core.MediaRepositoryTest
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "Tests failed with exit code $exitCode"
    }
} else {
    Write-Host 'No test sources found.'
}

Write-Host 'All tests completed successfully.'
