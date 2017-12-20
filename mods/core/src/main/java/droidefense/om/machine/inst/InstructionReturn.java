package droidefense.om.machine.inst;

import droidefense.om.machine.base.struct.generic.IDroidefenseMethod;
import droidefense.rulengine.base.AbstractAtomNode;
import droidefense.om.machine.base.exceptions.VirtualMachineRuntimeException;
import droidefense.om.machine.base.struct.generic.IAtomField;
import droidefense.om.machine.base.struct.generic.IAtomFrame;

import java.io.Serializable;

/**
 * Created by sergio on 25/3/16.
 */
public final class InstructionReturn implements Serializable {

    private IAtomFrame frame;
    private IAtomField field;
    private IDroidefenseMethod method;
    private int[] lowerCodes, upperCodes, codes;
    private Throwable error;
    private AbstractAtomNode node;

    public InstructionReturn(IAtomFrame frame, IDroidefenseMethod method, int[] opcodes, int[] upperCodes, int[] mIdx, Throwable error) {
        this.frame = frame;
        this.method = method;
        this.lowerCodes = opcodes;
        this.upperCodes = upperCodes;
        this.codes = mIdx;
        this.error = error;
    }

    public InstructionReturn(IAtomFrame frame, IDroidefenseMethod method, int[] lowerCodes, int[] upperCodes, int[] codes, Throwable error, AbstractAtomNode node) {
        this(frame, method, lowerCodes, upperCodes, codes, error);
        this.node = node;
    }

    public InstructionReturn(IAtomFrame frame, IAtomField field) {
        this.frame = frame;
        this.field = field;
    }

    public InstructionReturn(VirtualMachineRuntimeException e) {
        this.error = e;
    }

    public IAtomFrame getFrame() {
        return frame;
    }

    public void setFrame(IAtomFrame frame) {
        this.frame = frame;
    }

    public IDroidefenseMethod getMethod() {
        return method;
    }

    public void setMethod(IDroidefenseMethod method) {
        this.method = method;
    }

    public int[] getLowerCodes() {
        return lowerCodes;
    }

    public void setLowerCodes(int[] lowerCodes) {
        this.lowerCodes = lowerCodes;
    }

    public int[] getUpperCodes() {
        return upperCodes;
    }

    public void setUpperCodes(int[] upperCodes) {
        this.upperCodes = upperCodes;
    }

    public int[] getCodes() {
        return codes;
    }

    public void setCodes(int[] codes) {
        this.codes = codes;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public IAtomField getField() {
        return field;
    }

    public void setField(IAtomField field) {
        this.field = field;
    }

    public AbstractAtomNode getNode() {
        return node;
    }
}
