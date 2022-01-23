package nerlihmax.uniorai2;

import java.util.Arrays;

class IIRFilterBase {
    protected int numberOfCoefficients = 0;
    protected int maxNumberOfCoefficients = 16;

    protected double fGain;

    protected double[] inputValues = new double[maxNumberOfCoefficients + 1];
    protected double[] outputValues = new double[maxNumberOfCoefficients + 1];
    protected double[] inputCoefficients = new double[maxNumberOfCoefficients];
    protected double[] outputCoefficients = new double[maxNumberOfCoefficients];

    public IIRFilterBase() {
        Arrays.fill(inputValues, 0);
        Arrays.fill(outputValues, 0);
        Arrays.fill(inputCoefficients, 0);
        Arrays.fill(outputCoefficients, 0);
    }

    public float filter(float value) {
        for (int iter = 0; iter < numberOfCoefficients; iter++) {
            inputValues[iter] = inputValues[iter + 1];
            outputValues[iter] = outputValues[iter + 1];
        }

        double result = (double)value * fGain;
        inputValues[numberOfCoefficients] = result;

        for (int iter = 0; iter < numberOfCoefficients; iter++) {
            result = result
                    + inputValues[iter] * inputCoefficients[iter]
                    + outputValues[iter] * outputCoefficients[iter];
        }

        outputValues[numberOfCoefficients] = result;
        return (float)result;
    }
}

class BSFilter50 extends IIRFilterBase {
    public BSFilter50() {
        super();

        this.numberOfCoefficients = 4;
        this.fGain = 8.37089190566345E-001;

        this.inputCoefficients[0] = 1.00000000000000E+000;
        this.inputCoefficients[1] = -1.24589220970327E+000;
        this.inputCoefficients[2] = 2.38806184954982E+000;
        this.inputCoefficients[3] = -1.24589220970327E+000;

        this.outputCoefficients[0] = -7.00896781188402E-001;
        this.outputCoefficients[1] = 9.49760308799785E-001;
        this.outputCoefficients[2] = -1.97230236060631E+000;
        this.outputCoefficients[3] = 1.13608549390706E+000;
    }
}

class LPFilter25 extends IIRFilterBase {
    public LPFilter25() {
        super();

        this.numberOfCoefficients = 2;
        this.fGain = 6.74552738890719E-002;

        this.inputCoefficients[0] = 1.00000000000000E+000;
        this.inputCoefficients[1] = 2.00000000000000E+000;

        this.outputCoefficients[0] = -4.12801598096189E-001;
        this.outputCoefficients[1] = 1.14298050253990E+000;
    }
}

class HPFilterEEG extends IIRFilterBase {
    public HPFilterEEG() {
        super();

        this.numberOfCoefficients = 1;
        this.fGain = 9.93755964953657E-001;

        this.inputCoefficients[0] = -1.00000000000000E+000;

        this.outputCoefficients[0] = 9.87511929907314E-001;
    }
}

abstract class Sensor {
    public BSFilter50 bsf50 = new BSFilter50();

    abstract float process(float value);
}

public class SensorProcessors {
    public static class EEG extends Sensor {
        private int counter = 0;

        LPFilter25 lpf25;
        HPFilterEEG hpf;

        public EEG() {
            lpf25 = new LPFilter25();
            hpf = new HPFilterEEG();
        }

        @Override
        public float process(float value) {
            return hpf.filter(lpf25.filter(bsf50.filter(value)));
        }
    }
}