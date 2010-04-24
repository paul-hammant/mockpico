package com.thoughtworks.mockpico;
/**
 * Copyright (c) 2010 ThoughtWorks
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.NoInteractionsWanted;
import org.picocontainer.MutablePicoContainer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sun.tools.internal.ws.wsdl.parser.Util.fail;
import static com.thoughtworks.mockpico.Mockpico.annotatedMethodInjection;
import static com.thoughtworks.mockpico.Mockpico.makePicoContainer;
import static com.thoughtworks.mockpico.Mockpico.mockDepsFor;
import static com.thoughtworks.mockpico.Mockpico.resetAll;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.picocontainer.injectors.Injectors.CDI;

public class MockpicoTestCase {

    private static C c = new C();
    private static D d = new D();
    private static B b = new B(c);

    @Test
    public void testCanMockConstructorAndSetterDepsWhenNotInjected() {

        A a = mockDepsFor(A.class)
                .withSetters()
                .make();

        assertEquals(aMadeWith(mockCandB())
                + setterCalledWith(mockD()), a.toString());
    }

    @Test
    public void testCanUseRealConstructorAndSetterDepsWhenInjected() {

        A a = mockDepsFor(A.class)
                .withSetters()
                .withInjectees(b, c, d)
                .make();

        assertEquals(aMadeWith(memberVarsCandB())
                + setterCalledWith(memberVarD()), a.toString());
    }

    @Test
    public void testPicoCanMakeFromTypesAndCacheDeps() {

        A a = mockDepsFor(A.class)
                .withSetters()
                .withInjectees(B.class, C.class, D.class)
                .make();

        assertEquals(aMadeWith(newCandB())
                + setterCalledWith(newD()), a.toString());
    }

    @Test
    public void testCanSetterInjectionIsNotDefault() {

        A a = mockDepsFor(A.class)
                .withInjectees(b, c)
                .make();

        assertEquals(aMadeWith(memberVarsCandB())
                + atInjectMethodCalledWithBmemberVar(memberVarB())
                + autowiredMethodCalledWith(memberVarB())
                + autowiredFieldSetTo(memberVarB())
                + atInjectFieldSetTo(memberVarB())
                , a.toString());
    }

    @Test
    public void testCanSpecifyConstructorInjectionOnly() {

        A a = mockDepsFor(A.class)
                .withInjectionTypes(CDI())
                .withInjectees(b, c, d)
                .make();

        assertEquals(aMadeWith(memberVarsCandB()), a.toString());
    }

    @Test
    public void testCanUseAPicoContainerHandedInAndJournalInjectionsToSpecialObject() {
        MutablePicoContainer pico = makePicoContainer();

        A a = mockDepsFor(A.class)
                .using(pico)
                .make();

        assertEquals(aMadeWith(mockCandB())
                + setterCalledWith(mockD()), a.toString());

        String actual = pico.getComponent(Mockpico.Journal.class).toString();
        assertTrue(actual.indexOf("Constructor being injected:") > -1);
        assertTrue(actual.indexOf("  arg[0] type:class com.thoughtworks.mockpico.MockpicoTestCase$C, with: Mock for C, hashCode: ") > 0);
        assertTrue(actual.indexOf("  arg[1] type:class com.thoughtworks.mockpico.MockpicoTestCase$B, with: Mock for B, hashCode: ") > 0);
        assertTrue(actual.indexOf("Method being injected: 'setIt' with: Mock for D, hashCode: ") > 0);
    }

    @Test
    public void testCanMockConstructorAndDefaultInjecteesWhenNotSupplied() {

        A a = mockDepsFor(A.class)
                .make();

        assertEquals(aMadeWith(mockCandB())
                + atInjectMethodCalledWithBmemberVar(mockB())
                + autowiredMethodCalledWith(mockB())
                + autowiredFieldSetTo(mockB())
                + atInjectFieldSetTo(mockB()), a.toString());
    }

    @Test
    public void testCanUseMocksPassedIn() {
        List list1 = mock(List.class);

        NeedsList nl = mockDepsFor(NeedsList.class)
                .withInjectees(list1)
                .make();

        assertSame(list1, nl.list);
    }

    @Test
    public void testCanMockPrimivitesAndAlsoUseCustomAnnotation() {

        A bc = mockDepsFor(A.class)
                .using(makePicoContainer(CDI(), annotatedMethodInjection(A.Foobarred.class)))
                .make();

        assertEquals(aMadeWith(mockCandB()) + customAnnotatedMethodCalledWith(aBunchOfPrimitives()), bc.toString());
    }

    @Test
    public void verifyNoMoreInteractionsCanBePercolated() {
        MutablePicoContainer mocks = makePicoContainer();

        NeedsList nl = mockDepsFor(NeedsList.class)
                .using(mocks)
                .make();

        nl.oops();
        try {
            Mockpico.verifyNoMoreInteractionsForAll(mocks);
            fail("should have barfed");
        } catch (NoInteractionsWanted e) {
            // expected  
        }
    }

    @Test
    public void resetCanBePercolated() {
        MutablePicoContainer mocks = makePicoContainer();

        NeedsList nl = mockDepsFor(NeedsList.class)
                .using(mocks)
                .make();

        nl.oops();
        resetAll(mocks);
        Mockito.verifyNoMoreInteractions(mocks.getComponent(List.class));
    }

    public static class NeedsList {
        private List list;

        public NeedsList(List list) {
            this.list = list;
        }

        public void oops() {
            list.add("oops");
        }

    }


    public static class A {

        private StringBuilder sb = new StringBuilder();
        private Map<Object, String> printed = new HashMap<Object, String>();
        private int mocks;
        private int reals;

        @Autowired
        private B b1;

        @Inject
        private B b2;

        @Override
        public String toString() {
            String s = sb.toString();
            if (b1 != null) {
                s = s + ",b1=" + prt(b1);
            }
            if (b1 != null) {
                s = s + ",b2=" + prt(b2);
            }
            return s;
        }

        private String prt(Object obj) {
            String p = printed.get(obj);
            if (p == null) {
                if (obj.toString().indexOf("Mock for") > -1) {
                    Class<?> parent = obj.getClass().getSuperclass();
                    if (parent == Object.class) {
                        parent = obj.getClass().getInterfaces()[0];
                    }
                    p = "mock["+ parent.getName().substring(parent.getName().lastIndexOf('.')+1).replace("MockpicoTestCase$", "") +"]#" + mocks++;
                } else {
                    String name = obj.getClass().getName();
                    if (obj == b) {
                        return "b<c>";
                    } else if (obj == c) {
                        return "c";
                    } else if (obj == d) {
                        return "d";
                    }
                    p = name.substring(name.lastIndexOf(".")+1).replace("MockpicoTestCase$", "") + "#" + reals++;
                    if (obj instanceof B) {
                        p = p +  "<" + prt(((B) obj).c) + ">";
                    }
                }
                printed.put(obj, p);
            }
            return p;
        }


        public A(C c, B b) {
            sb.append("A(" + prt(c) + "," + prt(b) + ")");
        }

        public void setIt(D d) {
            sb.append(",setIt(" + prt(d) + ")");
        }

        @Inject
        public void inj3ct(B b) {
            sb.append(",inj3ct(" + prt(b) + ")");
        }

        @Autowired
        public void aut0wireMe(B b) {
            sb.append(",aut0wireMe(" + prt(b) + ")");
        }

        @Foobarred
        public void foobar(String str, int iint,
                           double dbl,
                           Double dbl2,
                           boolean bool,
                           float flt,
                           byte byt,
                           short shrt,
                           BigInteger bigInt,
                           char chr,
                           Long lng) {

            sb.append(",foobar(" + prt(str) + "," +
                    prt(iint) + "," +
                    prt(dbl) + "," +
                    prt(dbl2) + "," +
                    prt(flt) + "," +
                    prt(byt) + "," +
                    prt(shrt) + "," +
                    prt(bigInt) + "," +
                    prt(chr) + "," +
                    prt(lng) +
                    ")");
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
        public static @interface Foobarred {
        }
    }

    public static class C {
    }

    public static class D {
    }

    public static class B {
        private final C c;

        public B(C c) {
            this.c = c;
        }
    }

    private String newD() {
        return "D#2";
    }

    private String newCandB() {
        return "C#0,B#1<C#0>";
    }

    private String mockD() {
        return "mock[D]#2";
    }

    private String setterCalledWith(String with) {
        return ",setIt(" + with + ")";
    }

    private String memberVarD() {
        return "d";
    }

    private String autowiredMethodCalledWith(String with) {
        return ",aut0wireMe(" + with + ")";
    }

    private String autowiredFieldSetTo(String to) {
        return ",b1=" + to;
    }

    private String atInjectFieldSetTo(String to) {
        return ",b2=" + to;
    }

    private String memberVarB() {
        return "b<c>";
    }

    private String memberVarsCandB() {
        return "c,b<c>";
    }

    private String atInjectMethodCalledWithBmemberVar(String with) {
        return ",inj3ct("+with+")";
    }

    private String aMadeWith(String with) {
        return "A("+with+")";
    }

    private String mockB() {
        return "mock[B]#1";
    }

    private String mockCandB() {
        return "mock[C]#0,mock[B]#1";
    }

    private String aBunchOfPrimitives() {
        return "String#0,Integer#1,Double#2,Double#2,Float#3,Byte#4,Short#5,mock[BigInteger]#2,Character#6,Long#7";
    }

    private String customAnnotatedMethodCalledWith(String with) {
        return ",foobar("+with+")";
    }

}
