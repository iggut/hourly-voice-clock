#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <android/log.h>
#include "sherpa-onnx-c-api.h"

#define LOG_TAG "NativeTtsBridge"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static SherpaOnnxOfflineTts* (*pfnCreate)(const SherpaOnnxOfflineTtsConfig*) = nullptr;
static void (*pfnDestroy)(SherpaOnnxOfflineTts*) = nullptr;
static int32_t (*pfnSampleRate)(const SherpaOnnxOfflineTts*) = nullptr;
static SherpaOnnxGeneratedAudio (*pfnGenerate)(const SherpaOnnxOfflineTts*, const char*, int32_t, float) = nullptr;
static void (*pfnDestroyAudio)(SherpaOnnxGeneratedAudio*) = nullptr;

static bool resolveSymbols() {
    if (pfnCreate) return true;
    // Android loads dependencies with RTLD_LOCAL, so dlsym(RTLD_DEFAULT)
    // cannot see C API symbols. Re-open the library with RTLD_GLOBAL.
    dlopen("libsherpa-onnx-c-api.so", RTLD_NOW | RTLD_GLOBAL);
    dlopen("libsherpa-onnx-cxx-api.so", RTLD_NOW | RTLD_GLOBAL);
    dlopen("libonnxruntime.so", RTLD_NOW | RTLD_GLOBAL);

    pfnCreate = (decltype(pfnCreate)) dlsym(RTLD_DEFAULT, "SherpaOnnxCreateOfflineTts");
    pfnDestroy = (decltype(pfnDestroy)) dlsym(RTLD_DEFAULT, "SherpaOnnxDestroyOfflineTts");
    pfnSampleRate = (decltype(pfnSampleRate)) dlsym(RTLD_DEFAULT, "SherpaOnnxOfflineTtsSampleRate");
    pfnGenerate = (decltype(pfnGenerate)) dlsym(RTLD_DEFAULT, "SherpaOnnxOfflineTtsGenerate");
    pfnDestroyAudio = (decltype(pfnDestroyAudio)) dlsym(RTLD_DEFAULT, "SherpaOnnxDestroyOfflineTtsGeneratedAudio");
    if (!pfnCreate || !pfnDestroy || !pfnSampleRate || !pfnGenerate || !pfnDestroyAudio) {
        LOGE("Failed to resolve C API symbols");
        return false;
    }
    return true;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeCreate(
    JNIEnv* env, jobject /*thiz*/,
    jstring jModelPath,
    jstring jTokensPath,
    jstring jDataDir) {

    if (!resolveSymbols()) return 0;
    if (!jModelPath || !jTokensPath || !jDataDir) return 0;

    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    const char* tokensPath = env->GetStringUTFChars(jTokensPath, nullptr);
    const char* dataDir = env->GetStringUTFChars(jDataDir, nullptr);

    SherpaOnnxOfflineTtsConfig config;
    memset(&config, 0, sizeof(config));

    config.model.vits.model = modelPath;
    config.model.vits.tokens = tokensPath;
    config.model.vits.data_dir = dataDir;
    config.model.vits.length_scale = 1.0f;
    config.model.num_threads = 1;
    config.model.provider = "cpu";
    config.max_num_sentences = 1;

    SherpaOnnxOfflineTts* tts = pfnCreate(&config);

    env->ReleaseStringUTFChars(jModelPath, modelPath);
    env->ReleaseStringUTFChars(jTokensPath, tokensPath);
    env->ReleaseStringUTFChars(jDataDir, dataDir);

    return reinterpret_cast<jlong>(tts);
}

extern "C" JNIEXPORT void JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeDestroy(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr) {
    if (!pfnDestroy || ptr == 0) return;
    pfnDestroy(reinterpret_cast<SherpaOnnxOfflineTts*>(ptr));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeSampleRate(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong ptr) {
    if (!pfnSampleRate || ptr == 0) return 0;
    return pfnSampleRate(reinterpret_cast<SherpaOnnxOfflineTts*>(ptr));
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_hourlyvoiceclock_tts_local_NativeTtsBridge_nativeGenerate(
    JNIEnv* env, jobject /*thiz*/, jlong ptr, jstring jText, jint sid, jfloat speed) {
    if (!pfnGenerate || ptr == 0 || !jText) return nullptr;

    const char* text = env->GetStringUTFChars(jText, nullptr);
    if (!text) return nullptr;
    SherpaOnnxGeneratedAudio audio = pfnGenerate(
        reinterpret_cast<SherpaOnnxOfflineTts*>(ptr), text, sid, speed);
    env->ReleaseStringUTFChars(jText, text);

    if (audio.samples == nullptr || audio.n <= 0) {
        return nullptr;
    }

    jfloatArray result = env->NewFloatArray(audio.n);
    if (result) {
        env->SetFloatArrayRegion(result, 0, audio.n, audio.samples);
    }

    if (pfnDestroyAudio) pfnDestroyAudio(&audio);
    return result;
}
