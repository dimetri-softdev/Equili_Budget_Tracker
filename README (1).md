# Equili - Budget Tracker & Financial Planner

Equili is a modern, gamified personal finance application for Android designed to help users track their spending, set financial goals, and visualize their financial health through interactive analytics.

## 📺 Video Overview
Watch the full demonstration of the app here: [Equili App Overview](https://youtu.be/XFsjSuqLHWY?si=8w56K7M61A-ceEjG)

## 👥 Team Members
- **DIMETRI PETES**
- **GARREN GABRON**
- **NELSON DE VOS**
- **SILINDOKUHLE GQUKANI**

## 🚀 Features

- **Personalized Dashboard:** Get an instant overview of your monthly spending, budget progress, and gamification stats.
- **Smart Expense Logging:** Quickly add expenses with titles, amounts, categories, and optional receipt photo attachments.
- **Dynamic Analytics:** Visualize your spending habits with a professional Pie Chart breakdown by category.
- **Gamification System:** Earn XP for logging expenses and setting goals. Level up and maintain daily streaks to stay motivated.
- **Custom Categories:** Create and manage your own spending categories to fit your lifestyle.
- **Monthly Goals:** Set minimum and maximum spending targets to keep your budget on track.
- **Historical Records:** A full, filterable list of all past transactions with the ability to edit or delete entries.
- **Secure Local Storage:** All your data is stored securely on your device using the Room Persistence Library.
- **Modern UI:** Features a sleek "Glassmorphism" design with full support for both Light and Dark modes.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **Database:** [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Architecture:** MVVM (Model-View-ViewModel)
- **UI Components:** [Material Design 3](https://m3.material.io/), ViewBinding
- **Charts:** [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- **CI/CD:** GitHub Actions for automated testing and APK builds.

## 🧪 Automated Testing

The project includes unit tests for core logic, such as password complexity validation and email formatting. You can run these tests via:
```bash
./gradlew test
```

## 📦 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ST10375943/Equili_Budget_Tracker.git
   ```
2. **Open in Android Studio:**
   - File > Open > Select the `Equili` folder.
3. **Build the Project:**
   - Wait for Gradle to sync and build the project.
4. **Run on Emulator/Device:**
   - Click the **Run** icon in Android Studio.

## 📚 References & Acknowledgments

This project utilizes several open-source libraries and follows best practices from the Android developer community:

### Libraries
- **[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart):** Used for generating the spending breakdown charts.
- **[Jetpack Room](https://developer.android.com/jetpack/androidx/releases/room):** Local database management.
- **[Material Components for Android](https://github.com/material-components/material-components-android):** UI elements and themes.

### Documentation & Learning Resources
- **[Android Developers Documentation](https://developer.android.com/):** For Room, ViewModel, LiveData, and FileProvider implementations.
- **[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html):** For asynchronous database operations.
- **[Google Material Symbols & Icons](https://fonts.google.com/icons):** For app icons.

---
*Developed as part of a personal finance education initiative.*
