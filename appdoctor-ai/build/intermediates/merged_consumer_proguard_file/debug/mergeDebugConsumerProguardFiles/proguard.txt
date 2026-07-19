# AppDoctor AI consumer ProGuard/R8 rules.
# The AI plugin is discovered via java.util.ServiceLoader.
-keep class com.appdoctor.ai.AppDoctorAiPluginFactory { <init>(); }
