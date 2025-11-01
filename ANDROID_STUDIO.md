# Using the Plugin in Android Studio

## 📍 Where to Find Compose Multiplatform Wizard

The plugin is available through **Actions** in Android Studio:

### Option 1: Welcome Screen (Recommended)
1. Open Android Studio
2. On the Welcome Screen, look for **"Compose Multiplatform Project"** in the quick start section
3. Click it to create a new project

### Option 2: File Menu
1. Open Android Studio
2. Go to **File → New → Compose Multiplatform Project...**
3. The wizard dialog will open with Compose UI

## ⚠️ Why Not in "Phone and Tablet" Section?

The plugin currently uses **Actions** instead of native Android Studio wizard templates because:

1. **Compilation Issue**: AS wizard template API requires Android Studio SDK at compile time
2. **Our Approach**: We compile against IntelliJ Platform (which is available in both IDEA and AS)
3. **Runtime Loading**: The plugin works in both IDEs through optional dependencies

## ✨ Same Compose UI in Both IDEs

The wizard uses **the same Compose UI** in both IntelliJ IDEA and Android Studio:
- Same look and feel
- Same options (Android, iOS, Desktop, Web)
- Same validation
- Single codebase

## 🔮 Future Plans

We may add native AS wizard template integration using:
- Runtime reflection to access AS API
- Dynamic class loading
- Custom class loaders

For now, Actions provide a clean, working solution! 🚀

## 🐛 Troubleshooting

**Q: I don't see the "Compose Multiplatform Project" option**
- Make sure the plugin is installed and enabled
- Restart Android Studio
- Check `Settings → Plugins → Installed` for the plugin

**Q: The wizard doesn't work**
- Check Android Studio version (requires 2025.2.1.7+)
- Look at logs: `Help → Show Log in Finder`

**Q: Can I use it for existing projects?**
- Currently, the wizard creates new projects only
- Module wizard support is planned for future releases

