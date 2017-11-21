package droidefense.om.flow.experimental;

import droidefense.rulengine.map.BasicCFGFlowMap;
import droidefense.rulengine.nodes.EntryPointNode;
import droidefense.sdk.log4j.Log;
import droidefense.sdk.log4j.LoggerType;
import droidefense.om.flow.base.AbstractFlowWorker;
import droidefense.om.machine.base.AbstractDVMThread;
import droidefense.om.machine.base.DalvikVM;
import droidefense.om.machine.base.exceptions.NoMainClassFoundException;
import droidefense.om.machine.base.struct.generic.IAtomClass;
import droidefense.om.machine.base.struct.generic.IAtomFrame;
import droidefense.om.machine.base.struct.generic.IAtomMethod;
import droidefense.om.machine.inst.DalvikInstruction;
import droidefense.om.machine.inst.InstructionReturn;
import droidefense.om.machine.reader.DexClassReader;
import droidefense.sdk.model.base.DroidefenseProject;
import droidefense.sdk.model.base.ExecutionTimer;

import java.util.ArrayList;
import java.util.Vector;

public final strictfp class RealFlowWorker extends AbstractFlowWorker {

    public RealFlowWorker(DroidefenseProject project) {
        super(new DalvikVM(project), project);
        flowMap = new BasicCFGFlowMap();
    }

    public RealFlowWorker(final DalvikVM vm, DroidefenseProject project) {
        super(vm, project);
        flowMap = new BasicCFGFlowMap();
    }

    @Override
    public void preload() {
        Log.write(LoggerType.DEBUG, "WORKER: RealFlowWorker");
        this.setStatus(AbstractDVMThread.STATUS_NOT_STARTED);
        vm.setThreads(new Vector());
        vm.addThread(this);
    }

    @Override
    public void run() throws Throwable {
        //get main class and load
        if (currentProject.hasMainClass()) {
            DexClassReader.getInstance().load(currentProject.getMainClassName());
            execute(true);
        } else {
            throw new NoMainClassFoundException(currentProject.getProjectName() + " >> check main class manually");
        }
    }

    @Override
    public void finish() {
        Log.write(LoggerType.DEBUG, "WORKER: RealFlowWorker FINISHED!");
    }

    @Override
    public IAtomMethod[] getInitialMethodToRun(IAtomClass clazz) {
        ArrayList<IAtomMethod> list = new ArrayList<>();
        /*IAtomMethod[] l0 = clazz.getMethod("<init>");
        for (IAtomMethod m : l0) {
            list.add(m);
        }*/
        list.add(clazz.getMethod("onCreate", "(Landroid/os/Bundle;)V", true));
        return list.toArray(new IAtomMethod[list.size()]);
    }

    @Override
    public int getInitialArgumentCount(IAtomClass cls, IAtomMethod m) {
        return 0;
    }

    @Override
    public Object getInitialArguments(IAtomClass cls, IAtomMethod m) {
        return null;
    }

    @Override
    public IAtomClass[] getInitialDVMClass() {
        //get all
        if (currentProject.hasMainClass())
            return new IAtomClass[]{currentProject.getInternalInfo().getDexClass(currentProject.getMainClassName())};
        else {
            //else, return all reveivers, services,...
            IAtomClass[] alllist = currentProject.getInternalInfo().getAllClasses();
            ArrayList<IAtomClass> developerClasses = new ArrayList<>();
            for (IAtomClass cls : alllist) {
                if (environment.isDeveloperClass(cls.getName()))
                    developerClasses.add(cls);
            }
            return developerClasses.toArray(new IAtomClass[developerClasses.size()]);
        }
    }

    @Override
    public AbstractDVMThread reset() {
        //reset 'thread' status
        this.setStatus(STATUS_NOT_STARTED);
        this.removeFrames();
        this.timestamp = new ExecutionTimer();
        return this;
    }

    @Override
    public strictfp void execute(boolean endless) throws Throwable {

        IAtomFrame frame = getCurrentFrame();
        IAtomMethod method = frame.getMethod();

        int[] lowerCodes = method.getOpcodes();
        int[] upperCodes = method.getRegistercodes();
        int[] codes = method.getIndex();

        fromNode = EntryPointNode.builder();

        while (endless) {
            try {
                //1 ask if we have more inst to execute
                if (frame.getPc() >= lowerCodes.length || getFrames() == null || getFrames().isEmpty())
                    break;
                //skip sdk classes for faster execution
                if (method.isFake()) {
                    popFrame();
                    frame = getCurrentFrame();
                    if (frame != null) {
                        method = frame.getMethod();
                        if (method != null) {
                            upperCodes = method.getRegistercodes();
                            lowerCodes = method.getOpcodes();
                            codes = method.getIndex();
                            continue;
                        }
                    }
                    break;
                }
                int instVal = lowerCodes[frame.getPc()];
                System.out.println("DalvikInstruction: 0x" + Integer.toHexString(instVal));
                DalvikInstruction currentInstruction = instructions[instVal];
                InstructionReturn returnValue = currentInstruction.execute(flowMap, this, lowerCodes, upperCodes, codes, DalvikInstruction.CFG_EXECUTION);
                if (returnValue != null) {
                    //first check for errors in DalvikInstruction execution
                    if (returnValue.getError() != null) {
                        throw returnValue.getError();
                    }
                    //if no errors, update values
                    frame = returnValue.getFrame();
                    method = returnValue.getMethod();
                    upperCodes = returnValue.getUpperCodes();
                    lowerCodes = returnValue.getLowerCodes();
                    codes = returnValue.getCodes();
                    toNode = returnValue.getNode();
                    //save node connection
                    createNewConnection(fromNode, toNode, currentInstruction);
                }
            } catch (Throwable e) {
                frame = handleThrowable(e, frame);
                method = frame.getMethod();
                lowerCodes = method.getOpcodes();
                upperCodes = method.getRegistercodes();
                codes = method.getIndex();
            }
        }
    }
}