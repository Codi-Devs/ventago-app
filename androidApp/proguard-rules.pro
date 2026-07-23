# Epson's ePOS SDK samples keep the full com.epson namespace. The public APIs
# call JNI-backed internal packages such as com.epson.epsonio and
# com.epson.eposdevice that may otherwise look unused to R8.
-keep class com.epson.** { *; }
-dontwarn com.epson.**

# Preserve the H10P Binder API boundary that is called by a separate device
# service.
-keep class recieptservice.com.recieptservice.** { *; }
-keep interface recieptservice.com.recieptservice.** { *; }
