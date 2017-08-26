package tarehart.rlbot;

import mikera.vectorz.Vector3;

public class CarRotation {

    public Vector3 noseVector;
    public Vector3 roofVector;
    public Vector3 sideVector;

    public CarRotation(Vector3 noseVector, Vector3 roofVector) {

        this.noseVector = noseVector;
        this.roofVector = roofVector;

        this.sideVector = noseVector.copy();
        this.sideVector.crossProduct(roofVector);
    }
}
