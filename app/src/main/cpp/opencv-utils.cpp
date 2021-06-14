//
// Created by Arpit Lekhra on 24.03.21.
//
#include <jni.h>
#include <android/bitmap.h>
#include "opencv-utils.h"
#include <opencv2/imgproc/imgproc.hpp>

String test(Mat src){
    String cv = "Hello Opencv test method";
    return cv;
}

void drawkps(JNIEnv *env, Mat src, _jdoubleArray *kps_x, _jdoubleArray *kps_y)
{
    //jclass kpsClass = env->GetObjectClass(kps);
    //jfieldID iId = env->GetFieldID(kpsClass, "i", "I");
    int x=0;
    int y=0;
    jsize len = (*env).GetArrayLength(kps_x);
    jdouble *body_x = (*env).GetDoubleArrayElements(kps_x, 0);
    jdouble *body_y = (*env).GetDoubleArrayElements(kps_y, 0);
    for (int i = 0; i<len;i++){
        jdouble x = body_x[i];
        jdouble y = body_y[i];
        circle(src, Point(x,y),5,Scalar(255,0,0,255), FILLED);

    }
    
}

void drawbbox(JNIEnv *env, Mat src, jdouble kps_min_x,
              jdouble kps_min_y, jdouble kps_max_x, jdouble kps_max_y)
{
    rectangle(src, Point(kps_min_x,kps_min_y),Point(kps_max_x,kps_max_y) ,Scalar(255,0,255,0), 5);
}