package avrora.arch.avr;

import java.util.HashMap;

/**
 * The <code>AVRSymbol</code> class represents a symbol (or an enumeration as declared in the instruction set
 * description) relevant to the instruction set architecture. For example register names, status bit names, etc are
 * given here. This class provides a type-safe enumeration for such symbolic names.
 */
public class AVRSymbol {

    public final String symbol;
    public final int value;

    AVRSymbol(String sym, int v) {
        symbol = sym;
        value = v;
    }

    public int getValue() {
        return value;
    }

    public int getEncodingValue() {
        return value;
    }

    public static class GPR extends AVRSymbol {

        private static HashMap set = new HashMap();

        private static GPR newGPR(String n, int v) {
            GPR obj = new GPR(n, v);
            set.put(n, obj);
            return obj;
        }

        GPR(String sym, int v) {
            super(sym, v);
        }

        public static final GPR R0 = newGPR("r0", 0);
        public static final GPR R1 = newGPR("r1", 1);
        public static final GPR R2 = newGPR("r2", 2);
        public static final GPR R3 = newGPR("r3", 3);
        public static final GPR R4 = newGPR("r4", 4);
        public static final GPR R5 = newGPR("r5", 5);
        public static final GPR R6 = newGPR("r6", 6);
        public static final GPR R7 = newGPR("r7", 7);
        public static final GPR R8 = newGPR("r8", 8);
        public static final GPR R9 = newGPR("r9", 9);
        public static final GPR R10 = newGPR("r10", 10);
        public static final GPR R11 = newGPR("r11", 11);
        public static final GPR R12 = newGPR("r12", 12);
        public static final GPR R13 = newGPR("r13", 13);
        public static final GPR R14 = newGPR("r14", 14);
        public static final GPR R15 = newGPR("r15", 15);
        public static final GPR R16 = newGPR("r16", 16);
        public static final GPR R17 = newGPR("r17", 17);
        public static final GPR R18 = newGPR("r18", 18);
        public static final GPR R19 = newGPR("r19", 19);
        public static final GPR R20 = newGPR("r20", 20);
        public static final GPR R21 = newGPR("r21", 21);
        public static final GPR R22 = newGPR("r22", 22);
        public static final GPR R23 = newGPR("r23", 23);
        public static final GPR R24 = newGPR("r24", 24);
        public static final GPR R25 = newGPR("r25", 25);
        public static final GPR R26 = newGPR("r26", 26);
        public static final GPR R27 = newGPR("r27", 27);
        public static final GPR R28 = newGPR("r28", 28);
        public static final GPR R29 = newGPR("r29", 29);
        public static final GPR R30 = newGPR("r30", 30);
        public static final GPR R31 = newGPR("r31", 31);
    }

    public static GPR get_GPR(String name) {
        return (GPR)GPR.set.get(name);
    }

    public static class ADR extends AVRSymbol {

        private static HashMap set = new HashMap();

        private static ADR newADR(String n, int v) {
            ADR obj = new ADR(n, v);
            set.put(n, obj);
            return obj;
        }

        ADR(String sym, int v) {
            super(sym, v);
        }

        public static final ADR X = newADR("X", 26);
        public static final ADR Y = newADR("Y", 28);
        public static final ADR Z = newADR("Z", 30);
    }

    public static ADR get_ADR(String name) {
        return (ADR)ADR.set.get(name);
    }

    public static class HGPR extends AVRSymbol.GPR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static HGPR newHGPR(String n, int v, int ev) {
            HGPR obj = new HGPR(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        HGPR(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final HGPR R16 = newHGPR("r16", 16, 0);
        public static final HGPR R17 = newHGPR("r17", 17, 1);
        public static final HGPR R18 = newHGPR("r18", 18, 2);
        public static final HGPR R19 = newHGPR("r19", 19, 3);
        public static final HGPR R20 = newHGPR("r20", 20, 4);
        public static final HGPR R21 = newHGPR("r21", 21, 5);
        public static final HGPR R22 = newHGPR("r22", 22, 6);
        public static final HGPR R23 = newHGPR("r23", 23, 7);
        public static final HGPR R24 = newHGPR("r24", 24, 8);
        public static final HGPR R25 = newHGPR("r25", 25, 9);
        public static final HGPR R26 = newHGPR("r26", 26, 10);
        public static final HGPR R27 = newHGPR("r27", 27, 11);
        public static final HGPR R28 = newHGPR("r28", 28, 12);
        public static final HGPR R29 = newHGPR("r29", 29, 13);
        public static final HGPR R30 = newHGPR("r30", 30, 14);
        public static final HGPR R31 = newHGPR("r31", 31, 15);
    }

    public static HGPR get_HGPR(String name) {
        return (HGPR)HGPR.set.get(name);
    }

    public static class EGPR extends AVRSymbol.GPR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static EGPR newEGPR(String n, int v, int ev) {
            EGPR obj = new EGPR(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        EGPR(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final EGPR R0 = newEGPR("r0", 0, 0);
        public static final EGPR R2 = newEGPR("r2", 2, 1);
        public static final EGPR R4 = newEGPR("r4", 4, 2);
        public static final EGPR R6 = newEGPR("r6", 6, 3);
        public static final EGPR R8 = newEGPR("r8", 8, 4);
        public static final EGPR R10 = newEGPR("r10", 10, 5);
        public static final EGPR R12 = newEGPR("r12", 12, 6);
        public static final EGPR R14 = newEGPR("r14", 14, 7);
        public static final EGPR R16 = newEGPR("r16", 16, 8);
        public static final EGPR R18 = newEGPR("r18", 18, 9);
        public static final EGPR R20 = newEGPR("r20", 20, 10);
        public static final EGPR R22 = newEGPR("r22", 22, 11);
        public static final EGPR R24 = newEGPR("r24", 24, 12);
        public static final EGPR R26 = newEGPR("r26", 26, 13);
        public static final EGPR R28 = newEGPR("r28", 28, 14);
        public static final EGPR R30 = newEGPR("r30", 30, 15);
    }

    public static EGPR get_EGPR(String name) {
        return (EGPR)EGPR.set.get(name);
    }

    public static class MGPR extends AVRSymbol.GPR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static MGPR newMGPR(String n, int v, int ev) {
            MGPR obj = new MGPR(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        MGPR(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final MGPR R16 = newMGPR("r16", 16, 0);
        public static final MGPR R17 = newMGPR("r17", 17, 1);
        public static final MGPR R18 = newMGPR("r18", 18, 2);
        public static final MGPR R19 = newMGPR("r19", 19, 3);
        public static final MGPR R20 = newMGPR("r20", 20, 4);
        public static final MGPR R21 = newMGPR("r21", 21, 5);
        public static final MGPR R22 = newMGPR("r22", 22, 6);
        public static final MGPR R23 = newMGPR("r23", 23, 7);
    }

    public static MGPR get_MGPR(String name) {
        return (MGPR)MGPR.set.get(name);
    }

    public static class YZ extends AVRSymbol.ADR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static YZ newYZ(String n, int v, int ev) {
            YZ obj = new YZ(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        YZ(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final YZ Y = newYZ("Y", 28, 1);
        public static final YZ Z = newYZ("Z", 30, 0);
    }

    public static YZ get_YZ(String name) {
        return (YZ)YZ.set.get(name);
    }

    public static class RDL extends AVRSymbol.GPR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static RDL newRDL(String n, int v, int ev) {
            RDL obj = new RDL(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        RDL(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final RDL R24 = newRDL("r24", 24, 0);
        public static final RDL R26 = newRDL("r26", 26, 1);
        public static final RDL R28 = newRDL("r28", 28, 2);
        public static final RDL R30 = newRDL("r30", 30, 3);
    }

    public static RDL get_RDL(String name) {
        return (RDL)RDL.set.get(name);
    }

    public static class R0 extends AVRSymbol.GPR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static R0 newR0(String n, int v, int ev) {
            R0 obj = new R0(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        R0(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final R0 R0 = newR0("r0", 0, 0);
    }

    public static R0 get_R0(String name) {
        return (R0)R0.set.get(name);
    }

    public static class RZ extends AVRSymbol.ADR {

        public final int encoding;

        public int getEncodingValue() {
            return encoding;
        }

        private static HashMap set = new HashMap();

        private static RZ newRZ(String n, int v, int ev) {
            RZ obj = new RZ(n, v, ev);
            set.put(n, obj);
            return obj;
        }

        RZ(String sym, int v, int ev) {
            super(sym, v);
            encoding = ev;
        }

        public static final RZ Z = newRZ("Z", 30, 0);
    }

    public static RZ get_RZ(String name) {
        return (RZ)RZ.set.get(name);
    }

}
