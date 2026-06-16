#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#define LOG_TAG "NativeTtsBridge"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef jlong (*PFN_newFromFile)(JNIEnv*, jobject, jobject);
typedef void  (*PFN_delete)(JNIEnv*, jobject, jlong);
typedef jint  (*PFN_getSampleRate)(JNIEnv*, jobject, jlong);
typedef jobject (*PFN_generateWithCallbackImpl)(JNIEnv*, jobject, jlong, jstring, jint, jfloat, jobject);

static PFN_newFromFile pfnNewFromFile = nullptr;
static PFN_delete pfnDelete = nullptr;
static PFN_getSampleRate pfnGetSampleRate = nullptr;
static PFN_generateWithCallbackImpl pfnGenerate = nullptr;

static bool resolveSymbols() {
    if (pfnNewFromFile) return true;

    // Re-open with RTLD_GLOBAL so dlsym(RTLD_DEFAULT) can find the symbols
    dlopen("libsherpa-onnx-jni.so", RTLD_NOW | RTLD_GLOBAL);

    pfnNewFromFile = (PFN_newFromFile) dlsym(RTLD_DEFAULT, "Java_com_k2fsa_sherpa_onnx_OfflineTts_newFromFile");
    pfnDelete = (PFN_delete) dlsym(RTLD_DEFAULT, "Java_com_k2fsa_sherpa_onnx_OfflineTts_delete");
    pfnGetSampleRate = (PFN_getSampleRate) dlsym(RTLD_DEFAULT, "Java_com_k2fsa_sherpa_onnx_OfflineTts_getSampleRate");
    pfnGenerate = (PFN_generateWithCallbackImpl) dlsym(RTLD_DEFAULT, "Java_com_k2fsa_sherpa_onnx_OfflineTts_generateWithCallbackImpl");

    if (!pfnNewFromFile || !pfnDelete || !pfnGetSampleRate || !pfnGenerate) {
        LOGE("Failed to resolve JNI symbols: newFromFile=%p delete=%p getSampleRate=%p generate=%p",
             pfnNewFromFile, pfnDelete, pfnGetSampleRate, pfnGenerate);
        return false;
    }
    return true;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeCreate(
    JNIEnv* env, jclass /*thiz*/, jobject config) {

    if (!resolveSymbols()) return 0;

    jlong ptr = pfnNewFromFile(env, nullptr, config);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return 0;
    }
    return ptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeDestroy(
    JNIEnv* env, jclass /*thiz*/, jlong ptr) {
    if (!pfnDelete || ptr == 0) return;
    pfnDelete(env, nullptr, ptr);
    if (env->ExceptionCheck()) env->ExceptionClear();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeSampleRate(
    JNIEnv* env, jclass /*thiz*/, jlong ptr) {
    if (!pfnGetSampleRate || ptr == 0) return 0;
    jint sr = pfnGetSampleRate(env, nullptr, ptr);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return 0;
    }
    return sr;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeGenerate(
    JNIEnv* env, jclass /*thiz*/, jlong ptr, jstring jText, jint sid, jfloat speed) {
    if (!pfnGenerate || ptr == 0) return nullptr;

    jobject audio = pfnGenerate(env, nullptr, ptr, jText, sid, speed, nullptr);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    if (!audio) return nullptr;

    jclass generatedAudioCls = env->GetObjectClass(audio);
    if (!generatedAudioCls) {
        env->DeleteLocalRef(audio);
        return nullptr;
    }

    jfieldID samplesFid = env->GetFieldID(generatedAudioCls, "samples", "[F");
    env->DeleteLocalRef(generatedAudioCls);
    if (!samplesFid) {
        env->DeleteLocalRef(audio);
        return nullptr;
    }

    jfloatArray samples = (jfloatArray) env->GetObjectField(audio, samplesFid);
    if (!samples) {
        env->DeleteLocalRef(audio);
        return nullptr;
    }

    jsize len = env->GetArrayLength(samples);
    jfloatArray result = env->NewFloatArray(len);
    if (result) {
        jfloat* buf = env->GetFloatArrayElements(samples, nullptr);
        env->SetFloatArrayRegion(result, 0, len, buf);
        env->ReleaseFloatArrayElements(samples, buf, JNI_ABORT);
    }

    env->DeleteLocalRef(samples);
    env->DeleteLocalRef(audio);
    return result;
}
