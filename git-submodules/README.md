# Git Submodules

This directory contains git submodules for the `yral-mobile` repository.

## Cloning with Submodules

### Option 1: Clone with submodules (Recommended)

When cloning `yral-mobile` for the first time, use the `--recurse-submodules` flag to automatically initialize and clone all submodules:

```bash
git clone --recurse-submodules https://github.com/dolr-ai/yral-mobile.git
cd yral-mobile
```

### Option 2: Initialize submodules after cloning

If you've already cloned `yral-mobile` without submodules, initialize and update them:

```bash
git submodule update --init --recursive
```

## Submodules

This directory contains the following submodules:

- **yral-backend-canister** - Backend canister implementation

## Working with Submodules

### Pull latest changes from a submodule

```bash
cd git-submodules/yral-backend-canister
git pull origin main
cd ../..
```

### Update all submodules to latest

```bash
git submodule update --remote --recursive
```

### Commit submodule changes

After updating a submodule, commit the new reference in the parent repository:

```bash
git add git-submodules/
git commit -m "Update submodules"
```
