Write-Host "✅ Starting test script for label functionality..."

# Register Alice
$responseAlice = Invoke-RestMethod -Uri "http://localhost:3000/api/users" `
  -Method POST `
  -Headers @{ "Content-Type" = "application/json" } `
  -Body (@{ email = "alice@example.com"; password = "1234"; firstName = "Alice"; lastName = "Wonder" } | ConvertTo-Json)
Write-Host "✅ Registered Alice"

# Register Bob
$responseBob = Invoke-RestMethod -Uri "http://localhost:3000/api/users" `
  -Method POST `
  -Headers @{ "Content-Type" = "application/json" } `
  -Body (@{ email = "bob@example.com"; password = "1234"; firstName = "Bob"; lastName = "Builder" } | ConvertTo-Json)
Write-Host "✅ Registered Bob"

# Authenticate
$tokenAlice = (Invoke-RestMethod -Uri "http://localhost:3000/api/tokens" `
  -Method POST `
  -Headers @{ "Content-Type" = "application/json" } `
  -Body (@{ email = "alice@example.com"; password = "1234" } | ConvertTo-Json)).token
Write-Host "✅ Alice authenticated"

# Create label
Invoke-RestMethod -Uri "http://localhost:3000/api/labels" `
  -Method POST `
  -Headers @{ "Content-Type" = "application/json" } `
  -Body (@{ name = "important" } | ConvertTo-Json)
Write-Host "✅ Created label 'important'"

# Create mail
$mailBody = @{
    sender = "alice@example.com"
    recipient = "bob@example.com"
    subject = "Test Mail"
    content = "Hey Bob, this is a test from Alice!"
    labels = @("important")
}
Invoke-RestMethod -Uri "http://localhost:3000/api/mails" `
  -Method POST `
  -Headers @{
    "Authorization" = "Bearer $tokenAlice"
    "Content-Type" = "application/json"
  } `
  -Body ($mailBody | ConvertTo-Json -Depth 2)
Write-Host "✅ Mail with label sent"

Write-Host "🎉 All tests completed successfully!"
