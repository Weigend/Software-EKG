//______________________________________________________________________________
//
// The platform part of JNI for 64 bit Windows.
//
// jni.h is the same in every JDK, jni_md.h belongs to the platform of the JDK. A
// cross build on Linux or macOS has no Windows JDK at hand; build.sh takes this
// file then. A build on Windows uses include\win32\jni_md.h of its JDK.
//______________________________________________________________________________

#ifndef WPCACCESS_JNI_MD_H
#define WPCACCESS_JNI_MD_H

#ifndef _WIN64
#error "This jni_md.h is for 64 bit Windows only."
#endif

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
// 64 bit Windows has one calling convention
#define JNICALL

typedef int jint;
typedef long long jlong;
typedef signed char jbyte;

#endif
