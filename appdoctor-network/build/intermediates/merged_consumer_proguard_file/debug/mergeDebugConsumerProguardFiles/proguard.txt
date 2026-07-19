# AppDoctor network consumer ProGuard/R8 rules.
# The network plugin is discovered via java.util.ServiceLoader.
-keep class com.appdoctor.network.AppDoctorNetworkPluginFactory { <init>(); }
