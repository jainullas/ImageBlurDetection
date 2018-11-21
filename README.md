# ImageBlur Detection

To check whether an image is blurred or not in Android, selecting image from Gallery.(_From camera coming soon..._)


OpenCV library is used for Blur detection.

Download OpenCV by selecting a stable version of Android pack from [here](https:opencv.org/releases.html).
Integrating OpenCV to studio-project is quite a long procedure. Please refer this [awesome](https:medium.com/@sukritipaul005/a-beginners-guide-to-installing-opencv-android-in-android-studio-ea46a7b4f2d3) article.


#### APK Size
APK size is the main concern when we want to integrate with OpenCV.
* Debug APK size -  86 MB
* Debug APK size with Progaurd - 84.4 MB
* Debug APK size with Progaurd and R8 - 80 MB
* Debug APK size with Progaurd + R8 + NDK abiFilters(including only _armv7_ and _x86_)  - 24 MB


#### Workaround to minimise apk size
[Credits](https://stackoverflow.com/a/45074929/5785930)
* Splitting apks by including only those architectures that we need to support. Also include universal apk.
```groovy
 splits {
            abi {
                enable true
                reset()
                include "armeabi-v7a","x86"
                universalApk true
            }
        }
 ```
 * Creating an apk according to w.r.t architectures included. Also including universal apk.
 ```groovy
 ext.versionCodes = ['armeabi-v7a': 4, 'x86': 5]
 import com.android.build.OutputFile
 android.applicationVariants.all { variant ->
     variant.outputs.each { output ->
         def abiFilter = output.getFilter(OutputFile.ABI)
         def abiMultiplier = 0
         if (abiFilter != null) {
             abiMultiplier = project.ext.versionCodes.get(abiFilter)
         }
         output.versionCodeOverride = abiMultiplier * 1000 + android.defaultConfig.versionCode
     }
 }
 ```

 *Problem* with this approach is we have to upload all the apks separately for google play console, including univeral apk.

#### Useful info
According to [stats](https://stackoverflow.com/a/33230181/5785930), armv7 is the most commonly used android architectures.