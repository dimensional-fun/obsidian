rootProject.name = "obsidian"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

include(
    ":protocol",
    //
    ":server-api",
    ":server-impl",
    //
    "connector-api",
    "connector-"
)
