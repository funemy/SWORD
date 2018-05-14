package avrora.arch.msp430;
import java.util.HashMap;

/**
 * The <code>MSP430Symbol</code> class represents a symbol (or an
 * enumeration as declared in the instruction set description) relevant
 * to the instruction set architecture. For example register names,
 * status bit names, etc are given here. This class provides a type-safe
 * enumeration for such symbolic names.
 */
public class MSP430Symbol {
    public final String symbol;
    public final int value;
    
    MSP430Symbol(String sym, int v) { symbol = sym;  value = v; }
    public int getValue() { return value; }
    public int getEncodingValue() { return value; }
    
    public static class GPR extends MSP430Symbol {
        private static HashMap set = new HashMap();
        private static GPR newGPR(String n, int v) {
            GPR obj = new GPR(n, v);
            set.put(n, obj);
            return obj;
        }
        GPR(String sym, int v) { super(sym, v); }
        public static final GPR PC = newGPR("pc", 0);
        public static final GPR SP = newGPR("sp", 1);
        public static final GPR SR = newGPR("sr", 2);
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
    }
    
    public static GPR get_GPR(String name) {
        return (GPR)GPR.set.get(name);
    }
    
}
