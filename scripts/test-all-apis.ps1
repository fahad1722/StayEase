$ErrorActionPreference = 'Stop'

$base = 'http://localhost:8080/api/v1'
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

$ownerEmail = "owner$suffix@stayease.test"
$renterEmail = "renter$suffix@stayease.test"
$tempEmail = "temp$suffix@stayease.test"

$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Name,
        [int]$Expected,
        [int]$Actual,
        [string]$Path
    )

    $results.Add([pscustomobject]@{
        Name     = $Name
        Path     = $Path
        Expected = $Expected
        Actual   = $Actual
        Pass     = ($Expected -eq $Actual)
    })
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [int]$Expected,
        [hashtable]$Headers = @{},
        $Body = $null
    )

    $uri = "$base$Path"
    $params = @{
        Uri                = $uri
        Method             = $Method
        Headers            = $Headers
        SkipHttpErrorCheck = $true
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
        $params.ContentType = 'application/json'
    }

    $response = Invoke-WebRequest @params
    $status = [int]$response.StatusCode

    Add-Result -Name $Name -Expected $Expected -Actual $status -Path $Path

    if ($status -ne $Expected) {
        Write-Host "FAILED: $Name $Method $Path expected=$Expected actual=$status" -ForegroundColor Red
        if ($response.Content) {
            Write-Host $response.Content -ForegroundColor DarkYellow
        }
    } else {
        Write-Host "PASS: $Name ($status)" -ForegroundColor Green
    }

    return $response
}

function Parse-Json {
    param($Response)
    if ($null -eq $Response -or [string]::IsNullOrWhiteSpace($Response.Content)) {
        return $null
    }

    try {
        return $Response.Content | ConvertFrom-Json
    } catch {
        return $null
    }
}

# AUTH
$ownerRegisterResp = Invoke-Api -Name 'Auth Register OWNER' -Method 'POST' -Path '/auth/register' -Expected 200 -Body @{
    name = 'Owner User'
    email = $ownerEmail
    password = 'Owner@12345'
    role = 'OWNER'
}
$ownerRegister = Parse-Json $ownerRegisterResp

$renterRegisterResp = Invoke-Api -Name 'Auth Register RENTER' -Method 'POST' -Path '/auth/register' -Expected 200 -Body @{
    name = 'Renter User'
    email = $renterEmail
    password = 'Renter@12345'
    role = 'RENTER'
}
$renterRegister = Parse-Json $renterRegisterResp

$ownerLoginResp = Invoke-Api -Name 'Auth Login OWNER' -Method 'POST' -Path '/auth/login' -Expected 200 -Body @{
    email = $ownerEmail
    password = 'Owner@12345'
}
$ownerLogin = Parse-Json $ownerLoginResp

$renterLoginResp = Invoke-Api -Name 'Auth Login RENTER' -Method 'POST' -Path '/auth/login' -Expected 200 -Body @{
    email = $renterEmail
    password = 'Renter@12345'
}
$renterLogin = Parse-Json $renterLoginResp

$ownerRefreshResp = Invoke-Api -Name 'Auth Refresh Token OWNER' -Method 'POST' -Path '/auth/refresh-token' -Expected 200 -Body @{
    refreshToken = $ownerLogin.refreshToken
}
$ownerRefresh = Parse-Json $ownerRefreshResp

$ownerAccess = $ownerRefresh.accessToken
$ownerRefreshToken = $ownerRefresh.refreshToken
$renterAccess = $renterLogin.accessToken
$renterRefreshToken = $renterLogin.refreshToken

$ownerAuthHeader = @{ Authorization = "Bearer $ownerAccess" }
$renterAuthHeader = @{ Authorization = "Bearer $renterAccess" }

# PROPERTY
$propertyCreateResp = Invoke-Api -Name 'Property Create (OWNER)' -Method 'POST' -Path '/properties' -Expected 200 -Headers $ownerAuthHeader -Body @{
    title = "Sea View Suite $suffix"
    description = 'Beautiful sea-facing apartment'
    city = 'Dubai'
    pricePerNight = 180.50
    maxGuests = 4
}
$propertyCreate = Parse-Json $propertyCreateResp
$propertyId = $propertyCreate.id

Invoke-Api -Name 'Property Create Forbidden (RENTER)' -Method 'POST' -Path '/properties' -Expected 403 -Headers $renterAuthHeader -Body @{
    title = 'Should Fail'
    description = 'Renter should not create'
    city = 'Dubai'
    pricePerNight = 99.99
    maxGuests = 2
} | Out-Null

Invoke-Api -Name 'Property List Public' -Method 'GET' -Path '/properties' -Expected 200 | Out-Null
Invoke-Api -Name 'Property Get By Id Public' -Method 'GET' -Path "/properties/$propertyId" -Expected 200 | Out-Null
Invoke-Api -Name 'Property My Listings OWNER' -Method 'GET' -Path '/properties/my-listings' -Expected 200 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Property My Listings Forbidden RENTER' -Method 'GET' -Path '/properties/my-listings' -Expected 403 -Headers $renterAuthHeader | Out-Null

Invoke-Api -Name 'Property Update OWNER' -Method 'PUT' -Path "/properties/$propertyId" -Expected 200 -Headers $ownerAuthHeader -Body @{
    title = "Sea View Suite Updated $suffix"
    description = 'Updated description'
    city = 'Dubai'
    pricePerNight = 200.00
    maxGuests = 5
} | Out-Null

# BOOKING
$checkIn = (Get-Date).AddDays(5).ToString('yyyy-MM-dd')
$checkOut = (Get-Date).AddDays(8).ToString('yyyy-MM-dd')

$bookingCreateResp = Invoke-Api -Name 'Booking Create RENTER' -Method 'POST' -Path '/bookings' -Expected 200 -Headers $renterAuthHeader -Body @{
    propertyId = $propertyId
    checkIn = $checkIn
    checkOut = $checkOut
}
$bookingCreate = Parse-Json $bookingCreateResp
$bookingId = $bookingCreate.id

Invoke-Api -Name 'Booking Create Overlap Conflict' -Method 'POST' -Path '/bookings' -Expected 409 -Headers $renterAuthHeader -Body @{
    propertyId = $propertyId
    checkIn = $checkIn
    checkOut = $checkOut
} | Out-Null

Invoke-Api -Name 'Booking Get By Id RENTER' -Method 'GET' -Path "/bookings/$bookingId" -Expected 200 -Headers $renterAuthHeader | Out-Null
Invoke-Api -Name 'Booking Get By Id OWNER' -Method 'GET' -Path "/bookings/$bookingId" -Expected 200 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Booking My Bookings RENTER' -Method 'GET' -Path '/bookings/my-bookings' -Expected 200 -Headers $renterAuthHeader | Out-Null
Invoke-Api -Name 'Booking My Bookings Forbidden OWNER' -Method 'GET' -Path '/bookings/my-bookings' -Expected 403 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Booking By Property OWNER' -Method 'GET' -Path "/bookings/property/$propertyId" -Expected 200 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Booking By Property Forbidden RENTER' -Method 'GET' -Path "/bookings/property/$propertyId" -Expected 403 -Headers $renterAuthHeader | Out-Null

Invoke-Api -Name 'Booking Confirm OWNER' -Method 'PATCH' -Path "/bookings/$bookingId/confirm" -Expected 200 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Booking Confirm Forbidden RENTER' -Method 'PATCH' -Path "/bookings/$bookingId/confirm" -Expected 403 -Headers $renterAuthHeader | Out-Null
Invoke-Api -Name 'Booking Cancel RENTER' -Method 'PATCH' -Path "/bookings/$bookingId/cancel" -Expected 200 -Headers $renterAuthHeader | Out-Null

$booking2Resp = Invoke-Api -Name 'Booking Create RENTER #2' -Method 'POST' -Path '/bookings' -Expected 200 -Headers $renterAuthHeader -Body @{
    propertyId = $propertyId
    checkIn = (Get-Date).AddDays(12).ToString('yyyy-MM-dd')
    checkOut = (Get-Date).AddDays(14).ToString('yyyy-MM-dd')
}
$booking2 = Parse-Json $booking2Resp
$booking2Id = $booking2.id
Invoke-Api -Name 'Booking Cancel OWNER' -Method 'PATCH' -Path "/bookings/$booking2Id/cancel" -Expected 200 -Headers $ownerAuthHeader | Out-Null

# REVIEW
Invoke-Api -Name 'Review Create RENTER (expected business-rule fail)' -Method 'POST' -Path '/reviews' -Expected 403 -Headers $renterAuthHeader -Body @{
    propertyId = $propertyId
    rating = 5
    comment = 'Great place!'
} | Out-Null

Invoke-Api -Name 'Review Get By Property Public' -Method 'GET' -Path "/reviews/property/$propertyId" -Expected 200 | Out-Null
Invoke-Api -Name 'Review Delete Forbidden OWNER' -Method 'DELETE' -Path '/reviews/9999999' -Expected 403 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'Review Delete RENTER NotFound' -Method 'DELETE' -Path '/reviews/9999999' -Expected 404 -Headers $renterAuthHeader | Out-Null

# USER PROFILE
Invoke-Api -Name 'User Profile Get OWNER' -Method 'GET' -Path '/users/profile' -Expected 200 -Headers $ownerAuthHeader | Out-Null
Invoke-Api -Name 'User Profile Update OWNER' -Method 'PUT' -Path '/users/profile' -Expected 200 -Headers $ownerAuthHeader -Body @{
    name = 'Owner Updated'
    password = 'Owner@12345'
} | Out-Null

$tempRegisterResp = Invoke-Api -Name 'Auth Register TEMP user' -Method 'POST' -Path '/auth/register' -Expected 200 -Body @{
    name = 'Temp User'
    email = $tempEmail
    password = 'Temp@12345'
    role = 'RENTER'
}
$tempRegister = Parse-Json $tempRegisterResp
$tempAuthHeader = @{ Authorization = "Bearer $($tempRegister.accessToken)" }
Invoke-Api -Name 'User Profile Delete TEMP' -Method 'DELETE' -Path '/users/profile' -Expected 204 -Headers $tempAuthHeader | Out-Null

# AUTH LOGOUT
Invoke-Api -Name 'Auth Logout OWNER' -Method 'POST' -Path '/auth/logout' -Expected 200 -Headers @{ Authorization = "Bearer $ownerAccess"; 'X-Refresh-Token' = $ownerRefreshToken } | Out-Null
Invoke-Api -Name 'Auth Logout RENTER' -Method 'POST' -Path '/auth/logout' -Expected 200 -Headers @{ Authorization = "Bearer $renterAccess"; 'X-Refresh-Token' = $renterRefreshToken } | Out-Null

# PROPERTY DELETE at the end
Invoke-Api -Name 'Property Delete OWNER' -Method 'DELETE' -Path "/properties/$propertyId" -Expected 204 -Headers $ownerAuthHeader | Out-Null

Write-Host "`n================ API TEST SUMMARY ================" -ForegroundColor Cyan
$results | Sort-Object Name | Format-Table -AutoSize

$failed = $results | Where-Object { -not $_.Pass }
Write-Host ("Total: {0}  Passed: {1}  Failed: {2}" -f $results.Count, ($results.Count - $failed.Count), $failed.Count) -ForegroundColor Cyan

if ($failed.Count -gt 0) {
    exit 1
}

exit 0
