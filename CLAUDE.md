# CS 124 Independent Project

This is Kai's independent Android app project for CS 124 at the University of Illinois.

## Getting Started

Before writing any code, have a brief conversation with the student to set up the project:

1. **Understand the vision:** Ask the student to describe what they want their app to do in their own words. Read PLAN.md for the formal plan, but let the student tell you what excites them about it.

2. **Collaboration style:** Explain that there's a spectrum of ways to work together, and ask how they'd like to collaborate:
   - **Conversational:** You do most of the coding, the student guides direction and reviews results. Good for students who want to focus on the big picture.
   - **Explained:** You write the code but explain what you're doing and why as you go. Good for students who want to learn by watching and understanding.
   - **Guided:** You involve the student more directly — ask them to make decisions about implementation details, suggest they try writing parts themselves, and coach them through problems. Good for students who want hands-on practice.

   Record their preference below. Let them know they can change this at any time by just asking.

3. **Read PLAN.md** to understand the full app plan, then start implementing.

## Student Preferences

<!-- Update this section after discussing with the student. It will persist across sessions. -->
- **Language:** Java
- **Collaboration style:** (not yet chosen)

## Android Conventions

- Target SDK: 35, Min SDK: 24
- Use Jetpack libraries (AppCompat, Material Design Components, ConstraintLayout)
- Follow Android naming conventions: `activity_main.xml` for layouts, `MainActivity.java`/`MainActivity.kt` for activities
- Use `res/` directories properly: `layout/`, `values/`, `drawable/`, `mipmap/`
- App package: use `edu.illinois.cs.cs124.ay2026.project`

## Code Quality

Good code is code that's easy to read, easy to change, and hard to break. Lead by example here — just do these things consistently.

- **Formatting**: Follow the language's standard style (Java: Google Java Style; Kotlin: ktlint conventions). Remove trailing whitespace. Keep imports organized. Be consistent.
- **Linting**: Treat IDE warnings as bugs. Don't suppress warnings with annotations — fix the root cause. If a warning is truly a false positive, add a comment explaining why.
- **Naming**: Use descriptive names that reveal intent. `userList` not `ul`. `fetchRecipes()` not `getData()`. Activity and Fragment names should describe their purpose: `RecipeDetailActivity`, not `Activity2`.
- **Dead code**: Remove unused imports, commented-out code, and unreachable branches. Don't leave `// TODO` comments that won't get done — either do it now or delete the comment.
- **Small, meaningful commits**: Each commit should represent one logical change. Write messages that explain *why* — "Add recipe search so users can filter by ingredient" not "update code". Always build successfully before committing.
- **DRY basics**: If the same 5+ lines appear in multiple places, extract a helper method. But don't over-abstract — three similar lines are fine.

## Testing

After implementing a feature, suggest writing at least one test. Frame it as a natural next step: "Let's add a quick test to make sure this keeps working."

- **Test as you build**: Even a simple test like `"RecipeListActivity launches without crashing"` is valuable. One real test beats zero tests.
- **Run before committing**: Always build the project and run tests before committing. If the build breaks, stop and fix it before adding more features. Say: "Let me verify the build passes first."
- **Test user-facing behavior**: Focus tests on what matters to users — does the search return results? Does the back button work? Does the form validate input? Not every internal method needs a test.
- **Regression tests**: When fixing a bug, add a test that would have caught it. "This crashed because the list was empty — let's add a test for the empty case so it doesn't happen again."
- **Android testing basics**: Use AndroidX Test and JUnit 4. Robolectric for unit tests that need Android framework classes. Espresso for UI tests if the student is interested. Keep the test/src structure standard.
- **Don't fake coverage**: A test that asserts `true == true` teaches nothing. Tests should verify actual app behavior.

## Usability

When building a screen, step back and think about it from a user's perspective. Call out usability issues naturally as they come up.

- **Think like a first-time user**: After implementing a screen, ask: "If someone opened this with no context, would they know what to do?" Point out missing labels, confusing layouts, or hidden actions.
- **Handle every state**: Every screen has at least 4 states: loading, empty, populated, and error. Don't just build the happy path. When implementing a list screen, also handle "no items yet" and "failed to load." Say: "Let's also handle what happens when the list is empty."
- **Visible feedback for every action**: Tapping a button should produce visible feedback. Saving should show a confirmation. Loading should show a spinner. Errors should explain what went wrong and what to try.
- **Consistency**: Use the same navigation patterns throughout. If one screen has a toolbar with a back arrow, they all should. If one form shows errors below the field, don't put them in a toast on another form.
- **Touch targets and readability**: Buttons should be at least 48dp. Text should be readable (16sp minimum for body text). Don't put important actions in hard-to-tap corners.

## Security Basics

Light-touch guidance that builds security awareness early.

- **No hardcoded secrets**: Never put API keys, passwords, or tokens directly in source code. Use `local.properties` or `BuildConfig` fields that aren't committed to git. If you spot a hardcoded secret, flag it immediately.
- **Validate input**: Don't trust user input. Check that strings aren't empty before using them, numbers are in expected ranges, and URLs are well-formed. This prevents crashes and unexpected behavior.
- **Use HTTPS**: All network requests should use HTTPS, never HTTP. Android's network security config blocks cleartext by default — don't disable it.
- **Don't log sensitive data**: Avoid logging passwords, tokens, or personal information. Log messages should help with debugging, not expose user data.
- **Store data safely**: Use SharedPreferences for simple settings, Room database for structured data. Don't write sensitive information to plain text files on external storage.

## Development Workflow

- Build and test frequently — run the app in the emulator after each significant change
- Commit and push regularly so your work is saved and session logs are captured
- Your Claude Code session transcripts in `.claude-sessions/` are automatically saved with each commit
- Do NOT modify files in `.claude/`, `.claude-sessions/`, or `.githooks/` — these handle session logging
- **Route all work through this conversation** — even for tasks that happen outside the editor (setting up Firebase, configuring a server, etc.), ask for step-by-step instructions here so the time is logged

## Grading

- Staff will assign you tasks during class. Check your current task on the /MP page at cs124.org.
- When you've made progress on a task, click "Review This Activity" on the /MP page to submit for review.
- Grading is generous — the goal is to verify effort and engagement, not perfection.

## Process Guidance

- **Review before committing**: Before each commit, review what changed. Say: "Let me review our changes before committing" and briefly summarize what was modified and why. This catches mistakes and teaches good habits.
- **One feature at a time**: Get one feature working end-to-end (UI → logic → tested → committed) before starting the next. Don't start three features and finish none.
- **Don't ignore errors**: If the build fails or the app crashes, stop everything and fix it. Never pile features on top of a broken foundation. Say: "The build is failing — let's fix this before adding anything new."
- **Understand what happened**: If the student doesn't understand a change, explain it. Even when using the "Conversational" collaboration style, pause occasionally to explain key decisions. Ask: "Does this approach make sense? Want me to explain how the RecyclerView adapter works?"
- **Incremental verification**: After each change, build and run the app. Check that both the new feature works AND existing features still work. Don't wait until the end of a session to discover things are broken.
- **Clean up before stopping**: At the end of a session, make sure the code compiles, tests pass, and everything is committed and pushed. Don't leave the project in a broken state between sessions.
- **If you get stuck**: Describe the problem clearly and include any error messages. Break the problem down into smaller pieces.
