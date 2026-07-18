# AppDoctor UI consumer ProGuard/R8 rules.
# Keep the reflectively-loaded factory entry point.
-keep class com.appdoctor.ui.ComposeOverlayFactory { <init>(); }
