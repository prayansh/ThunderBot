package tarehart.rlbot.input;

import mikera.vectorz.Vector3;

public class CarOrientation {

    public Vector3 noseVector;
    public Vector3 roofVector;
    public Vector3 sideVector;

    public CarOrientation(Vector3 noseVector, Vector3 roofVector) {

        this.noseVector = noseVector;
        this.roofVector = roofVector;

        this.sideVector = noseVector.copy();
        this.sideVector.crossProduct(roofVector);
    }
}
