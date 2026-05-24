$f = 'src/test/java/com/calculator/core/ExpressionEvaluatorTest.java'
$b = [System.IO.File]::ReadAllBytes($f)
if ($b.Length -ge 3) { Write-Host ($b[0].ToString('X2') + ' ' + $b[1].ToString('X2') + ' ' + $b[2].ToString('X2')) }
if ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) {
    Write-Host 'BOM detected'
    $text = Get-Content -Raw -Path $f
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    [System.IO.File]::WriteAllBytes($f,$bytes)
    Write-Host 'BOM stripped'
} else { Write-Host 'No BOM' }

Set-Location 'D:\claude\shiyan'
$env:JAVA_HOME='D:\Java\jdk\jdk-21_windows-x64_bin\jdk-21.0.10'
.\mvnw.cmd clean test-compile
.\mvnw.cmd test
