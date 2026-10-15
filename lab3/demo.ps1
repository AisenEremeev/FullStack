param([string]$BaseUrl = 'http://localhost:8083')
$ErrorActionPreference = 'Stop'
$secure = $false
$authHeaders = @{}

function Request([string]$Method, [string]$Path, [int]$Expected, [string]$Body = '', [hashtable]$Headers = @{}) {
    $options = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $Headers; UseBasicParsing = $true; ErrorAction = 'Stop' }
    if ($Body) { $options.ContentType = 'application/json'; $options.Body = $Body }
    try {
        $response = Invoke-WebRequest @options
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if (-not $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = ''
    }
    if ($status -ne $Expected) { throw "$Method $Path expected $Expected, received $status" }
    Write-Host "PASS $Method $Path -> $status"
    if ($content) { return ($content | ConvertFrom-Json) }
}

if ($secure) {
    Request 'GET' '/api/customers' 401 | Out-Null
    $username = 'demo_' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $password = [Guid]::NewGuid().ToString('N') + '!aA1'
    $credentials = @{ username = $username; password = $password } | ConvertTo-Json
    Request 'POST' '/api/auth/register' 201 $credentials | Out-Null
    Request 'POST' '/api/auth/register' 409 $credentials | Out-Null
    $auth = Request 'POST' '/api/auth/login' 200 $credentials
    $authHeaders = @{ Authorization = "Bearer $($auth.accessToken)" }
    $me = Request 'GET' '/api/auth/me' 200 '' $authHeaders
    if ($me.username -ne $username) { throw 'Unexpected identity' }
    $wrongCredentials = @{ username = $username; password = 'Incorrect-password-123' } | ConvertTo-Json
    Request 'POST' '/api/auth/login' 401 $wrongCredentials | Out-Null
    Request 'GET' '/api/customers' 401 '' @{ Authorization = 'Bearer invalid.token.value' } | Out-Null
}

$customer = $null
try {
    $customer = Request 'POST' '/api/customers' 201 '{"firstName":"Ada","lastName":"Lovelace"}' $authHeaders
    $read = Request 'GET' "/api/customers/$($customer.id)" 200 '' $authHeaders
    if ($read.firstName -ne 'Ada') { throw 'Created record differs' }
    $found = @(Request 'GET' '/api/customers?lastName=LOVELACE' 200 '' $authHeaders)
    if ($customer.id -notin $found.id) { throw 'Search did not return created record' }
    Request 'PUT' "/api/customers/$($customer.id)" 200 '{"firstName":"Grace","lastName":"Hopper"}' $authHeaders | Out-Null
    $updated = Request 'GET' "/api/customers/$($customer.id)" 200 '' $authHeaders
    if ($updated.lastName -ne 'Hopper') { throw 'Update was not saved' }
    Request 'POST' '/api/customers' 400 '{}' $authHeaders | Out-Null
} finally {
    if ($customer) { Request 'DELETE' "/api/customers/$($customer.id)" 204 '' $authHeaders | Out-Null }
}
Request 'GET' "/api/customers/$($customer.id)" 404 '' $authHeaders | Out-Null
Write-Host 'All demo checks passed.'
