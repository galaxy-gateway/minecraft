# Modrinth Client Setup Guide

Install Minecraft 1.21.1 with CC: Tweaked using the Modrinth App.

**Server Address:** `starbase.minecraft.party`

---

## Step 1: Install the Modrinth App

1. Go to https://modrinth.com/app
2. Download for your OS (Windows/macOS/Linux)
3. Install and launch the app

## Step 2: Sign In

1. Click **Sign In** (top right)
2. Sign in with your Microsoft/Minecraft account

## Step 3: Create a Profile

1. Click **+** or **Create Profile**
2. Configure:
   - **Name:** `Starbase` (or any name)
   - **Game Version:** `1.21.1`
   - **Mod Loader:** `Fabric`
3. Click **Create**

## Step 4: Add Mods

With your profile selected:

### Fabric API
1. Click **Add Content** → Search `Fabric API`
2. Install **Fabric API** by FabricMC
3. Select version `0.104.0+1.21.1` or compatible

### CC: Tweaked
1. Search `CC: Tweaked`
2. Install **CC: Tweaked** by SquidDev
3. Select version `1.117.0` for 1.21.1

## Step 5: Play

1. Click **Play** on your profile
2. Once Minecraft loads, go to **Multiplayer**
3. Click **Add Server**:
   - **Name:** `Starbase`
   - **Address:** `starbase.minecraft.party`
4. Join and play!

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Protocol error / packet decode | Mod versions don't match server - use exact versions above |
| Game crashes | Make sure Fabric API is installed (required by CC: Tweaked) |
| Can't connect | Verify server address and that you're on 1.21.1 |

## Required Versions

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Fabric API | 0.104.0+1.21.1 |
| CC: Tweaked | 1.117.0 |
