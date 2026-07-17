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

Write-Host "`n========== STEP 1: Create Collections ==========" -ForegroundColor Cyan

# Collection: together_rooms
$roomsBody = @{
    collectionId = "together_rooms"
    name = "Together Rooms"
    permissions = @(
        'read("any")',
        'create("users")',
        'update("users")',
        'delete("users")'
    )
    documentSecurity = $true
    enabled = $true
}
Appwrite-Post "/databases/$dbId/collections" $roomsBody

# Collection: together_presence
$presenceBody = @{
    collectionId = "together_presence"
    name = "Together Presence"
    permissions = @(
        'read("any")',
        'create("users")',
        'update("users")',
        'delete("users")'
    )
    documentSecurity = $true
    enabled = $true
}
Appwrite-Post "/databases/$dbId/collections" $presenceBody

Write-Host "`n========== STEP 2: Create together_rooms Attributes ==========" -ForegroundColor Cyan

# String attributes for together_rooms
$roomStrings = @(
    @{ key="roomCode"; size=20; required=$true },
    @{ key="roomId"; size=50; required=$true },
    @{ key="hostParticipantId"; size=50; required=$true },
    @{ key="shardId"; size=20; required=$false; default="" },
    @{ key="hostAuthUid"; size=50; required=$false; default="" },
    @{ key="status"; size=20; required=$true; default="active" },
    @{ key="mediaId"; size=200; required=$false; default="" },
    @{ key="title"; size=500; required=$false; default="" },
    @{ key="artist"; size=500; required=$false; default="" },
    @{ key="thumbnailUrl"; size=2000; required=$false; default="" },
    @{ key="playbackState"; size=20; required=$false; default="PAUSED" },
    @{ key="issuedByParticipantId"; size=50; required=$false; default="" }
)

foreach ($attr in $roomStrings) {
    $body = @{
        key = $attr.key
        size = $attr.size
        required = $attr.required
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/string" $body
}

# Integer attributes for together_rooms
$roomIntegers = @(
    @{ key="durationSeconds"; required=$false; default=-1 },
    @{ key="schemaVersion"; required=$false; default=1 },
    @{ key="stateVersion"; required=$false; default=0 },
    @{ key="positionMs"; required=$false; default=0 },
    @{ key="createdAtEpochMs"; required=$false; default=0 },
    @{ key="lastActivityAtEpochMs"; required=$false; default=0 },
    @{ key="updatedAtEpochMs"; required=$false; default=0 }
)

foreach ($attr in $roomIntegers) {
    $body = @{
        key = $attr.key
        required = $attr.required
        min = -1
        max = 9999999999999
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/integer" $body
}

# Float attributes for together_rooms
$roomFloats = @(
    @{ key="playbackSpeed"; required=$false; default=1.0 }
)

foreach ($attr in $roomFloats) {
    $body = @{
        key = $attr.key
        required = $attr.required
        min = 0.0
        max = 10.0
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/float" $body
}

# Boolean attributes for together_rooms
$roomBooleans = @(
    @{ key="allowGuestControls"; required=$false; default=$true }
)

foreach ($attr in $roomBooleans) {
    $body = @{
        key = $attr.key
        required = $attr.required
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_rooms/attributes/boolean" $body
}

Write-Host "`n========== STEP 3: Create together_presence Attributes ==========" -ForegroundColor Cyan

# String attributes for together_presence
$presenceStrings = @(
    @{ key="roomId"; size=50; required=$true },
    @{ key="participantId"; size=50; required=$true },
    @{ key="displayName"; size=100; required=$false; default="" },
    @{ key="role"; size=20; required=$true },
    @{ key="authUid"; size=50; required=$false; default="" }
)

foreach ($attr in $presenceStrings) {
    $body = @{
        key = $attr.key
        size = $attr.size
        required = $attr.required
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_presence/attributes/string" $body
}

# Integer attributes for together_presence
$presenceIntegers = @(
    @{ key="joinedAtEpochMs"; required=$false; default=0 },
    @{ key="lastSeenAtEpochMs"; required=$false; default=0 }
)

foreach ($attr in $presenceIntegers) {
    $body = @{
        key = $attr.key
        required = $attr.required
        min = 0
        max = 9999999999999
    }
    if ($attr.ContainsKey("default")) {
        $body["default"] = $attr.default
    }
    Appwrite-Post "/databases/$dbId/collections/together_presence/attributes/integer" $body
}

Write-Host "`n========== STEP 4: Create Indexes ==========" -ForegroundColor Cyan

# Index on roomId for together_rooms
Appwrite-Post "/databases/$dbId/collections/together_rooms/indexes" @{
    key = "idx_roomId"
    type = "key"
    attributes = @("roomId")
    orders = @("ASC")
}

# Index on status for together_rooms
Appwrite-Post "/databases/$dbId/collections/together_rooms/indexes" @{
    key = "idx_status"
    type = "key"
    attributes = @("status")
    orders = @("ASC")
}

# Index on roomId for together_presence (for queries)
Appwrite-Post "/databases/$dbId/collections/together_presence/indexes" @{
    key = "idx_roomId"
    type = "key"
    attributes = @("roomId")
    orders = @("ASC")
}

Write-Host "`n========== DONE ==========" -ForegroundColor Green
Write-Host "Database: echofy"
Write-Host "Collection: together_rooms (with 21 attributes)"
Write-Host "Collection: together_presence (with 7 attributes)"
Write-Host "Indexes: 3 created"
