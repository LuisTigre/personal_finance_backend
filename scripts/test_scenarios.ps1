# Config
$KC_URL = "http://localhost:8080"
$APP_URL = "http://localhost:8081"
$REALM = "Persfin"

function Get-AdminToken {
    $body = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = "admin"
        password = "admin"
    }
    $response = Invoke-RestMethod -Uri "$KC_URL/realms/master/protocol/openid-connect/token" -Method Post -Body $body
    return $response.access_token
}

function Create-User {
    param ($token, $username, $email, $password)
    
    # Check if user exists
    $headers = @{ Authorization = "Bearer $token" }
    $users = Invoke-RestMethod -Uri "$KC_URL/admin/realms/$REALM/users?username=$username" -Headers $headers -Method Get
    
    if ($users.Count -gt 0) {
        Write-Host "User $username already exists."
        return
    }

    Write-Host "Creating user $username..."
    $userBody = @{
        username = $username
        email = $email
        enabled = $true
        firstName = $username
        lastName = "User"
        credentials = @(
            @{
                type = "password"
                value = $password
                temporary = $false
            }
        )
    } | ConvertTo-Json -Depth 3

    Invoke-RestMethod -Uri "$KC_URL/admin/realms/$REALM/users" -Headers $headers -Method Post -Body $userBody -ContentType "application/json"
    Write-Host "User $username created."
}

function Get-UserToken {
    param ($email, $password)
    $body = @{
        grant_type = "password"
        client_id = "personal-finance-api"
        client_secret = "personal-finance-secret"
        username = $email
        password = $password
    }
    try {
        $response = Invoke-RestMethod -Uri "$KC_URL/realms/$REALM/protocol/openid-connect/token" -Method Post -Body $body
        return $response.access_token
    } catch {
        Write-Error "Failed to get token for $email. Ensure client 'personal-finance-api' has 'Direct Access Grants' enabled in Keycloak."
        throw $_
    }
}

function Call-Api {
    param ($token, $method, $path, $body=$null)
    $headers = @{ Authorization = "Bearer $token" }
    $uri = "$APP_URL$path"
    
    Write-Host "[$method] $path"
    try {
        if ($body) {
            $json = $body | ConvertTo-Json -Depth 5
            return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers -Body $json -ContentType "application/json"
        } else {
            return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers -ContentType "application/json"
        }
    } catch {
        Write-Host "Error calling API: $_"
        $_.Exception.Response.GetResponseStream() | %{ $reader = New-Object System.IO.StreamReader($_); $reader.ReadToEnd() }
    }
}

# --- MAIN ---

Write-Host "1. Getting Admin Token..."
$adminToken = Get-AdminToken

Write-Host "2. Creating User 'bob'..."
Create-User -token $adminToken -username "bob" -email "bob@example.com" -password "password"

# Alice is already in realm json
Write-Host "3. Getting Tokens for Alice and Bob..."
$aliceToken = Get-UserToken -email "alice@example.com" -password "password"
$bobToken = Get-UserToken -email "bob@example.com" -password "password"

Write-Host "`n--- SCENARIO START ---`n"

# 1. Register users in App
Write-Host "4. Alice logs in (JIT Registration)..."
$alice = Call-Api -token $aliceToken -method "GET" -path "/api/me"
Write-Host "Alice ID: $($alice.id)"

Write-Host "5. Bob logs in (JIT Registration)..."
$bob = Call-Api -token $bobToken -method "GET" -path "/api/me"
Write-Host "Bob ID: $($bob.id)"

# 2. Alice Create Wallet
Write-Host "6. Alice creates a wallet..."
$walletReq = @{
    name = "Holiday Fund"
    currency = "USD"
    initialBalance = 1000.00
}
$wallet = Call-Api -token $aliceToken -method "POST" -path "/api/wallets" -body $walletReq
$walletId = $wallet.id
Write-Host "Wallet Created: $($wallet.name) (ID: $walletId)"

# 3. List Wallets (Verify Alice sees it)
Write-Host "7. Alice lists wallets..."
$aliceWallets = Call-Api -token $aliceToken -method "GET" -path "/api/wallets" # Fixed path from /api/me/wallets to /api/wallets
Write-Host "Alice has $($aliceWallets.Count) wallets."

# 4. Add Bob to Wallet
Write-Host "8. Alice adds Bob to wallet..."
$addMemberReq = @{
    identifier = "bob@example.com"
    role = "WRITER"
}
$updatedWallet = Call-Api -token $aliceToken -method "POST" -path "/api/wallets/$walletId/members" -body $addMemberReq
Write-Host "Bob added. Members count: $($updatedWallet.members.Count)"

# 5. Bob check Wallet
Write-Host "9. Bob checks his wallets..."
$bobWallets = Call-Api -token $bobToken -method "GET" -path "/api/wallets"
Write-Host "Bob has $($bobWallets.Count) wallets."
if ($bobWallets.Count -gt 0) {
    Write-Host "Bob sees: $($bobWallets[0].name)"
} else {
    Write-Error "Bob should see the wallet!"
}

Write-Host "`n--- SCENARIO END ---"
