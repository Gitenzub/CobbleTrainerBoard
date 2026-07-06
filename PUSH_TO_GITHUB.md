# Push to GitHub

## Option A — with GitHub CLI

From this folder:

```powershell
git init
git add .
git commit -m "Initial public release"
gh repo create CobbleTrainerBoard --public --source=. --remote=origin --push
```

For a private repository, replace `--public` with `--private`.

## Option B — without GitHub CLI

1. Create an empty repository on GitHub named `CobbleTrainerBoard`.
2. Do not add a README, license, or `.gitignore` on GitHub, because this folder already contains them.
3. Copy the repository URL.
4. Run:

```powershell
git init
git add .
git commit -m "Initial public release"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/CobbleTrainerBoard.git
git push -u origin main
```

## Useful follow-up commands

```powershell
git status
git log --oneline --max-count=5
git remote -v
```

## Before pushing

Run this check to make sure no private nickname/path slipped in:

```powershell
Get-ChildItem -Recurse -File | Select-String -Pattern "PRIVATE_NICKNAME","PRIVATE_HANDLE","PRIVATE_EMAIL","PRIVATE_URL"
```

No result means the obvious private strings were removed.
