# AppDoctor database consumer ProGuard/R8 rules.
# The database plugin is discovered via java.util.ServiceLoader.
-keep class com.appdoctor.database.AppDoctorDatabasePluginFactory { <init>(); }
