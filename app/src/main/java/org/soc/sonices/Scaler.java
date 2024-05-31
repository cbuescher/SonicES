package org.soc.sonices;

public record Scaler(float inputMin, float inputMax, float outputMin, float outputMax) {

    public float scale(float value) {
        float v = outputMin + (outputMax - outputMin) * ((value - inputMin) / (inputMax - inputMin));
        if (v > outputMax) {
            v = outputMax;
        } else if (v < outputMin) {
            v = outputMin;
        }
        return v;
    }
}
