$ErrorActionPreference = "Continue"

$base = "https://fra.cloud.appwrite.io/v1"
$projectId = "69f0c83d001d9fc244d4"
$apiKey = "standard_661a90e227fdfad3afa5faf5af8a61210d40f0b32fb1edf1421b3c7d588cf62628c733ce032f067bc91d2b38e7c6c73bc0136364e73c878c4e553f61220961d563e9b42e704d1ae9d0ad5f5f3cb75fdf6b3aba0ad06def621f93e5b923caadf613ff26f6285f2aceefc447a61cb7794001266729cc924859350177339b4959da"
$dbId = "echofy"

$headers = @{
    "Content-Type"     = "application/json"
    "X-Appwrite-Project" = $projectId
    "X-Appwrite-Key"   = $apiKey
}

function Appwrite-Post($path, $body) {
    $uri = "$base$path"
    try {
        $resp = Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -Body ($body | ConvertTo-Json -Depth 5)
        Write-Host "OK: $path" -ForegroundColor Green
        return $resp
    } catch {
        $err = $_.ErrorDetails.Message
        if ($err -match "already exists") {
            Write-Host "SKIP (already exists): $path" -ForegroundColor Yellow
        } else {
            Write-Host "FAIL: $path => $err" -ForegroundColor Red
        }
        return $null
    }
}

# hostName
$bodyString = @{
    key = "hostName"
    size = 200
    required = $false
    default = ""
}
Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/string" $bodyString

# requireApproval
$bodyBoolean = @{
    key = "requireApproval"
    required = $false
    default = $false
}
Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/boolean" $bodyBoolean
