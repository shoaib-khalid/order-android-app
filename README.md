Steps for carrying over changes from Symplified app to Easydukan:

1. Create new intermediate branch from staging or master branch.
2. In app/build.gradle, set the namespace, applicationId, versionCode, and versionName values to match that of the easydukan app
3. In settings.gradle, set rootProject.name to that in the easydukan branch
4. In strings.xml, preserve app_name and welcome_message values to that in the easydukan branch
5. In nav_header.xml and every activity_login.xml file, set the top ImageView src from "sym_white_cloud" to "ic_easy_dukan_logo"
6. In app/java, refactor com.symplified.order package to com.symplified.easydukan
7. Replace files in "src/main/res/mipmap" directory with the one from Easydukan, as well as ic_launcher-playstore.png in "src/main" directory
8. In App.java, Set BASE_URL_PRODUCTION to "https://api.deliverin.pk/".
9. Replace google-services.json file with the one from the EasyDukan branch.
10. Run app and check to make sure everything looks visually consistent.
11. Merge this branch into "staging-easydukan"
12. Delete the intermediate branch
13. When building the app bundle, select "easydukan_release_alias" as the Key Alias