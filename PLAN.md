PLAN.md — Bus Schedule & Class Reminder App
1. App Overview
A mobile app for university/college students that lets them store their class timetable and personal bus schedules in one place. The app automatically sends reminder notifications before each class or bus departure, helping students avoid being late. It is designed to work fully offline with no internet connection required.

2. Key Screens / Views
🏠 Home Screen

Displays today's upcoming classes and bus times in chronological order
Shows a countdown or time remaining until the next event
Quick-access button to add a new schedule entry

📅 Class Schedule Screen

List of all saved classes (subject name, time, day of week, room/location)
Tap any class to edit or delete it
"Add Class" button opens a form to input class details
Toggle to enable/disable reminder for each class

🚌 Bus Schedule Screen

List of saved bus routes (route name/number, departure time, days active)
Tap any entry to edit or delete
"Add Bus" button opens a form to input bus details
Toggle to enable/disable reminder for each bus entry

➕ Add / Edit Entry Screen (shared form)

Input fields: title, day(s) of week, time, location (optional), reminder lead time (e.g. 5, 10, 15, 30 mins before)
Save and Cancel buttons

🔔 Notification Settings Screen

Global toggle to enable/disable all reminders
Default reminder lead time setting
Option to set a custom notification sound (optional stretch goal)


3. Core Features

Add, edit, and delete class and bus schedule entries
Day-of-week repeating schedules (e.g. every Monday and Wednesday)
Local push notifications sent X minutes before each event
Today's view showing only the current day's relevant entries sorted by time
Per-entry reminder toggle to mute specific classes or bus routes without deleting them
Persistent local storage so all data survives app restarts


4. Data
What is stored:
FieldTypeDescriptionidInteger (auto)Unique ID for each entrytypeString"class" or "bus"titleStringSubject name or bus route namedaysStringComma-separated days (e.g. "MON,WED,FRI")timeStringHH:MM format (24hr)locationStringRoom number or bus stop (optional)reminder_minutesIntegerHow many minutes before to notifyis_activeBooleanWhether reminder is enabled
Where it is stored:

SQLite local database on the device using Android's built-in Room library
No cloud storage or internet connection needed
Data stays on the user's phone only


5. External Dependencies
DependencyPurposeRequired?Android Room (SQLite)Local data storage✅ Yes — core featureAndroid AlarmManager / WorkManagerScheduling and triggering notifications✅ Yes — core featureAndroid NotificationManagerDisplaying push notifications✅ Yes — core featureTransit/Bus API (e.g. local transit authority)Real-time bus data❌ No — users input times manuallyFirebase / Backend serverCloud sync or multi-user features❌ No — fully offline app

All dependencies are built into the Android SDK or available as standard Jetpack libraries. No external accounts, API keys, or internet access are required to build or run this app.


🗓️ Quick Timeline Reference
PhaseWeeksGoalSetup & PlanningWeek 1–2Android Studio setup, project structure, DB schemaCore BackendWeek 3–4Room database, alarm scheduling logicUI DevelopmentWeek 5–6All screens built and connectedIntegrationWeek 7–8Notifications firing, data persisting correctlyTesting & PolishWeek 9–10Bug fixes, UI cleanup, final demo prep
Team size: 1–3 members | Deadline: End of April