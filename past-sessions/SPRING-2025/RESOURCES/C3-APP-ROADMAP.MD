# C3-CRUNCH-LABS-PORTFOLIO

---

## Java Fundamentals & OOP Overview

### Java Basics
- **Variables:** `int age = 20;` | `String name = "Arnold";`
- **Functions:** `public int add(int a, int b) { return a + b; }`
- **Loops:** `for`, `while`, `do-while`
- **return vs print:**
  - **return:** Sends value to caller.
  - **print:** Displays output.

### OOP Principles
- **Encapsulation:** Protect data using private fields + getters/setters.
- **Inheritance:** Reuse code (`Dog extends Animal`).
- **Polymorphism:** One method, many forms.
- **Abstraction:** Hide complexity using interfaces/abstract classes.

### Java Libraries
- Key packages: `java.lang`, `java.util`, `java.io`.

---

## Agile Methodology

### Definition
Agile is a flexible, iterative approach to project management and software development, focused on delivering small, incremental changes quickly.

### Key Components
- **Sprints:** Short development cycles (1-4 weeks).
- **Scrum Meetings:** Daily check-ins for progress updates.
- **Backlog:** A prioritized list of tasks and features.
- **User Stories:** Simple descriptions of features from the user’s perspective.
- **Retrospectives:** Meetings at the end of sprints to improve processes.

### Importance
- **Adaptability:** Quickly responds to changes in requirements.
- **Collaboration:** Encourages teamwork and continuous feedback.
- **Efficiency:** Delivers working software faster.

---

## JavaScript & React Native for Frontend Development

### JavaScript
Core programming language for building interactive web applications.

### React Native
A framework for building mobile apps using JavaScript, enabling cross-platform development for iOS and Android with a shared codebase.

### Use Cases of Expo Go
- **Rapid Prototyping:** Quickly build and test app features.
- **Collaborative Development:** Share QR codes with teammates or clients to review progress.
- **Simplified Testing:** Instantly test apps on multiple devices without platform-specific builds.

### Why Use Expo Go?
- **No Native Builds Needed:** Develop and test your app without building an APK or IPA.
- **Cross-Platform Development:** Works for both iOS and Android devices with a single codebase.
- **Live Reloading:** Automatically reloads the app when code changes are saved.
- **Access to Expo SDK:** Provides pre-built libraries for common tasks like camera access, location services, push notifications, and more.

### Overview of Expo Go
Expo Go is a mobile app that allows developers to preview and test React Native apps directly on their physical devices without needing to build or install the app manually. It simplifies the development process by offering a fast and convenient way to see changes in real-time.

---

## React Native Components and Firebase Authentication

### Components
- Use `TextInput` for email/password fields and `Button` for login/signup actions.

### Firebase Authentication
- **Install Firebase.**
- Use Firebase's `signInWithEmailAndPassword` method.

### Firebase Benefits
- Secure and scalable authentication system.
- Supports email/password, Google, Facebook, and other providers.
- Real-time database integration for user-specific data.

---

## Database Management in React Native

### Options
- **SQLite:** Local relational database, great for offline storage.
- **Firebase Realtime Database:** Cloud-hosted, provides real-time sync.
- **Realm:** Fast, object-based local database for large datasets.

### Common Use Cases
- Storing user preferences/settings.
- Caching data for offline functionality.
- Managing app-specific data (e.g., notes, to-do lists).

### Benefits of Using UseContext & AsyncStorage
- **UseContext:** Centralizes app-wide state (e.g., auth, theme). Makes state sharing easier across components.
- **AsyncStorage:** Provides persistent data storage even when the app is closed. Ideal for small amounts of data like tokens, user preferences, etc.

---

## Strapi: A Headless CMS

### What is Strapi?
Strapi is a "headless" Content Management System (CMS) that simplifies building APIs with a user-friendly admin panel to manage content.

### Why Use Strapi?
- Ready-to-use admin interface.
- Customizable API endpoints.
- Built-in user authentication.
- Database integrations out of the box.
- RESTful API and GraphQL support.

### Creating an API with Strapi
1. Access the admin panel (typically `http://localhost:1337/admin`).
2. Navigate to "Content-Types Builder."
3. Create your data structure (collections, fields).
4. Set permissions in `Settings > Users & Permissions`.

---

## Key FlatList and ScrollView Properties

### FlatList Properties
- **horizontal:** Enables horizontal scrolling.
- **pagingEnabled:** Snaps to full-width items.
- **showsHorizontalScrollIndicator:** Hides/shows scroll bar.
- **snapToInterval:** Controls sliding snap points.

### ScrollView Properties
- **contentContainerStyle:** Styles the content container.
- **showsVerticalScrollIndicator:** Controls scroll bar visibility.

---

## Best Practices

- Always include loading states during API calls.
- Implement proper error handling.
- Use environment variables for API URLs.
- Consider implementing request caching.
- Add proper validation before sending data.
- Include user feedback for successful/failed operations.

---

## APK Location & Testing

### Debug APK Location
`android/app/build/outputs/apk/debug/app-debug.apk`

### Release APK Location
`android/app/build/outputs/apk/release/app-release.apk`

### Testing Steps
1. Install on Android device.
2. Test all functionality.
3. Verify permissions.
4. Check app performance.
