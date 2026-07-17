#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <sys/types.h>
#include <sys/wait.h>
#include <csignal>

#define TAG "NekoDaemon"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static volatile bool g_running = true;
static time_t g_startTime = 0;

static void handle_signal(int sig) {
    if (sig == SIGTERM || sig == SIGINT) {
        g_running = false;
        LOGI("Signal %d received, shutting down", sig);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_io_tl_nekopanel_service_NekoDaemon_startDaemon(
    JNIEnv* env, jclass /*cls*/, jstring apkPath, jstring nativeLibDir) {

    const char* apk = env->GetStringUTFChars(apkPath, nullptr);
    const char* libDir = env->GetStringUTFChars(nativeLibDir, nullptr);

    LOGI("Starting NekoPanel daemon (apk=%s, libDir=%s)", apk, libDir);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("Fork failed");
        env->ReleaseStringUTFChars(apkPath, apk);
        env->ReleaseStringUTFChars(nativeLibDir, libDir);
        return;
    }

    if (pid > 0) {
        // Parent process: return to Java
        LOGI("Daemon forked with PID %d", pid);
        env->ReleaseStringUTFChars(apkPath, apk);
        env->ReleaseStringUTFChars(nativeLibDir, libDir);
        return;
    }

    // Child process (daemon)
    setsid();
    signal(SIGTERM, handle_signal);
    signal(SIGINT, handle_signal);

    g_startTime = time(nullptr);
    const char* pkg = "io.tl.nekopanel";
    char cmd[512];

    while (g_running) {
        snprintf(cmd, sizeof(cmd),
            "am start-foreground-service -n %s/.service.DataDaemonService >/dev/null 2>&1",
            pkg);
        int ret = system(cmd);
        if (ret != 0) {
            LOGI("am start-foreground-service returned %d", ret);
        }
        sleep(30);
    }

    LOGI("Daemon exiting");
    _exit(0);
}

extern "C" JNIEXPORT void JNICALL
Java_io_tl_nekopanel_service_NekoDaemon_stopDaemon(JNIEnv* env, jclass /*cls*/) {
    LOGI("Stop daemon requested");
    g_running = false;
}

extern "C" JNIEXPORT jint JNICALL
Java_io_tl_nekopanel_service_NekoDaemon_nativeGetUptime(JNIEnv* env, jclass /*cls*/) {
    if (g_startTime == 0) return 0;
    return static_cast<jint>(time(nullptr) - g_startTime);
}
