package org.skr.SkrScript;


/**
 * Created by rat on 23.11.14.
 */
public class Operators {

//    private static final Value rv = new Value();
//    private static final Value lv = new Value();

    public static boolean execOp(byte opCode, RunContext rc) {

        switch (opCode) {
            case Def.OP_NOT:
                return opNot( rc );
            case Def.OP_U_ADD:
                // funny operator, it does nothing
                return true;
            case Def.OP_U_SUB:
                return opUnSub( rc );
            case Def.OP_TYPEOF:
                return opTypeOf( rc );
            case Def.OP_DIV:
            case Def.OP_MUL:
            case Def.OP_ADD:
            case Def.OP_SUB:
            case Def.OP_LESS:
            case Def.OP_GRT:
            case Def.OP_LOEQ:
            case Def.OP_GOEQ:
//                boolean res = opArithmetic( opCode, rc );
//                printMsg("execOp", "op: " + ScriptDumper.getOpCodeStr( opCode) + " res: " + rc.lval.toString(), rc );
//                return  res;
                return opArithmetic( opCode, rc );
            case Def.OP_NOT_EQ:
                return opNotEqual( rc );
            case Def.OP_EQ:
                return opEqual( rc );
            case Def.OP_AND:
            case Def.OP_OR:
                return opAndOr( opCode, rc );
            case Def.OP_ASSIGN:
                return opAssign( rc );
            case Def.OP_GET_PROP:
                return PropertyAccess.getProperty(rc);
            case Def.OP_GET_PROP_REF:
                return opGetPropRef( rc );
        }
        Engine.printError("execOp", "Unknown op. opCode: " + opCode, rc);
        return false;
    }

    private static boolean opGetPropRef(RunContext rc) {
        rc.obtainLv();
        rc.obtainRv();
        if ( ! rc.r.isPropertyCode() ) {
            return Engine.printError("opGetPropRef", "rvalue is not a property", rc);
        }
        rc.pr.obj.set( rc.l );
        rc.pr.prop = (Integer) rc.r.val;
        rc.l.val = rc.pr;
        rc.l.dts = Def.DTS_PROP_REF;
        return true;
    }

    public static boolean opNot( RunContext rc ) {
        rc.obtainRv();
        if ( ! rc.r.isBool() )
            return Engine.printError("opNot", "rvalue is not a boolean. dts: " + rc.r.dts, rc);
        rc.l.val = ! (Boolean) rc.r.val;
        rc.l.dts = rc.r.dts;
        return true;
    }

    public static boolean opUnSub( RunContext rc ) {
        rc.obtainRv();
        if ( !rc.r.isNumber())
            return Engine.printError("opUnSub", "rvalue is not a number type. dts: " + rc.r.dts, rc);
        rc.l.dts = rc.r.dts;
        rc.l.val = - (Float) rc.r.val;
        return true;
    }

    public static boolean opTypeOf( RunContext rc ) {
//        printMsg("opTypeOf: ", "rv: " + rc.r, rc);
        rc.obtainRv();
//        printMsg("opTypeOf: ", "obtained rv: " + rc.r, rc);
        rc.l.dts = Def.DTS_TYPE;
        rc.l.val = rc.r.dts;
//        printMsg("opTypeOf: ", "lv: " + rc.l, rc);
        return true;
    }

    public static boolean opArithmetic( byte opCode, RunContext rc ) {
//        Engine.printMsg("opArithmetic.", "lval: " + rc.l + " rval: " + rc.r + " op: " + Dumper.getOpCodeStr( opCode), rc);
        rc.obtainLv();
        rc.obtainRv();

//        Engine.printMsg("opArithmetic.", "Obtained: lval: " + rc.l + " rval: " + rc.r + " op: " + Dumper.getOpCodeStr( opCode), rc);

        switch ( rc.l.dts ) {
            case Def.DTS_NUMBER:
                return opNumberArithmetic(opCode, rc );
            case Def.DTS_STRING:
//                Engine.printMsg("opArithmetic.", "rval: " + rv.toString() + " op: " + ScriptDumper.getOpCodeStr( opCode), rc);
                if ( opCode != Def.OP_ADD )
                    return Engine.printError("opArithmetic", "Unsupported string op. opCode: " + opCode, rc);
                rc.l.dts = Def.DTS_STRING;
                if ( !rc.r.isString() )
                    TypeCast.cast(rc.r, Def.DTS_STRING, rc);
                rc.l.val = (String)rc.l.val + rc.r.val;
                return true;
            case Def.DTS_TYPE:
            case Def.DTS_BOOL:
            case Def.DTS_NULL:
                return Engine.printError("opArithmetic", "opCode: " + opCode + ". illegal lvalue type. dts: " + rc.l.dts, rc);
        }
        if ( rc.extension != null )
            return rc.extension.opArithmetic( opCode, rc.l, rc.r, rc.l, rc );
        return Engine.printError("opArithmetic", "opCode: " + opCode + ". illegal lvalue type. dts: " + rc.l.dts, rc);
    }

    protected static boolean opNumberArithmetic(byte opCode,  RunContext rc ) {
//        printMsg("opNumberArithmetic.", "rval: " + rc.r + " op: " + Dumper.getOpCodeStr( opCode), rc);

        if ( ! rc.r.isNumber() ) {
            if ( ! TypeCast.cast(rc.r, Def.DTS_NUMBER, rc) )
                return Engine.printError("opNumberArithmetic", "rvalue is not a number. dts: " + rc.r.dts, rc);
        }

        rc.l.dts = Def.DTS_NUMBER;
        switch ( opCode ) {
            case Def.OP_ADD:
                rc.l.val = (Float) rc.l.val + (Float) rc.r.val;
                return true;
            case Def.OP_SUB:
                rc.l.val = (Float) rc.l.val - (Float) rc.r.val;
                return true;
            case Def.OP_MUL:
                rc.l.val = (Float) rc.l.val * (Float) rc.r.val;
                return true;
            case Def.OP_DIV:
                rc.l.val = (Float) rc.l.val / (Float) rc.r.val;
                return true;
        }

        rc.l.dts = Def.DTS_BOOL;
        switch ( opCode ) {
            case Def.OP_LESS:
                rc.l.val = (Float) rc.l.val < (Float) rc.r.val;
                return true;
            case Def.OP_LOEQ:
                rc.l.val = (Float) rc.l.val <= (Float) rc.r.val;
                return true;
            case Def.OP_GRT:
                rc.l.val = (Float) rc.l.val > (Float) rc.r.val;
                return true;
            case Def.OP_GOEQ:
                rc.l.val = (Float) rc.l.val >= (Float) rc.r.val;
                return true;
        }
        return Engine.printError("opNumberArithmetic", "Unexpected opCode. opCode: " + opCode, rc);
    }

    public static boolean opEqual( RunContext rc ) {
        rc.obtainRv();
        rc.obtainLv();

        if ( !TypeCast.cast(rc.r, rc.l.dts, rc) )
            return false;

        rc.l.dts = Def.DTS_BOOL;
        if ( rc.l.val == null )
            return rc.r.val == null;
        rc.l.val = rc.l.val.equals( rc.r.val );
        return true;
    }

    public static boolean opNotEqual( RunContext rc ) {
        rc.obtainRv();
        rc.obtainLv();

        if ( !TypeCast.cast(rc.r, rc.l.dts, rc) )
            return false;

        rc.l.dts = Def.DTS_BOOL;
        if ( rc.l.val == null)
            return rc.r.val != null;

        rc.l.val = ! rc.l.val.equals( rc.r.val );
        return true;
    }

    public static boolean opAndOr(byte opCode, RunContext rc ) {
        rc.obtainLv();
        rc.obtainRv();

        if ( !rc.l.isBool() )
            return Engine.printError("opAndOr", "LValue is not a boolean. dts: " + rc.l.dts, rc);

        if ( !rc.r.isBool() )
            return Engine.printError("opAndOr", "RValue is not a boolean. dts: " + rc.r, rc);

        rc.l.dts = Def.DTS_BOOL;
        Boolean a = (Boolean) rc.l.val;
        Boolean b = (Boolean) rc.r.val;

        if ( opCode == Def.OP_AND )
            rc.l.val = ( a && b );
        else if ( opCode == Def.OP_OR )
            rc.l.val = ( a || b );
        return true;
    }

    protected static boolean opAssign( RunContext rc ) {
//        printMsg("opAssign", "l: " + rc.l + " r: " + rc.r, rc);
        return ( rc.obtainRv() != null ) && rc.rvToLvVar();
    }

/*
        rc.obtainRv( rv );
        rc.obtainLv( lv );

        switch ( rv.dts ) {
            case ScriptCodes.DTS_NULL:
                return true;
            case ScriptCodes.DTS_REG:
            case ScriptCodes.DTS_VAR:
            case ScriptCodes.DTS_BOOL:
            case ScriptCodes.DTS_NUMBER:
            case ScriptCodes.DTS_STRING:
            case ScriptCodes.DTS_TYPE:
            default:

        }
        printError("", "illegal rvalue type. dts: " + rv.dts, rc);
        return false;
*/

}