/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.shrikeCT;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;

public class BootstrapMethodsReader extends AttributeReader {

  public interface BootstrapMethod {
    int invokeType();
    String methodClass();
    String methodName();
    String methodType();
    Object callArgument(ClassLoader cl, int i);
    int callArgumentKind(int i);
  }
  
  private BootstrapMethod entries[];
  
  protected BootstrapMethodsReader(AttrIterator attr) throws InvalidClassFileException {
    super(attr, "BootstrapMethods");
    readBootstrapEntries();
  }

  private void readBootstrapEntries() throws InvalidClassFileException {
    final ConstantPoolParser cp = cr.getCP();

    entries = new BootstrapMethod[cr.getUShort(attr + 6)];
    int base = 8;
    for(int i = 0; i < entries.length; i++) {
      final int methodHandleOffset = cr.getUShort(attr + base);
      final int argsBase = attr + base + 4;
      
      final int argumentCount = cr.getUShort(attr + base + 2);
      entries[i] = new BootstrapMethod() {
        private final int invokeType = cp.getCPHandleKind(methodHandleOffset);
        private final String methodClass = cp.getCPHandleClass(methodHandleOffset);
        private final String methodName = cp.getCPHandleName(methodHandleOffset);
        private final String methodType = cp.getCPHandleType(methodHandleOffset);
        
        @Override
        public String toString() {
          return methodClass + ":" + methodName + methodType;
        }
        
        @Override
        public int invokeType() {
          return invokeType;
        }

        @Override
        public String methodClass() {
          return methodClass;
        }

        @Override
        public String methodName() {
          return methodName;
        }

        @Override
        public String methodType() {
          return methodType;
        }

        @Override
        public int callArgumentKind(int i) {
          assert 0 <= i && i < argumentCount;
          int index = argsBase + (2*i);
          return cp.getItemType(cr.getUShort(index));
        }
        
        @Override
        public Object callArgument(ClassLoader cl, int i) {
          try {
            int index = cr.getUShort(argsBase + (2*i));
            int t = callArgumentKind(i);
            switch (t) {
            case ClassConstants.CONSTANT_Utf8:
              return cp.getCPUtf8(index);
            case ClassConstants.CONSTANT_Class:
              return cp.getCPClass(index);
            case ClassConstants.CONSTANT_String:
              return cp.getCPString(index);
            case ClassConstants.CONSTANT_Integer:
              return cp.getCPInt(index);
            case ClassConstants.CONSTANT_Float:
              return cp.getCPFloat(index);
            case ClassConstants.CONSTANT_Double:
              return cp.getCPDouble(index);
            case ClassConstants.CONSTANT_Long:
              return cp.getCPLong(index);
            case ClassConstants.CONSTANT_MethodHandle:
              String className = cp.getCPHandleClass(index);
              Class<?> cls = Class.forName(className, false, cl);
              String eltName = cp.getCPHandleName(index);
              String eltDesc = cp.getCPHandleType(index);
              MethodType type = MethodType.fromMethodDescriptorString(eltDesc, cl);
              Method m = cls.getDeclaredMethod(eltName, type.parameterList().toArray(new Class[type.parameterCount()]));
              Lookup lk = MethodHandles.lookup().in(cls);
              m.setAccessible(true);
              return lk.unreflect(m);
            case ClassConstants.CONSTANT_MethodType:
              return MethodType.fromMethodDescriptorString(cp.getCPMethodType(index), this.getClass().getClassLoader());
            default:
              assert false : "invalid type " + t;
            }            
          } catch (IllegalArgumentException e) {
            assert false : e;
          } catch (InvalidClassFileException e) {
            assert false : e;
          } catch (ClassNotFoundException e) {
            assert false : e;
          } catch (NoSuchMethodException e) {
            assert false : e;
          } catch (SecurityException e) {
            assert false : e;
          } catch (IllegalAccessException e) {
            assert false : e;
          }
          return null;
        }
      };
      
      base += (argumentCount*2) + 4;
    }
  }

  public int count() {
    return entries.length;
  }
  
  public BootstrapMethod getEntry(int i) {
    return entries[i];
  }
}
