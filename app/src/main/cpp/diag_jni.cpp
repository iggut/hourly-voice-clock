#include <jni.h>
#include <android/log.h>

#define LOG_TAG "DiagJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_hourlyvoiceclock_NativeDiag_testFieldIds(
    JNIEnv* env, jclass /*cls*/, jobject config) {

    jclass cls = env->GetObjectClass(config);
    if (!cls) {
        LOGE("GetObjectClass returned null");
        return;
    }

    struct FieldTest {
        const char* name;
        const char* sig;
    };

    FieldTest fields[] = {
        {"model", "Lcom/k2fsa/sherpa/onnx/OfflineTtsModelConfig;"},
        {"ruleFsts", "Ljava/lang/String;"},
        {"ruleFars", "Ljava/lang/String;"},
        {"maxNumSentences", "I"},
        {"silenceScale", "F"},
    };

    for (auto& f : fields) {
        jfieldID fid = env->GetFieldID(cls, f.name, f.sig);
        if (fid) {
            LOGI("OK: %s %s", f.name, f.sig);
        } else {
            LOGE("FAIL: %s %s", f.name, f.sig);
            if (env->ExceptionCheck()) {
                jthrowable ex = env->ExceptionOccurred();
                env->ExceptionClear();
                jclass exCls = env->GetObjectClass(ex);
                jmethodID getMessage = env->GetMethodID(exCls, "getMessage", "()Ljava/lang/String;");
                jstring msg = (jstring) env->CallObjectMethod(ex, getMessage);
                const char* cmsg = env->GetStringUTFChars(msg, nullptr);
                LOGE("Exception: %s", cmsg);
                env->ReleaseStringUTFChars(msg, cmsg);
            }
        }
    }

    // Also test nested model class
    jfieldID modelFid = env->GetFieldID(cls, "model", "Lcom/k2fsa/sherpa/onnx/OfflineTtsModelConfig;");
    if (modelFid) {
        jobject model = env->GetObjectField(config, modelFid);
        jclass modelCls = env->GetObjectClass(model);
        FieldTest modelFields[] = {
            {"vits", "Lcom/k2fsa/sherpa/onnx/OfflineTtsVitsModelConfig;"},
            {"numThreads", "I"},
            {"debug", "I"},
            {"provider", "Ljava/lang/String;"},
        };
        for (auto& f : modelFields) {
            jfieldID fid = env->GetFieldID(modelCls, f.name, f.sig);
            if (fid) {
                LOGI("OK: model.%s %s", f.name, f.sig);
            } else {
                LOGE("FAIL: model.%s %s", f.name, f.sig);
                if (env->ExceptionCheck()) env->ExceptionClear();
            }
        }
    }
}
