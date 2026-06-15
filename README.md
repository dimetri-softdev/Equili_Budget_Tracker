# Equili - Budget Tracker & Financial Planner

Equili is a modern, gamified personal finance application for Android designed to help users track their spending, set financial goals, and visualize their financial health through interactive analytics.

## 📺 Video Overview
Watch the full demonstration of the app here: [Equili App Overview](https://youtu.be/XFsjSuqLHWY?si=8w56K7M61A-ceEjG)

## 👥 Team Members
- **DIMETRI PETES**
- **GARREN GABRON**
- **NELSON DE VOS**
- **SILINDOKUHLE GQUKANI**

## 🌟 Advanced Features (Original Contributions)
The following features are our own ideas:

- **🔐 Advanced Biometric Security:** A secure login system utilizing Fingerprint/Face ID. This feature includes hardware support detection, automatic prompts for returning users, and secure account linking. (Developed by Dimetri Petes).
- **🤖 Intelligent AI Financial Advisor:** A custom-built AI engine that analyzes real-time spending data, budget goals, and gamification progress. It provides personalized coaching, spending alerts, and financial status reports via a conversational chat interface. (Developed by Dimetri Petes).

## 🚀 Core Features

- **Personalized Dashboard:** Get an instant overview of your monthly spending, budget progress, and gamification stats.
- **Smart Expense Logging:** Quickly add expenses with titles, amounts, categories, and optional receipt photo attachments.
- **Dynamic Analytics:** Visualize your spending habits with a professional Pie Chart breakdown by category.
- **Gamification System:** Earn XP for logging expenses and setting goals. Level up and maintain daily streaks to stay motivated.
- **Custom Categories:** Create and manage your own spending categories to fit your lifestyle.
- **Monthly Goals:** Set minimum and maximum spending targets to keep your budget on track.
- **Historical Records:** A full, filterable list of all past transactions with the ability to edit or delete entries.
- **Daily Spending Allowance:** Dynamically calculates how much you can spend today based on your remaining monthly budget.
- **Cloud Synchronization:** Powered by Firebase for real-time data sync across devices and secure authentication.

## 🛠️ Tech Stack

- **Language:** [Kotlin 2.4.0](https://kotlinlang.org/)
- **Database:** [Firebase Realtime Database](https://firebase.google.com/products/realtime-database) & [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Auth:** Firebase Authentication
- **Architecture:** MVVM (Model-View-ViewModel)
- **UI Components:** [Material Design 3](https://m3.material.io/), ViewBinding, Glassmorphism Design
- **Charts:** [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

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

- **[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart):** Generating spending breakdown charts.
- **[Firebase](https://firebase.google.com/):** Cloud data management and authentication.
- **[Android Biometric Library](https://developer.android.com/training/sign-in/biometric-auth):** For the original biometric integration.

---
*Developed as part of a personal finance education initiative.*
