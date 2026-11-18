package dev.ajuntanaga.io24;

/** Passive/Vintage analog-network designers, ported from the Linux host.
 * Every operation is binary32; association is intentional. Golden vectors are
 * from the independent UC instruction interpreter, not from this port.
 */
final class NativeEq {
    private NativeEq() { }

    private static float[] normalize(float[] denominator, float[] numerator, float gain) {
        float scale = 1.0f / denominator[0];
        float[] output = new float[denominator.length * 2 - 1];
        output[0] = scale * numerator[0] * gain;
        for (int i = 1; i < denominator.length; i++) {
            output[i * 2 - 1] = -(scale * denominator[i]);
            output[i * 2] = scale * numerator[i] * gain;
        }
        return output;
    }

    static float[] bilinear3(float[] a, float period) {
        float squared = (period * period);
        float cubed = (squared * period);
        float second = ((a[2] * 4.0f) * period);
        float third24 = (a[3] * 24.0f);
        float third8 = (a[3] * 8.0f);
        float first = ((a[1] + a[1]) * squared);
        float constant = (a[0] * cubed);
        float constant3 = ((a[0] * 3.0f) * cubed);
        return new float[] {((third8 + second) + (constant + first)), (((constant3 + first) - second) - third24), (((constant3 - first) - second) + third24), (((constant - first) + second) - third8)};
    }

    static float[] passiveLow(int index, float boost, float attenuation, float rate) {
        float param_3 = boost;
        float param_4 = attenuation;
        float param_5 = (1.0f / rate);
        float t6 = ((param_4 * 9980.0f) + 100.00001f);
        float t11 = ((param_3 * 980.0f) + 100.0f);
        float[] components54 = new float[][] {new float[] {2.2e-06f, 1e-07f}, new float[] {1e-06f, 4.7e-08f}, new float[] {4.7e-07f, 2.2e-08f}, new float[] {3.3e-07f, 1.5e-08f}}[index];
        float t2 = components54[0];
        float t4 = components54[1];
        float t9 = ((t2 * t11) * t6);
        float t8 = (t9 * (t4 * 10010000000000.0f));
        float t1 = (t8 * 4.0f);
        t9 = (t9 * (t4 * 131120000000000.0f));
        float t5 = (((((t4 * 12011000000.0f) + (t2 * 10000000.0f)) * t11) + (t4 * 10010000000000.0f)) * t6);
        float t7 = (((((t11 * 1011000.0f) + 10000000.0f) * t6) + 10010000000000.0f) + (t11 * 12011000000.0f));
        float t3 = (param_5 * param_5);
        float t10 = (((((t11 * 1011000.0f) + 11120001000.0f) * t6) + 131120000000000.0f) + (t11 * 12011000000.0f));
        t6 = ((((((t2 * 11120001000.0f) + (t4 * 1000000000.0f)) + ((t4 * 10011000000.0f) + (t4 * 1000000000.0f))) * t11) + (t4 * 131120000000000.0f)) * t6);
        t4 = (((((t11 * (t2 * 10000000.0f)) * 2002000.0f) + t5) + t5) * param_5);
        float local_c4 = (((t7 + t7) * t3) - (t8 * 8.0f));
        param_5 = (((((t2 * t11) * 262240000000000.0f) + t6) + t6) * param_5);
        float local_c8 = ((t4 + t1) + (t7 * t3));
        float local_c0 = ((t1 - t4) + (t7 * t3));
        t6 = (t9 * 4.0f);
        float local_b8 = ((param_5 + t6) + (t10 * t3));
        float local_b0 = ((t6 - param_5) + (t10 * t3));
        float local_b4 = (((t10 + t10) * t3) - (t9 * 8.0f));
        return normalize(new float[] {local_b8, local_b4, local_b0}, new float[] {local_c8, local_c4, local_c0}, Float.intBitsToFloat(0x4150999a));
    }

    static float[] passiveHigh(int index, float boost, float attenuation, int attenuation_index, float bandwidth, float rate) {
        float param_3 = boost;
        float param_4 = attenuation;
        float param_6 = bandwidth;
        float param_7 = (1.0f / rate);
        float t7 = (9990.0f - (param_3 * 998.0f));
        float t21 = ((param_6 * 99.799995f) + 1.0f);
        float t8 = (999.0f - (param_6 * 99.799995f));
        float t16 = ((param_3 * 998.0f) + 10.0f);
        float t10 = ((param_4 * 206.79999f) + 110.0f);
        float[] components83 = new float[][] {new float[] {1.5e-08f, 0.175f}, new float[] {1.5e-08f, 0.1f}, new float[] {1e-08f, 0.09f}, new float[] {1e-08f, 0.065f}, new float[] {6.8e-09f, 0.035f}, new float[] {6.8e-09f, 0.023f}, new float[] {3.3e-09f, 0.0266f}}[index];
        float t4 = components83[0];
        float local_res8 = components83[1];
        float t1 = new float[] {1.97e-07f, 9.4e-08f, 4.7e-08f}[attenuation_index];
        float t12 = (t16 + t8);
        float t13 = (t8 + 75.0f);
        float t5 = (t12 + 75.0f);
        float t20 = ((t13 * t21) + (t8 * 75.0f));
        float t2 = (t16 + t10);
        float t15 = ((t2 * t8) + t12);
        float t11 = ((((t8 + 10000.0f) * t7) + (t12 + t7)) + ((t12 * 10000.0f) + (t16 * t8)));
        float t3 = ((((t16 + t7) + 10001.0f) * t21) + t11);
        float t9 = (t10 + 10000.0f);
        float t6 = (t2 * t7);
        float t17 = ((((((t13 * t16) + ((t13 + 10000.0f) * t7)) + (((t5 * 10000.0f) + 75.0f) + (t12 + t7))) * t21) + (t11 * 75.0f)) * t1);
        float t14 = ((t21 + t8) * 10000.0f);
        t12 = (((((((t10 + t8) * t16) + (t10 * t8)) * 10000.0f) + (t6 * (t8 + 10000.0f))) + ((t16 * t8) * t10)) + (((t9 + t12) * t7) + (t9 * t12)));
        float local_bc = (((t4 * 10000.0f) * t20) * (local_res8 * t1));
        float local_b0 = ((local_res8 * t4) * t17);
        float local_c0 = (local_bc * 24.0f);
        local_bc = (local_bc * 8.0f);
        float t18 = (param_7 * param_7);
        float t19 = (t18 * param_7);
        t11 = (((((((t2 + 1.0f) * t21) + t15) * t4) + (t20 * t1)) * 20000.0f) * t18);
        t15 = (((((((((t2 * t13) + t5) * t21) + (t15 * 75.0f)) * t1) + ((t21 + t8) * local_res8)) * (t4 * 10000.0f)) * 4.0f) * param_7);
        t8 = (t19 * t14);
        t14 = ((t14 * 3.0f) * t19);
        float local_c8 = ((t11 + t8) + (t15 + local_bc));
        local_bc = (((t8 - t11) + t15) - local_bc);
        float local_c4 = (((t14 + t11) - t15) - local_c0);
        local_c0 = (((t14 - t11) - t15) + local_c0);
        float local_ac = (local_b0 * 8.0f);
        local_b0 = (local_b0 * 24.0f);
        t8 = ((t3 * 3.0f) * t19);
        param_7 = (((((((((((((t13 + t10) * t16) + (t13 * t10)) * 10000.0f) + ((t9 + t5) * t7)) + (((t6 * (t13 + 10000.0f)) + ((t16 * t10) * t13)) + (t9 * t5))) * t21) + (t12 * 75.0f)) * t1) + (t3 * local_res8)) * t4) * 4.0f) * param_7);
        t18 = (((((((((t6 + (t16 * t10)) + (t2 * 10000.0f)) + (t9 + t7)) * t21) + t12) * (t4 + t4)) + t17) + t17) * t18);
        float local_b8 = ((t18 + (t3 * t19)) + (param_7 + local_ac));
        local_ac = ((((t3 * t19) - t18) + param_7) - local_ac);
        float local_b4 = (((t8 + t18) - param_7) - local_b0);
        local_b0 = (((t8 - t18) - param_7) + local_b0);
        return normalize(new float[] {local_b8, local_b4, local_b0, local_ac}, new float[] {local_c8, local_c4, local_c0, local_bc}, 12.0f);
    }

    static float[] vintageMid(String band, int index, float gain, float rate) {
        float param_3 = gain;
        float param_4 = (1.0f / rate);
        float t8 = (5000.0f - (param_3 * 312.4375f));
        float t4 = ((param_3 * 312.4375f) + 5000.0f);
        float[][] table;
        if ("low-mid".equals(band)) {
            table = new float[][] {new float[] {1e-08f, (-688160000000.0f), (-1376320000000.0f), 688160000000.0f, 1376320000000.0f}, new float[] {5.8e-09f, (-500479980000.0f), (-1000959970000.0f), 500479980000.0f, 1000959970000.0f}, new float[] {3.3e-09f, (-312800020000.0f), (-625600040000.0f), 312800020000.0f, 625600040000.0f}};
        } else {
            table = new float[][] {new float[] {1e-07f, (-6256000000000.0f), (-12512000000000.0f), 6256000000000.0f, 12512000000000.0f}, new float[] {4.7e-08f, (-3128000000000.0f), (-6256000000000.0f), 3128000000000.0f, 6256000000000.0f}, new float[] {2.2e-08f, (-1407599900000.0f), (-2815199800000.0f), 1407599900000.0f, 2815199800000.0f}};
        }
        float[] components132 = table[index];
        float t5 = components132[0];
        float t6 = components132[1];
        float t9 = components132[2];
        float t7 = components132[3];
        float t1 = components132[4];
        float t3 = (param_4 * param_4);
        float t2 = (t4 * 6800.0f);
        float local_c4 = ((t3 * (-1564000000000.0f)) - (t9 * t5));
        t4 = ((((((((((-15000.0f) - t8) - 6800.0f) * t5) * 63920000.0f) - (t5 * 524144000000.0f)) * t4) - ((t5 * 63920000.0f) * (t8 * 6800.0f))) + ((((((-15000.0f) - t8) * t5) * 13600.0f) - (t5 * 111520000.0f)) * t2)) * param_4);
        float local_c8 = ((t4 + (t6 * t5)) + (t3 * (-782000000000.0f)));
        float local_c0 = (((t6 * t5) - t4) + (t3 * (-782000000000.0f)));
        param_4 = (((((((t8 + 6800.0f) * t2) * 4700.0f) + (t8 * 958800000000.0f)) + ((t2 + 157760000.0f) * (t8 * 6800.0f))) * (t5 + t5)) * param_4);
        float local_b8 = (((t7 * t5) + param_4) + (t3 * 782000000000.0f));
        float local_b0 = (((t7 * t5) - param_4) + (t3 * 782000000000.0f));
        float local_b4 = ((t3 * 1564000000000.0f) - (t1 * t5));
        return normalize(new float[] {local_b8, local_b4, local_b0}, new float[] {local_c8, local_c4, local_c0}, (-1.0f));
    }

    static float[] vintageHigh(float gain, float rate) {
        float param_2 = gain;
        float local_res20 = (1.0f / rate);
        float t12 = (5000.0f - (param_2 * 312.4375f));
        float t4 = ((param_2 * 312.4375f) + 5000.0f);
        float t6 = (t4 * 620.0f);
        float t5 = (t4 + 620.0f);
        float t9 = (((-620.0f) - t4) * 620.0f);
        float t11 = ((((-620.0f) - t4) * 5100.0f) - t6);
        float t7 = ((t5 * 5100.0f) + t6);
        float t2 = ((t11 * 5100.0f) - (t12 * t7));
        float t1 = ((t12 * 0.00018876f) + 0.104346f);
        float t14 = ((t4 + 5100.0f) * 6200.0f);
        float t3 = ((((t4 * 17720.0f) + 64362000.0f) + t14) * 145700000.0f);
        float t15 = ((((t14 * 1116250000.0f) + t3) + (((t4 + 5100.0f) * 3423949800000.0f) + t3)) * 3.3e-08f);
        t3 = (((t11 * 6200.0f) - (t7 * 12000.0f)) * 145700000.0f);
        float t16 = (((((t9 - (t5 * 12000.0f)) * 5100.0f) - (t6 * 12620.0f)) + (t11 * 5100.0f)) * 145700000.0f);
        float t13 = (((-2.6352235e+19f) - (t12 * 5167105000000000.0f)) - ((t12 + 5100.0f) * 1.0000849e+16f));
        float t10 = ((-9.988347e+19f) - (t12 * 1.9584995e+16f));
        t14 = (((((-12620.0f) - t12) * 5100.0f) - (t12 * 12620.0f)) * 833404000000.0f);
        t4 = (t2 * 3423949800000.0f);
        float t17 = (((t2 * 6200.0f) - ((t7 * 12000.0f) * (t12 + 5100.0f))) * 145700000.0f);
        float t8 = (t15 * t1);
        t2 = ((((((t9 * 5100.0f) - (((t12 + 12000.0f) * t5) * 5100.0f)) - ((t12 + 12620.0f) * t6)) * 5100.0f) - (t7 * (t12 * 12620.0f))) * 145700000.0f);
        t4 = (((t17 + t17) + t4) + ((t2 + t4) + t2));
        t5 = (local_res20 * local_res20);
        t2 = (t4 * 4.356e-15f);
        float local_cc = ((t5 * (-1.9808346e+17f)) - (t4 * 8.712e-15f));
        t4 = (((t15 * 11440.0f) + (t1 * 34629977000000.0f)) * local_res20);
        local_res20 = ((((((t3 * 6.6e-08f) + (t3 * 6.6e-08f)) + ((t11 * 3423949800000.0f) * 6.6e-08f)) + ((((t11 * 3423949800000.0f) + t16) + t16) * 6.6e-08f)) + ((((t14 * 6.6e-08f) + (t14 * 6.6e-08f)) + (t10 * 6.6e-08f)) + (((t13 + t10) + t13) * 6.6e-08f))) * local_res20);
        t1 = (t8 * 4.0f);
        float local_d0 = ((local_res20 + t2) + (t5 * (-9.904173e+16f)));
        float local_c8 = ((t2 - local_res20) + (t5 * (-9.904173e+16f)));
        float local_bc = ((t5 * 1.9808346e+17f) - (t8 * 8.0f));
        float local_c0 = ((t4 + t1) + (t5 * 9.904173e+16f));
        float local_b8 = ((t1 - t4) + (t5 * 9.904173e+16f));
        return normalize(new float[] {local_c0, local_bc, local_b8}, new float[] {local_d0, local_cc, local_c8}, (-1.0f));
    }

    static float[] vintageLow(int index, float gain, float rate) {
        float param_3 = gain;
        float param_5 = (1.0f / rate);
        float t6 = (23500.0f - (param_3 * 1439.375f));
        float t2 = ((param_3 * 1439.375f) + 23500.0f);
        float t4 = new float[] {2e-07f, 1e-07f, 4.7e-08f, 1.5e-08f}[index];
        float t5 = (t2 + 6200.0f);
        float t7 = (t4 * 6200.0f);
        float local_d8 = (t5 * (-11440.0f));
        float t3 = (t2 * t4);
        float local_d4 = ((((t2 * t7) * (-11440.0f)) - ((t5 * (t4 * t6)) * 11440.0f)) - (((((t2 * 8.58e-05f) + 1.5615599f) * t6) + (t2 * 2.052336f)) + 19108.002f));
        float local_d0 = (((((((((t7 * (-5720.0f)) - (t4 * 169998400.0f)) + (t7 * (-5720.0f))) * t2) - (t4 * 1273866900000.0f)) * t6) + (t3 * (-1273866900000.0f))) * 1.5e-08f) - (((t2 * t7) * 11440.0f) * (t4 * t6)));
        float local_cc = ((t6 * (t3 * (-1273866900000.0f))) * (t4 * 1.5e-08f));
        float local_c8 = ((t6 * 11440.0f) + 70928000.0f);
        float local_c4 = (((((t5 + 17720.0f) * t6) * 8.58e-05f) + ((t2 * 1.5615599f) + 19108.002f)) + ((((t3 + t7) * t6) * 11440.0f) + (t3 * 70928000.0f)));
        float local_bc = (((t2 * 19108.002f) * t6) * (t4 * t4));
        float local_c0 = (((((((((t4 * 29720.0f) + t7) + t7) * t2) + (t4 * 222704000.0f)) * t6) * 8.58e-05f) + (t3 * 19108.002f)) + ((t6 * t7) * (t3 * 11440.0f)));
        return normalize(bilinear3(new float[] {local_c8, local_c4, local_c0, local_bc}, param_5), bilinear3(new float[] {local_d8, local_d4, local_d0, local_cc}, param_5), (-1.0f));
    }

    static float[] vintage(String band, int index, float gain, float rate) {
        switch (band) {
            case "low": return vintageLow(index, gain, rate);
            case "high": return vintageHigh(gain, rate);
            case "low-mid": case "hi-mid": return vintageMid(band, index, gain, rate);
            default: throw new IllegalArgumentException("Unknown Vintage band " + band);
        }
    }
}
