# Builds function-upload.zip for Lambda hoght-s3-api
Set-Location $PSScriptRoot
python -c "import zipfile; z=zipfile.ZipFile('function-upload.zip','w',zipfile.ZIP_DEFLATED); z.write('index.js'); z.write('package.json'); z.close()"
Write-Host "Created function-upload.zip — upload this in AWS Lambda -> Code -> Upload from .zip"
