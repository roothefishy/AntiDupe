# DupeAlerts

**Advanced anti-dupe detection system for Minecraft servers with comprehensive logging, staff management GUI, and intelligent stash detection.**

![Version](https://img.shields.io/badge/version-2.1--SNAPSHOT-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green)
![License](https://img.shields.io/badge/license-MIT-yellow)

## 🚀 Features

### 🔍 Intelligent Dupe Detection
- **Real-time Inventory Monitoring** - Tracks suspicious inventory changes across multiple contexts (hotbar switches, clicks, pickups, drops)
- **Book Dupe Prevention** - Automatically blocks book title exploits
- **Item Frame Exploit Detection** - Identifies rapid item frame interactions (potential dupe method)
- **Crafting Auto-Clicker Detection** - Flags abnormally fast crafting patterns
- **Enderman Interaction Logging** - Monitors suspicious entity manipulation
- **Advanced Threshold System** - Smart detection that adapts to item stack sizes

### 📊 Staff Management GUI
- **Interactive Dupe Logs** (`/dupelogs`)
  - Paginated view of all dupe incidents
  - Detailed log inspection with before/after inventory comparison
  - One-click player banning
  - Log dismissal for false positives
  - Flag system for further investigation
  - Teleport to incident location

- **Stash Detection System** (`/stashes`)
  - Automatic detection of suspicious chest storage
  - Identifies large item hoards (500+ items threshold)
  - Tracks multiple max-stack indicators
  - Location mapping for staff review

### 🛡️ Protection Features
- **Permission-Based Alerts** - Customizable alert system for staff members
- **Bypass Permissions** - Flexible permission structure for trusted players

### 📝 Comprehensive Logging
- **File-Based System** - Persistent logs stored in plugin data folder
- **Automatic Archiving** - Old logs archived after 30 days
- **Detailed Snapshots** - Before/after inventory states captured
- **Location Tracking** - World coordinates logged for each incident
- **Detection Method Tags** - Clear indicators of how dupes were detected

## 📦 Installation

1. Download the latest `DupeAlerts-2.1.jar` from [Releases](../../releases)
2. Place the JAR file in your server's `plugins/` folder
3. Restart your Minecraft server
4. Configure permissions (optional)

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dupelogs` | Open the dupe logs management GUI | `dupealerts.staff` |
| `/stashes` | Open the stash detection GUI | `dupealerts.staff` |

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `dupealerts.staff` | Access to dupe logs, stash finder, and management features | OP |
| `dupealerts.alerts` | Receive real-time dupe alerts in chat | OP |
| `dupealerts.bypass` | Bypass all dupe detection (for trusted staff) | None |

## 🛠️ Configuration

The plugin automatically creates a `config.yml` file on first run.

### Detection Thresholds
- **Inventory Check Cooldown**: 100ms
- **Alert Cooldown**: 5 seconds
- **Item Frame Threshold**: 10 interactions in 10 seconds
- **Crafting Threshold**: 20 crafts in 5 seconds
- **Stash Item Threshold**: 500+ items


## 📋 How It Works

### Detection Methods

1. **Inventory Jump Detection**
   - Monitors item count changes
   - Flags increases beyond normal thresholds (2x max stack size)
   - Context-aware (different triggers for different actions)

2. **Book Title Exploit Prevention**
   - Blocks books with titles longer than 16 characters
   - Prevents crash/dupe exploits
   - Notifies player and staff

3. **Item Frame Spam Detection**
   - Tracks rapid item frame breaking
   - Uses decay system (infractions expire after 10s)
   - Flags sustained rapid interactions

4. **Auto-Clicker Crafting**
   - Monitors crafting speed
   - Decay-based infraction system
   - Identifies inhuman crafting patterns

5. **Stash Detection**
   - Scans chests on close
   - Identifies suspicious hoarding patterns
   - Flags for staff investigation

### Alert System

When a potential dupe is detected:
1. **Instant Notification** - Staff with `dupealerts.alerts` receive chat alerts
2. **Log Creation** - Incident logged with full details
3. **Inventory Snapshot** - Before/after states captured
4. **Location Tracking** - Coordinates saved for investigation
5. **GUI Access** - Staff can review via `/dupelogs`

## 🎯 Use Cases

- **Survival Servers** - Protect economy and gameplay balance
- **Economy Servers** - Prevent item duplication exploits
- **Minigame Servers** - Detect unfair advantage attempts
- **SMP Servers** - Monitor and manage suspicious activity

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**roothefishy**

## 🐛 Bug Reports & Suggestions

Found a bug or have a suggestion? Please open an issue on GitHub!

## ⭐ Support

If you find this plugin useful, please consider giving it a star on GitHub!

---

**Note**: This plugin is designed to detect and alert staff about potential duplication exploits. It does not prevent all dupe methods but provides comprehensive monitoring and management tools for server administrators.
