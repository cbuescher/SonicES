package org.soc.sonices;

public record Scaler(float inputMin, float inputMax, float outputMin, float outputMax) {

    public float scale(float value) {
        return outputMin + (outputMax - outputMin) * ((value - inputMin) / (inputMax - inputMin));
    }
}
