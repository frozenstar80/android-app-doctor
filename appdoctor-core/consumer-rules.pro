# AppDoctor core consumer ProGuard/R8 rules.
#
# AppDoctor is a debug-only tool. The recommended way to strip it from release
# builds entirely is to use `debugImplementation` for the artifacts (see README).
#
# The UI module is discovered reflectively by the core module, so if you *do*
# ship it to release and rely on runtime gating, keep the entry points.
-keep class com.appdoctor.core.AppDoctor { public *; }
-keep class com.appdoctor.core.overlay.OverlayFactory { *; }
-keep class * implements com.appdoctor.core.overlay.OverlayFactory { <init>(); }
