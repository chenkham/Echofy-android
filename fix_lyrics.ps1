$file = "app\src\main\java\com\Chenkham\Echofy\ui\component\Lyrics.kt"
$content = [System.IO.File]::ReadAllText($file)
$old = "    val isPremium = LocalAdManager.current?.isPremium?.collectAsState()?.value == true`r`n`r`n    val isAppleStyle = playerLayoutStyle == PlayerLayoutStyle.APPLE_MUSIC && isPremium"
$new = "    val isAppleStyle = playerLayoutStyle == PlayerLayoutStyle.APPLE_MUSIC"
$content = $content.Replace($old, $new)
[System.IO.File]::WriteAllText($file, $content)
Write-Host "Done"
