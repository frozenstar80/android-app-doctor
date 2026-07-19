# AppDoctor compose consumer ProGuard/R8 rules.
# The compose plugin is discovered via java.util.ServiceLoader.
-keep class com.appdoctor.compose.AppDoctorComposePluginFactory { <init>(); }
