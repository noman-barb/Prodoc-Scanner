#include <jni.h>
#include <android/log.h>

#include <iostream>
#include <opencv2/opencv.hpp>
#include <string>
#include<cmath>
#include<algorithm>
#include<vector>

#include <time.h>
#include <time.h>

#define IMAGE_SIZE 512
#define min_x 0
#define min_y 0
#define max_x 512
#define max_y 512


using namespace cv;
using namespace std;


static double now_ms(void) {

    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;

}

float det(float a, float b, float c, float d);

float get_gradient(int point1[], int point2[]);

int evaluate_line(int point1[], int point2[], int x);

float get_angle(int point1[], int point2[]);

int get_xintercept(int point1[], int point2[]);

int get_yintercept(int point1[], int point2[]);

float normd(Vec4i v);

void color_cluster(Mat &image, Mat &result);

void normalize_image(Mat &image, Mat &result);

double median_mat(cv::Mat Input);

void auto_canny(Mat &image, Mat &result, float sigma);

void find_lines(Mat &image, vector<Vec4i> &filtered_lines);

void draw_line(Vec4i points, Mat result);

void mask_lines(vector<Vec4i> lines, Mat &result, int top);

void find_hulls(Mat &mask, vector<vector<Point> > &hulls);

bool find_approx_quad(vector<Point> &cnt, vector<Point> &quad_cnt);

bool is_valid_quad(vector<Point> &cnt);

void find_corners(Mat &image, vector<Point> &corners);

void vector_Point_to_Mat(vector<Point> v_point, Mat &mat);

#define TAG "NativeLib"

using namespace std;
using namespace cv;

extern "C" {
void JNICALL
Java_com_aaindia_prodocscanner_TestActivity_adaptiveThresholdFromJNI(JNIEnv *env,
                                                                     jobject instance,
                                                                     jlong matAddr) {

    // get Mat from raw address
    Mat &image_original = *(Mat *) matAddr;

    clock_t begin = clock();


    Mat image;
    resize(image_original, image, Size(512, 512));
    image.convertTo(image, CV_8U);

    vector<Point> corners;

    try {
        find_corners(image, corners);


        double scale_x = image_original.cols * 1.00 / image.cols;
        double scale_y = image_original.rows * 1.00 / image.rows;

        for (size_t i = 0; i < corners.size(); i++) {
            corners[i].x *= scale_x;
            corners[i].y *= scale_y;
        }




//        vector<vector<Point>> _corners;
//        _corners.push_back(corners);
//
//        drawContours(image_original, _corners, 0, Scalar(0, 255, 0), 5);


        __android_log_print(ANDROID_LOG_INFO, TAG, "working");
    }
    catch (...) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "not working");
    }








    // log computation time to Android Logcat
    double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, TAG, "adaptiveThreshold computation time = %f seconds\n",
                        totalTime);
}
}


void find_corners(Mat &image, vector<Point> &corners) {



    /**
    COLOR SEGMENTATION -> MEDIAN BLUR 7
    */

    Mat cluster_mask(image.rows, image.cols, CV_8U);
    color_cluster(image, cluster_mask);
    medianBlur(cluster_mask, cluster_mask, 7);

    /**
    NORMALIZED IMAGE -> MEDIAN BLUR 3
    */

    Mat norm_image(image.rows, image.cols, CV_8U);
    normalize_image(image, norm_image);
    medianBlur(norm_image, norm_image, 3);

    /**
    EDGE -> DILATE, FOR CLUSTER AND NORM_IMG
    */

    auto_canny(cluster_mask, cluster_mask, 1);
    auto_canny(norm_image, norm_image, 1);
    dilate(cluster_mask, cluster_mask, Mat::ones(5, 5, CV_8U));
    dilate(norm_image, norm_image, Mat::ones(5, 5, CV_8U));

    /**
    FIND RELEVANT HOUGH LINES FOR CLUSTER AND NORM
    */

    vector<Vec4i> hough_lines_cluster;
    find_lines(cluster_mask, hough_lines_cluster);

    vector<Vec4i> hough_lines_norm;
    find_lines(norm_image, hough_lines_norm);

    /**
    MASK LINES FOR CLUSTER AND NORM
    */

    Mat mask_lines_cluster = Mat::zeros(image.rows, image.cols, CV_8U);
    Mat mask_lines_norm = Mat::zeros(image.rows, image.cols, CV_8U);

    mask_lines(hough_lines_cluster, mask_lines_cluster, 21);
    mask_lines(hough_lines_norm, mask_lines_norm, 21);

    /**
    FIND HULLS FOR CLUSTER AND NORM
    */

    vector<vector<Point> > hulls_cluster;
    vector<vector<Point> > hulls_norm;

    find_hulls(mask_lines_cluster, hulls_cluster);
    find_hulls(mask_lines_norm, hulls_norm);


    /**
    MATCH BETWEEN CLUSTER AND NORM
    */

    size_t top = 4;
    int overlap = -1;
    size_t best_norm_index = -1, best_cluster_index = -1;

    for (size_t i = 1; i < hulls_cluster.size(); i++) {

        if (i == top)
            break;

        Mat mask_cluster = Mat::ones(image.rows, image.cols, CV_8U);

        drawContours(mask_cluster, hulls_cluster, i, Scalar(255, 255, 255), -1);

        for (size_t j = 1; j < hulls_norm.size(); j++) {

            if (j == top)
                break;

            Mat mask_norm = Mat::zeros(image.rows, image.cols, CV_8U);
            drawContours(mask_norm, hulls_norm, j, Scalar(255, 255, 255), -1);


            Mat compare_mat = mask_norm == mask_cluster;
            int match = countNonZero(compare_mat);

            if (match > overlap) {
                overlap = match;
                best_norm_index = j;
                best_cluster_index = i;
            }


        }
    }

    double area_cluster = contourArea(hulls_cluster[best_cluster_index]);
    double area_norm = contourArea(hulls_norm[best_norm_index]);

    vector<vector<Point>> best_hull;
    vector<vector<Point>> best_hull2;

    if (area_cluster > area_norm) {

        best_hull.push_back(hulls_cluster[best_cluster_index]);
        best_hull2.push_back(hulls_cluster[best_cluster_index]);

    } else {

        best_hull.push_back(hulls_norm[best_norm_index]);
        best_hull2.push_back(hulls_norm[best_norm_index]);

    }
    if (!find_approx_quad(best_hull[0], best_hull2[0])) {

        best_hull2[0][0] = Point(5, 5);
        best_hull2[0][1] = Point(5, 505);
        best_hull2[0][2] = Point(505, 505);
        best_hull2[0][3] = Point(505, 5);
    }

    corners = best_hull2[0];
    //drawContours( image,  best_hull2 ,  0, Scalar(255,0, 0)  , 2 );



}


bool is_valid_quad(vector<Point> &cnt) {


    double peri = arcLength(cnt, true);

    for (size_t i = 0; i < cnt.size(); i++) {

        Point p1 = cnt[i];
        Point p2 = (i == 3) ? cnt[0] : cnt[i + 1];

        double side = norm(p1 - p2);

        if (side / peri < 0.13)
            return false;
    }

    for (size_t i = 0; i < cnt.size(); i++) {

        Point p1 = cnt[i];
        Point p2 = (i == 3) ? cnt[0] : cnt[i + 1];
        Point p3 = (i >= 2) ? cnt[i - 2] : cnt[i + 2];

        int x1 = p1.x, y1 = p1.y;
        int x2 = p2.x, y2 = p2.y;
        int x3 = p3.x, y3 = p3.y;

        double area = 0.5 * det(x1 - x2, x2 - x3, y1 - y2, y2 - y3);

        if (area < 5000)
            return false;
    }

    return true;


}


bool find_approx_quad(vector<Point> &cnt, vector<Point> &quad_cnt) {

    double cnt_area = contourArea(cnt);


    if (cnt_area < 150.0 * 150)
        return false;

    double arc_len = arcLength(cnt, true);

    double e1 = 0, e2 = 1, e = 0.5;

    int max_it = 10;

    bool is_found = false;

    for (int i = 0; i < max_it; i++) {

        e = (e1 + e2) / 2.0;

        double epsilon = e * arc_len;

        approxPolyDP(cnt, quad_cnt, epsilon, true);

        size_t n_corners = quad_cnt.size();

        if (n_corners > 4) {
            e1 = e;
        } else if (n_corners < 4) {
            e2 = e;
        } else {

            is_found = is_valid_quad(quad_cnt);

            break;
        }

    }

    return is_found;

}


void find_hulls(Mat &mask, vector<vector<Point> > &hulls) {

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;

    findContours(mask, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);


    vector<vector<Point> > hull(contours.size());

    for (size_t i = 0; i < contours.size(); i++) {
        convexHull(contours[i], hull[i]);
    }

    hulls = hull;


    sort(hulls.begin(), hulls.end(), [](const vector<Point> &lhs, const vector<Point> &rhs) {

        return (contourArea(lhs) > contourArea(rhs));

    });
}

void mask_lines(vector<Vec4i> lines, Mat &result, int top) {

    for (auto i = lines.begin(); i != lines.end(); ++i) {
        if (!top)
            break;
        draw_line(*i, result);
        top--;
    }

    draw_line({5, 5, 5, 505}, result);
    draw_line({5, 5, 505, 5}, result);
    draw_line({505, 505, 5, 505}, result);
    draw_line({505, 505, 505, 5}, result);

}


void find_lines(Mat &image, vector<Vec4i> &filtered_lines) {

    vector<Vec4i> lines;

    HoughLinesP(image, lines, 1, 3.14 / 180, 20, 100, 16);

    for (size_t i = 0; i < lines.size(); i++) {
        Vec4i l = lines[i];

        int point1[2] = {l[0], l[1]};
        int point2[2] = {l[2], l[3]};

        float angle = abs(get_angle(point1, point2));

        if (angle < 20 || angle > 70) {
            filtered_lines.push_back(l);
        }
    }


    sort(filtered_lines.begin(), filtered_lines.end(), [](const Vec4i &lhs, const Vec4i &rhs) {

        return (normd(lhs) > normd(rhs));

    });

}


void auto_canny(Mat &image, Mat &result, float sigma) {


    if (image.channels() == 3) {
        //CV_BGR2GRAY = 22
        cvtColor(image, result, COLOR_RGB2GRAY);
    } else if (image.channels() == 4) {
        cvtColor(image, result, COLOR_RGBA2GRAY);
    } else {
        result = image;
    }


    double v = median_mat(result);
    v = v * 255;


    int lower = (int) std::max(0.0, (1.0 - sigma) * v);
    int upper = (int) std::min(255.0, (1.0 + sigma) * v);

    Canny(result, result, lower, upper);
}


void normalize_image(Mat &image, Mat &result) {


    vector<Mat> image_planes;
    split(image, image_planes);

    //__android_log_print(ANDROID_LOG_INFO, TAG, "SPLIT %f", now_ms() - begin);

    Mat temp(image.rows, image.cols, CV_8U);


    if (image.channels() > 2) {

        for (int i = 0; i < 3; ++i) {
            dilate(image_planes[i], temp, Mat::ones(7, 7, CV_8U));
            //medianBlur(temp, temp, 21);

            blur(temp, temp, Size(21, 21));


            absdiff(image_planes[i], temp, temp);
            temp = 255 - temp;
            //NORM_MINMAX = 32
            normalize(temp, image_planes[i], 0, 255, 32);
        }

        merge(image_planes, result);

        image_planes[0].release();
        image_planes[1].release();
        image_planes[2].release();
    } else {


        Mat image_original = image;

        // __android_log_print(ANDROID_LOG_INFO, TAG, "BEFORE DILATE %f", now_ms() - begin);

        dilate(image_original, temp, Mat::ones(7, 7, CV_8U));

        //__android_log_print(ANDROID_LOG_INFO, TAG, "AFTER DILATE %f", now_ms() - begin);


        blur(temp, temp, Size(21, 21));

        //__android_log_print(ANDROID_LOG_INFO, TAG, "AFTER BLUR %f", now_ms() - begin);

        absdiff(image_original, temp, temp);

        //__android_log_print(ANDROID_LOG_INFO, TAG, "AFTER DIFF %f", now_ms() - begin);

        temp = 255 - temp;

        //__android_log_print(ANDROID_LOG_INFO, TAG, "AFTER INVERT %f", now_ms() - begin);

        //NORM_MINMAX = 32
        normalize(temp, image_original, 0, 255, 32);

        //__android_log_print(ANDROID_LOG_INFO, TAG, "AFTER NORMALIZE %f", now_ms() - begin);


    }


}


void color_cluster(Mat &image, Mat &result) {

    int n = image.rows * image.cols;

    vector<Mat> image_planes;
    split(image, image_planes);

    Mat pixel_values(n, 3, CV_8U);
    for (int i = 0; i < 3; ++i)
        image_planes[i].reshape(1, n).copyTo(pixel_values.col(i));
    pixel_values.convertTo(pixel_values, CV_32F);

    //CV_TERMCRIT_ITER | CV_TERMCRIT_EPS = 3

    kmeans(pixel_values, 2, result,
           TermCriteria(TermCriteria::EPS + TermCriteria::MAX_ITER, 10, 0.001), 1,
           KMEANS_RANDOM_CENTERS);
    result = result.reshape(0, image.rows);
    result = result * 255;
    result.convertTo(result, CV_8U);

}


double median_mat(cv::Mat Input) {

    int nVals = 256;

    // COMPUTE HISTOGRAM OF SINGLE CHANNEL MATRIX
    float range[] = {0.0, (float) nVals};
    const float *histRange = {range};
    bool uniform = true;
    bool accumulate = false;
    cv::Mat hist;
    calcHist(&Input, 1, 0, cv::Mat(), hist, 1, &nVals, &histRange, uniform, accumulate);

    // COMPUTE CUMULATIVE DISTRIBUTION FUNCTION (CDF)
    cv::Mat cdf;
    hist.copyTo(cdf);
    for (int i = 1; i <= nVals - 1; i++) {
        cdf.at<float>(i) += cdf.at<float>(i - 1);
    }
    cdf /= Input.total();

    // COMPUTE MEDIAN
    double medianVal;
    for (int i = 0; i <= nVals - 1; i++) {
        if (cdf.at<float>(i) >= 0.5) {
            medianVal = i;
            break;
        }
    }
    return medianVal / nVals;

}

void draw_line(Vec4i points, Mat result) {

    int x1 = points[0];
    int y1 = points[1];
    int x2 = points[2];
    int y2 = points[3];

    int point1[2] = {x1, y1};
    int point2[2] = {x2, y2};

    int x_intercept = get_xintercept(point1, point2);
    int y_intercept = get_yintercept(point1, point2);


    auto p1 = Point(x_intercept, 0);
    auto p2 = Point(0, y_intercept);

    if (x_intercept <= 0 && y_intercept > 0) {
        p1 = Point(512, evaluate_line(point1, point2, 512));

        p2 = Point(0, y_intercept);
    } else if (y_intercept <= 0 && x_intercept > 0) {

        p1 = Point(x_intercept, 0);
        p2 = Point(512, evaluate_line(point1, point2, 512));

    }


    line(result, p1, p2, Scalar(255, 255, 255), 3, LINE_8);


}


//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////





float get_gradient(int point1[2], int point2[2]) {
    int x1 = point1[0];
    int y1 = point1[1];

    int x2 = point2[0];
    int y2 = point2[1];

    if (x2 == x1)
        return (y2 - y1) / 0.001;

    return (y2 - y1) * 1.00 / (x2 - x1);
}

int evaluate_line(int point1[2], int point2[2], int x) {


    return (int) (get_gradient(point1, point2) * (x - point1[0]) + point1[1]);
}

float get_angle(int point1[2], int point2[2]) {

    return atan(get_gradient(point1, point2)) * 180.0 / 3.14;
}

int get_xintercept(int point1[2], int point2[2]) {

    int x1 = point1[0];
    int y1 = point1[1];

    int x2 = point2[0];
    int y2 = point2[1];

    float m = get_gradient(point1, point2);

    if (m == 0)
        m = 0.001;

    return (int) ((m * x1 - y1) / m);
}

int get_yintercept(int point1[2], int point2[2]) {

    int x1 = point1[0];
    int y1 = point1[1];

    int x2 = point2[0];
    int y2 = point2[1];

    float m = get_gradient(point1, point2);

    if (m == 0)
        m = 0.001;

    return (int) (-m * x1 + y1);


}

float normd(Vec4i v) {

    int x1 = v[0];
    int y1 = v[1];

    int x2 = v[2];
    int y2 = v[3];

    return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
}

float det(float a, float b, float c, float d) {

    return a * d - b * c;
}

void vector_Point_to_Mat(vector<Point> v_point, Mat &mat) {
    mat = Mat(v_point, true);
}


void BrightnessAndContrastAuto(const cv::Mat &src, cv::Mat &dst, float clipHistPercent) {

    //   CV_Assert(clipHistPercent >= 0);
//    CV_Assert((src.type() == CV_8UC1) || (src.type() == CV_8UC3) || (src.type() == CV_8UC4));

    int histSize = 256;
    float alpha, beta;
    double minGray = 0, maxGray = 0;

    //to calculate grayscale histogram
    cv::Mat gray;
    if (src.type() == CV_8UC1) gray = src;
    else if (src.type() == CV_8UC3) cvtColor(src, gray, COLOR_BGR2GRAY);
    else if (src.type() == CV_8UC4) cvtColor(src, gray, COLOR_BGRA2GRAY);
    if (clipHistPercent == 0) {
        // keep full available range
        cv::minMaxLoc(gray, &minGray, &maxGray);
    } else {
        cv::Mat hist; //the grayscale histogram

        float range[] = {0, 256};
        const float *histRange = {range};
        bool uniform = true;
        bool accumulate = false;
        calcHist(&gray, 1, 0, cv::Mat(), hist, 1, &histSize, &histRange, uniform, accumulate);

        // calculate cumulative distribution from the histogram
        std::vector<float> accumulator(histSize);
        accumulator[0] = hist.at<float>(0);
        for (int i = 1; i < histSize; i++) {
            accumulator[i] = accumulator[i - 1] + hist.at<float>(i);
        }

        // locate points that cuts at required value
        float max = accumulator.back();
        clipHistPercent *= (max / 100.0); //make percent as absolute
        clipHistPercent /= 2.0; // left and right wings
        // locate left cut
        minGray = 0;
        while (accumulator[minGray] < clipHistPercent)
            minGray++;

        // locate right cut
        maxGray = histSize - 1;
        while (accumulator[maxGray] >= (max - clipHistPercent))
            maxGray--;
    }



    // current range
    float inputRange = maxGray - minGray;

    alpha = (histSize - 1) / inputRange;   // alpha expands current range to histsize range
    beta = -minGray * alpha;             // beta shifts current range so that minGray will go to 0

    // Apply brightness and contrast normalization
    // convertTo operates with saurate_cast
    src.convertTo(dst, -1, alpha, beta);

    // restore alpha channel from source
//    if (dst.type() == CV_8UC4) {
//        int from_to[] = {3, 3};
//        cv::mixChannels(&src, 4, &dst, 1, from_to, 1);
//    }



}


void gamma_correction(Mat &src, Mat &dst, float fGamma) {

    fGamma = 1 / fGamma;

    unsigned char lut[256];

    for (int i = 0; i < 256; i++) {

        lut[i] = saturate_cast<uchar>(pow((float) (i / 255.0), fGamma) * 255.0f);

    }

    dst = src.clone();

    const int channels = dst.channels();

    switch (channels) {

        case 1: {

            MatIterator_<uchar> it, end;

            for (it = dst.begin<uchar>(), end = dst.end<uchar>(); it != end; it++)

                *it = lut[(*it)];

            break;

        }

        case 3: {

            MatIterator_<Vec3b> it, end;

            for (it = dst.begin<Vec3b>(), end = dst.end<Vec3b>(); it != end; it++) {

                (*it)[0] = lut[((*it)[0])];

                (*it)[1] = lut[((*it)[1])];

                (*it)[2] = lut[((*it)[2])];

            }

            break;

        }

    }

}


extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_activity_ImageCropActivity_cropV1Native(JNIEnv *env, jobject thiz,
                                                                 jlong matAddr,
                                                                 jlong native_obj_addr1) {

    // get Mat from raw address
    Mat &image_original = *(Mat *) matAddr;

    Mat &crop_bounds = *(Mat *) native_obj_addr1;


    Mat image;
    resize(image_original, image, Size(512, 512));
    image.convertTo(image, CV_8U);

    vector<Point> corners;


    try {
        find_corners(image, corners);


        double scale_x = image_original.cols * 1.00 / image.cols;
        double scale_y = image_original.rows * 1.00 / image.rows;

        for (size_t i = 0; i < corners.size(); i++) {
            corners[i].x *= scale_x;
            corners[i].y *= scale_y;
        }

        vector_Point_to_Mat(corners, crop_bounds);


//        vector<vector<Point>> _corners;
//        _corners.push_back(corners);
//
//        drawContours(image_original, _corners, 0, Scalar(0, 255, 0), 5);

    }
    catch (...) {

    }


}


void paperizeNative(jlong matAddr, jfloat colorVal) {

    Mat &image_original = *(Mat *) matAddr;

    normalize_image(image_original, image_original);

    colorVal = colorVal >= 50 ? colorVal - 49 : colorVal / 50;

    BrightnessAndContrastAuto(image_original, image_original, colorVal * 1.0 / 10);
}


void paperizeNative2(Mat &image_original, jfloat colorVal) {


    normalize_image(image_original, image_original);

    colorVal = colorVal >= 50 ? colorVal - 49 : colorVal / 50;

    BrightnessAndContrastAuto(image_original, image_original, colorVal * 1.0 / 10);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_brightnessContrastNative(JNIEnv *env, jclass clazz,
                                                                  jlong matAddr, jlong colorVal) {

    Mat &image_original = *(Mat *) matAddr;

    Mat image(image_original);
    BrightnessAndContrastAuto(image_original, image, colorVal);

    image_original = image;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_paperizeNative(JNIEnv *env, jclass clazz,
                                                        jlong matAddr, jfloat colorVal) {

    paperizeNative(matAddr, colorVal);


}extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_adjustGamma(JNIEnv *env, jclass clazz,
                                                           jlong matAddr, jfloat gamma) {

    Mat &image_original = *(Mat *) matAddr;

    gamma_correction(image_original, image_original, gamma);
}



extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_cropV1Native(JNIEnv *env, jclass clazz,
                                                      jlong matAddr,
                                                      jlong native_obj_addr1) {

    // get Mat from raw address
    Mat &image_original = *(Mat *) matAddr;

    Mat &crop_bounds = *(Mat *) native_obj_addr1;


    Mat image;
    resize(image_original, image, Size(512, 512));
    image.convertTo(image, CV_8U);

    vector<Point> corners;


    try {
        find_corners(image, corners);


        double scale_x = image_original.cols * 1.00 / image.cols;
        double scale_y = image_original.rows * 1.00 / image.rows;

        for (size_t i = 0; i < corners.size(); i++) {
            corners[i].x *= scale_x;
            corners[i].y *= scale_y;
        }

        vector_Point_to_Mat(corners, crop_bounds);


//        vector<vector<Point>> _corners;
//        _corners.push_back(corners);
//
//        drawContours(image_original, _corners, 0, Scalar(0, 255, 0), 5);

    }
    catch (...) {

    }


}


void cleanTextNativeGray(Mat &mat, jfloat colorVal) {


    Mat mat1 = mat.clone();

    blur(mat1, mat1, Size(3, 3));
    Mat binary(mat.rows, mat.cols, CV_8U);
    adaptiveThreshold(mat1, binary, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 51, 10);
    mat1.release();

    erode(binary, binary, Mat::ones(5, 5, CV_8U), Point(-1, -1), 3);


    paperizeNative2(mat, 0);
    bitwise_or(mat, binary, mat);


    double thresh = threshold(mat, binary, 0, 255, THRESH_OTSU);

    double control = colorVal;
    control += 1;
    control = control >= 50 ? control / 5.0 : control / 20.0;

    gamma_correction(mat, mat, (float) ((thresh + 55) * 1.0 / 255) * 0.8 * control);


    bitwise_not(binary, binary);
    bitwise_xor(mat, mat, mat, binary);


    binary.release();


}

extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_cleanTextNative(JNIEnv *env, jclass clazz,
                                                         jlong matAddr, jfloat colorVal) {


    Mat &mat = *(Mat *) matAddr;


    if (mat.channels() > 1) {


        Mat mat_original = mat.clone();

        if (mat.channels() == 3) {
            cvtColor(mat, mat, COLOR_BGR2GRAY);

        } else if (mat.channels() == 4) {
            cvtColor(mat, mat, COLOR_BGRA2GRAY);
            // cvtColor(mat_original, mat_original, COLOR_BGRA2BGR);
        }


        cleanTextNativeGray(mat, colorVal);



        vector<Mat> original_mats;
        split(mat_original, original_mats);

        vector<Mat> final_mats;
        split(mat_original, final_mats);


        mat.copyTo(final_mats[0]);
        mat.copyTo(final_mats[1]);
        mat.copyTo(final_mats[2]);


        threshold(mat, mat, 1, 255, THRESH_BINARY);

        bitwise_not(mat, mat);


        original_mats[0].copyTo(final_mats[0], mat);
        original_mats[1].copyTo(final_mats[1], mat);
        original_mats[2].copyTo(final_mats[2], mat);


        original_mats[0].release();
        original_mats[1].release();
        original_mats[2].release();


        merge(final_mats, mat_original);

        final_mats[0].release();
        final_mats[1].release();
        final_mats[2].release();


        cvtColor(mat_original, mat_original, COLOR_BGR2HSV);


        vector<Mat> mats2;

        split(mat_original, mats2);


        add(mats2[2], mats2[2] * ((colorVal-50)/50)*3, mats2[2], mat);


        merge(mats2, mat_original);

        mats2[0].release();
        mats2[1].release();
        mats2[2].release();


        cvtColor(mat_original, mat, COLOR_HSV2BGR);

        BrightnessAndContrastAuto(mat, mat, 2);

        mat_original.release();


    } else {


//        int shallBlur = 0;
//        if ((mat.rows / 1000.0) * (mat.cols / 1000.0) > 6) {
//            shallBlur = 1;
//        }

        cleanTextNativeGray(mat, colorVal);

//        if (shallBlur)
//            blur(mat, mat, Size(2, 2));


    }


}extern "C"
JNIEXPORT void JNICALL
Java_com_aaindia_prodocscanner_utils_MatFilter_doNothing(JNIEnv *env, jclass clazz) {
    // TODO: implement doNothing()
}