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
        circle(src, Point(x,y),5,Scalar(255,255,255), FILLED);
    }
    line(src,Point(body_x[0],body_y[0]), Point(body_x[1],body_y[1]), Scalar(241,242,224,255),10);
    line(src,Point(body_x[1],body_y[1]), Point(body_x[2],body_y[2]), Scalar(196,203,128,255),10);
    line(src,Point(body_x[2],body_y[2]), Point(body_x[6],body_y[6]), Scalar(136,150,0,255),10);
    line(src,Point(body_x[6],body_y[6]), Point(body_x[3],body_y[3]), Scalar(64,77,0,255),10);
    line(src,Point(body_x[3],body_y[3]), Point(body_x[4],body_y[4]), Scalar(201,230,200,255),10);
    line(src,Point(body_x[4],body_y[4]), Point(body_x[5],body_y[5]), Scalar(132,199,129,255),10);
    line(src,Point(body_x[6],body_y[6]), Point(body_x[8],body_y[8]), Scalar(71,160,67,255),10);
    line(src,Point(body_x[8],body_y[8]), Point(body_x[13],body_y[13]), Scalar(32,94,27,255),10);
    line(src,Point(body_x[13],body_y[13]), Point(body_x[14],body_y[14]), Scalar(130,224,255,255),10);
    line(src,Point(body_x[14],body_y[14]), Point(body_x[15],body_y[15]), Scalar(7,193,255,255),10);
    line(src,Point(body_x[8],body_y[8]), Point(body_x[12],body_y[12]), Scalar(0,160,255,255),10);
    line(src,Point(body_x[12],body_y[12]), Point(body_x[11],body_y[11]), Scalar(0,111,255,255),10);
    line(src,Point(body_x[11],body_y[11]), Point(body_x[10],body_y[10]), Scalar(220,216,207,255),10);
    
}

void drawbbox(JNIEnv *env, Mat src, jdouble kps_min_x,
              jdouble kps_min_y, jdouble kps_max_x, jdouble kps_max_y)
{
    rectangle(src, Point(kps_min_x,kps_min_y),Point(kps_max_x,kps_max_y) ,Scalar(255,0,0,255), 5);
}
void drawcom(JNIEnv *env, Mat src, _jdoubleArray *comxlist, _jdoubleArray *comylist){
    jsize len = (*env).GetArrayLength(comxlist);
    jdouble *body_x = (*env).GetDoubleArrayElements(comxlist, 0);
    jdouble *body_y = (*env).GetDoubleArrayElements(comylist, 0);
    for (int i = 0; i<len;i++){
        jdouble x = body_x[i];
        jdouble y = body_y[i];
        circle(src, Point(x,y),5,Scalar(255,0,0,255), FILLED);
    }
}