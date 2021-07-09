//
// Created by Arpit Lekhra on 24.03.21.
//
#pragma once


#include <opencv2/core.hpp>

using namespace cv;

String test(Mat src);
void drawkps(JNIEnv *env, Mat src, _jdoubleArray *kps_x, _jdoubleArray *kps_y);
void drawbbox(JNIEnv *env, Mat src, jdouble kps_min_x,
              jdouble kps_min_y, jdouble kps_max_x, jdouble kps_max_y);
void drawcom(JNIEnv *env, Mat src, _jdoubleArray *comxlist, _jdoubleArray *comylist);
