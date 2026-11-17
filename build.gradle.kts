// The loader and the test mod share these plugin versions.
plugins {
    // Formatting and compiler warnings, checked by `check`: https://github.com/Brendan-Black/black-java
    id("black.java-standards") version "1.3.6" apply false
    // NullAway: whatever isn't @Nullable is never null, checked at compile time.
    id("net.ltgt.errorprone") version "5.2.0" apply false
}
