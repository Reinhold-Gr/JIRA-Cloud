git status

$CommitMessage = Read-Host "Commit-Nachricht eingeben"

git add .
git commit -m $CommitMessage

git remote -v
Write-Host ""
Write-Host "Zum Pushen ENTER drücken, zum Abbrechen STRG+C." -ForegroundColor Yellow
Read-Host

git push