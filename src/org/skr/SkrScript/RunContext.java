package org.skr.SkrScript;

import java.nio.ByteBuffer;

/**
 * Created by rat on 22.11.14.
 */

public class RunContext {

    public static class PropertyRef {

        public final Value obj = new Value();
        public int prop;

        @Override
        public String toString() {
            return obj.toString() + " @ " + prop;
        }
    }

    EngineExtension extension = null;

    Stack<Integer> varOffsetStack;
    Stack<Integer> varNumStack;
    Stack<Integer> retCodeStack;

    ValueStack valueStack;

    Slot slot;
    Script script;
    byte [] bytes;

    int pos = 0;
    int opPos = 0;
    int varNum = 0;
    int retCode = -1;

    ValuePool vars = new ValuePool( 128 );
    ValuePool regs;

    boolean outEnabled = true;
    boolean errEnabled = true;

    public final Value l = new Value();
    public final Value r = new Value();
    public final PropertyRef pr = new PropertyRef();



    public RunContext(int stackSize, int valueStackSize) {
        if ( stackSize > 0 ) {
            varNumStack = new Stack<Integer>( stackSize );
            varOffsetStack = new Stack<Integer>( stackSize );
            retCodeStack = new Stack<Integer>( stackSize );
        }
        if ( valueStackSize > 0 ) {
            valueStack = new ValueStack( valueStackSize );
        }
    }

    public boolean isOutEnabled() {
        return outEnabled;
    }

    public void setOutEnabled(boolean outEnabled) {
        this.outEnabled = outEnabled;
    }

    public boolean isErrEnabled() {
        return errEnabled;
    }

    public void setErrEnabled(boolean errEnabled) {
        this.errEnabled = errEnabled;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
        this.script = slot.script;
        this.bytes = script.bytes;
        this.regs = slot.registers;
    }

    public Slot getSlot() {
        return slot;
    }

    public void reset() {
        pos = 0;
        vars.offset =  0 ;
        varNum = 0;
        retCode = -1;
        varOffsetStack.reset();
        varNumStack.reset();
        retCodeStack.reset();
        valueStack.reset();
        extension = null;
    }

    public void setExtension( EngineExtension extension ) {
        this.extension = extension;
    }

    protected boolean hasMoreBytes() {
        return ( pos < bytes.length );
    }

    protected byte nextByte() {
        return bytes[pos++];
    }

    private static final ByteBuffer tmpBb = ByteBuffer.allocateDirect(4);

    protected int readInt() {
        tmpBb.put(0, bytes[ pos++ ] );
        tmpBb.put(1, bytes[ pos++ ] );
        tmpBb.put(2, bytes[ pos++ ] );
        tmpBb.put(3, bytes[ pos++ ] );
        return tmpBb.getInt(0);
    }

    protected float readFloat() {
        tmpBb.put(0, bytes[ pos++ ] );
        tmpBb.put(1, bytes[ pos++ ] );
        tmpBb.put(2, bytes[ pos++ ] );
        tmpBb.put(3, bytes[ pos++ ] );
        return tmpBb.getFloat(0);
    }

    protected void pushEnv() {
        varOffsetStack.push( vars.offset );
        varNumStack.push( varNum );
        retCodeStack.push( retCode );
    }

    protected void popEnv() {
        vars.offset = varOffsetStack.pop();
        varNum = varNumStack.pop();
        retCode = retCodeStack.pop();
    }

    protected void pushLv() {
        valueStack.push( l );
//        Engine.printMsg("pushLv", l.toString(), this );
    }

    protected void pushRv() {
        valueStack.push( r );
//        Engine.printMsg("pushRv", r.toString(), this );
    }
    protected void popLv() {
        valueStack.pop( l );
//        Engine.printMsg("popLv", l.toString(), this );
    }

    protected void popRv() {
        valueStack.pop( r );
//        Engine.printMsg("popRv", r.toString(), this );
    }

    protected void popArg( ValuePool args, int index ) {
        valueStack.pop( args.get( index ) );
    }

    protected void fcall( int addr, byte argNumber ) {

        if ( addr >= bytes.length )
            return;

        pushEnv();

        retCode = this.pos;
        vars.offset += varNum;
        varNum = argNumber;
        pos = addr;

//        Engine.printMsg("fcall", "addr: " + pos + " retCode: " + retCode, this );
    }

    protected void ret( ) {
//        Engine.printMsg("ret.", "lval: " + lval.toString(), this);
        pos = retCode;
        popEnv();
    }

    protected void jumpF( int addr ) {
        if ( ! l.isBool() )
            return;
        if ( ! l.asBool( this ) )
            pos = addr;
    }

    protected void lvToRv() {
        r.set( l );
    }

    protected void rvToLv() {
        l.set( r );
    }

    protected Value obtainRv() {
        return r.obtain( this );
    }

    protected Value obtainLv() {
        return l.obtain( this );
    }

    protected boolean rvToLvVar() {
        return l.setVar( r, this );
    }

    protected void readValue( Value v ) {
        byte dts = nextByte();

        switch ( dts ) {
            case Def.DTS_NULL:
                v.setAsNull();
                return;
            case Def.DTS_REG:
                v.setAsReg( readInt() );
                return;
            case Def.DTS_VAR:
                v.setAsVar( readInt() );
                return;
            case Def.DTS_INT:
                v.setAsInt( readInt() );
                return;
            case Def.DTS_FLOAT:
                v.setAsFloat( readFloat() );
                return;
            case Def.DTS_BOOL:
                v.setAsBool( nextByte() != 0 );
                return;
            case Def.DTS_STRING:
                v.setAsString( script.strings[ readInt() ] );
                return ;
            case Def.DTS_TYPE:
                v.set( nextByte(), dts );
                return;
            case Def.DTS_PROP_CODE:
                v.setAsPropCode( readInt() );
                return;
            default:
                Engine.printError("readValue", "Undefined dts: " + dts, this);
        }
    }

    protected static boolean run( int entryPoint, RunContext rc ) {

        rc.pos = entryPoint;

//        printMsg("run", "entryPoint: " + entryPoint, rc);


        while ( rc.hasMoreBytes() ) {
            rc.opPos = rc.pos;
            byte opCode = rc.nextByte();
//            printMsg("run", "opCode: " + Builder.getOpName( opCode ), rc);
            switch ( opCode ) {
                case Def.SETRV:
                    rc.readValue(rc.r);
                    continue;
                case Def.SETLV:
                    rc.readValue( rc.l );
                    continue;
                case Def.POPRV:
                    rc.popRv();
                    continue;
                case Def.PUSHRV:
                    rc.pushRv();
                    continue;
                case Def.POPLV:
                    rc.popLv();
                    continue;
                case Def.PUSHLV:
                    rc.pushLv();
                    continue;
                case Def.JUMP:
                    rc.pos = rc.readInt();
                    continue;
                case Def.JUMPF:
                    rc.jumpF( rc.readInt() );
                    continue;
                case Def.RET:
                    if ( rc.retCode < 0 ) {
//                        printMsg("run", "finalize", rc);
                        return true;
                    }
                    rc.ret( );
                    continue;
                case Def.INCVARNUM:
                    rc.varNum++;
                    continue;
                case Def.DECVARNUM:
                    int count = rc.readInt();
                    rc.varNum -= count;
                    continue;
                case Def.OBTAINLV:
                    rc.obtainLv();
                    continue;
                case Def.OBTAINRV:
                    rc.obtainRv();
                    continue;
                case Def.LVTORV:
                    rc.lvToRv();
                    continue;
                case Def.RVTOLV:
                    rc.rvToLv();
                    continue;
                case Def.OP_F_CALL:
                    int faddr = rc.readInt();
                    byte numOfArg = rc.nextByte();
                    if ( faddr <= Def.FUNCTIONS_START_ADDR ) {
                        if ( ! BuildInFunctions.call(faddr, rc, numOfArg) )
                            return false;
                        continue;
                    }
                    rc.fcall( faddr, numOfArg );
                    continue;
            }

            if ( Def.isOperator(opCode) ) {
//                Engine.printMsg("run", "op: " + ScriptDumper.getOpCodeStr( opCode ), rc);
                if ( ! Operators.execOp(opCode, rc) )
                    return false;
//                Engine.printMsg("run", "op: " + Dumper.getOpCodeStr(opCode) + " res: lval: " + rc.l, rc);
                continue;
            }

            Engine.printError("run", "Undefined opCode: " + opCode, rc);
            break;
        }

        return Engine.printError("run", "Unexpected code finalization", rc);
    }
}
