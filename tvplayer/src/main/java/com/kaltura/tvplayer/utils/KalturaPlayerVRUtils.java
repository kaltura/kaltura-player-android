package com.kaltura.tvplayer.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.kaltura.playkit.player.vr.VRInteractionMode;

public class KalturaPlayerVRUtils {

    public static boolean isVRModeSupported(Context context, VRInteractionMode mode) {
        switch (mode) {
            case Touch:
                //Always supported
                return true;
            case Motion:
            case MotionWithTouch:
                SensorManager motionSensorManager = (SensorManager) context
                        .getSystemService(Context.SENSOR_SERVICE);
                return motionSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
            case CardboardMotion:
            case CardboardMotionWithTouch:
                SensorManager cardboardSensorManager = (SensorManager) context
                        .getSystemService(Context.SENSOR_SERVICE);
                Sensor accelerometerSensor = cardboardSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor gyroSensor = cardboardSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                return accelerometerSensor != null && gyroSensor != null;
            default:
                return true;
        }
    }

    public static boolean isVRLibAvailable() {
        try {
            Class<?> clazz = Class.forName("com.kaltura.playkitvr.DefaultVRPlayerFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
