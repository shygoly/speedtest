# Consumer proguard rules for core module
# These rules will be applied to projects that consume this library

# Keep all public APIs of the core module
-keep public class com.swiftest.core.** {
    public *;
}

# Keep callback interfaces - essential for library users
-keep interface com.swiftest.core.interfaces.** {
    *;
}

# Keep all model classes and their fields
-keep class com.swiftest.core.models.** {
    <fields>;
    <methods>;
}